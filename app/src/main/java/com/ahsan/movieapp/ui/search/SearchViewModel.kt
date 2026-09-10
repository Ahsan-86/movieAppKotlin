package com.ahsan.movieapp.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahsan.movieapp.data.repository.MovieRepository
import com.ahsan.movieapp.data.repository.PreferencesRepository
import com.ahsan.movieapp.data.repository.SearchViewMode
import com.ahsan.movieapp.domain.model.DiscoverFilters
import com.ahsan.movieapp.domain.model.GenreChip
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.domain.model.Person
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val movies: List<Movie> = emptyList(),
    val people: List<Person> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val genreChips: List<GenreChip> = emptyList(),
    val viewMode: SearchViewMode = SearchViewMode.GRID,
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val errorMessage: String? = null,
    // Phase 2.5 collapsible filter panel — only usable on the blank/no-query state, since TMDB's
    // text-search endpoints don't accept any of these params.
    val filters: DiscoverFilters = DiscoverFilters(),
    val isFilterPanelExpanded: Boolean = false,
    val isFilterApplied: Boolean = false,
    val filteredMovies: List<Movie> = emptyList(),
    val isLoadingFilteredResults: Boolean = false,
    val filterErrorMessage: String? = null
)

/**
 * Phase 2 redo, extended: a single debounced call to [MovieRepository.search] (TMDB
 * `/search/multi`) backs both the movie grid and the people row. The first-open state shows genre
 * chips (tapping one navigates to a dedicated full-screen genre browser — see GenreScreen/
 * GenreViewModel — rather than browsing inline here) instead of a blank prompt. The results
 * layout (list/grid/4-up grid) is a persisted preference, not local state.
 */
@HiltViewModel
@OptIn(FlowPreview::class)
class SearchViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")
    private var searchJob: Job? = null
    private var filterJob: Job? = null

    init {
        queryFlow
            .debounce(350)
            .distinctUntilChanged()
            .onEach { query -> runSearch(query) }
            .launchIn(viewModelScope)

        repository.observeRecentSearches()
            .onEach { recent -> _uiState.update { it.copy(recentSearches = recent) } }
            .launchIn(viewModelScope)

        preferencesRepository.searchViewMode
            .onEach { mode -> _uiState.update { it.copy(viewMode = mode) } }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            repository.getGenreChips().onSuccess { chips ->
                _uiState.update { it.copy(genreChips = chips) }
            }
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
        queryFlow.value = query
    }

    /** Called when the user explicitly commits a search (IME search action) — this is what gets recorded to history, not every debounced keystroke. */
    fun onSearchSubmit() {
        val query = _uiState.value.query.trim()
        if (query.isNotBlank()) {
            viewModelScope.launch { repository.recordSearch(query) }
        }
    }

    fun onRecentSearchClick(query: String) {
        _uiState.update { it.copy(query = query) }
        queryFlow.value = query
        viewModelScope.launch { repository.recordSearch(query) }
    }

    fun clearSearchHistory() {
        viewModelScope.launch { repository.clearSearchHistory() }
    }

    fun onViewModeSelected(mode: SearchViewMode) {
        viewModelScope.launch { preferencesRepository.setSearchViewMode(mode) }
    }

    fun toggleFavorite(movie: Movie) {
        viewModelScope.launch {
            repository.toggleFavorite(movie)
            // search() isn't re-collected on every favorite toggle, so flip the badge
            // optimistically rather than re-running the whole lookup for one changed favorite.
            // filteredMovies needs the same treatment for the same reason.
            _uiState.update { state ->
                state.copy(
                    movies = state.movies.map {
                        if (it.id == movie.id) it.copy(isFavorite = !it.isFavorite) else it
                    },
                    filteredMovies = state.filteredMovies.map {
                        if (it.id == movie.id) it.copy(isFavorite = !it.isFavorite) else it
                    }
                )
            }
        }
    }

    // --- Phase 2.5: collapsible filter panel (genre/year/language/min rating) ---

    fun onToggleFilterPanel() {
        _uiState.update { it.copy(isFilterPanelExpanded = !it.isFilterPanelExpanded) }
    }

    fun onGenreFilterSelected(genreId: Int?) {
        _uiState.update { it.copy(filters = it.filters.copy(genreId = genreId)) }
    }

    fun onYearFilterSelected(year: Int?) {
        _uiState.update { it.copy(filters = it.filters.copy(year = year)) }
    }

    fun onLanguageFilterSelected(language: String?) {
        _uiState.update { it.copy(filters = it.filters.copy(language = language)) }
    }

    fun onMinRatingFilterChanged(minRating: Float?) {
        _uiState.update { it.copy(filters = it.filters.copy(minRating = minRating)) }
    }

    /** Runs the current filter combination and switches the screen over to showing its results. */
    fun onApplyFilters() {
        val filters = _uiState.value.filters
        if (filters.isEmpty) return
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            _uiState.update {
                it.copy(isFilterApplied = true, isLoadingFilteredResults = true, filterErrorMessage = null)
            }
            repository.discoverMovies(filters)
                .onSuccess { movies ->
                    _uiState.update { it.copy(filteredMovies = movies, isLoadingFilteredResults = false) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isLoadingFilteredResults = false, filterErrorMessage = throwable.message ?: "Couldn't load results")
                    }
                }
        }
    }

    /** Back to the filter panel (expanded, criteria kept) instead of the plain blank state. */
    fun onEditFilters() {
        _uiState.update { it.copy(isFilterApplied = false, isFilterPanelExpanded = true) }
    }

    fun onClearFilters() {
        filterJob?.cancel()
        _uiState.update {
            it.copy(filters = DiscoverFilters(), isFilterApplied = false, filteredMovies = emptyList(), filterErrorMessage = null)
        }
    }

    private fun runSearch(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update {
                it.copy(movies = emptyList(), people = emptyList(), isSearching = false, hasSearched = false, errorMessage = null)
            }
            return
        }
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, errorMessage = null) }
            repository.search(query)
                .onSuccess { results ->
                    _uiState.update {
                        it.copy(movies = results.movies, people = results.people, isSearching = false, hasSearched = true)
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isSearching = false, hasSearched = true, errorMessage = throwable.message ?: "Search failed")
                    }
                }
        }
    }
}
