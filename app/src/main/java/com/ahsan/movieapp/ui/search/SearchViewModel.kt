package com.ahsan.movieapp.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ahsan.movieapp.data.repository.MovieRepository
import com.ahsan.movieapp.data.repository.PreferencesRepository
import com.ahsan.movieapp.data.repository.SearchViewMode
import com.ahsan.movieapp.domain.model.DiscoverFilters
import com.ahsan.movieapp.domain.model.GenreChip
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.domain.model.Person
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    // The movie grid is now Paging 3 (see pagedSearchMovies below) — this only tracks the people
    // row, which stays a small one-shot fetch per query (never paginated, see the class doc).
    val people: List<Person> = emptyList(),
    val isSearchingPeople: Boolean = false,
    val recentSearches: List<String> = emptyList(),
    val genreChips: List<GenreChip> = emptyList(),
    val viewMode: SearchViewMode = SearchViewMode.GRID,
    // Phase 2.5 collapsible filter panel — only usable on the blank/no-query state, since TMDB's
    // text-search endpoints don't accept any of these params. `filters` is the panel's in-progress
    // draft; pagedFilteredMovies below only re-pages once `onApplyFilters()` commits it.
    val filters: DiscoverFilters = DiscoverFilters(),
    val isFilterPanelExpanded: Boolean = false,
    val isFilterApplied: Boolean = false
)

/**
 * Phase 2 redo, extended, now Phase 4 Round 4 (pagination): the movie grid — both text-search
 * results and the filter panel's Discover results — is Paging 3 infinite scroll, backed by
 * [MovieRepository.getPagedSearchMovies]/[MovieRepository.getPagedDiscoverMovies]. Neither has a
 * Room cache table (arbitrary search text and arbitrary filter combinations both have too many
 * possible keys to usefully cache), so unlike Trending/Genre's Movies tab, favorite status isn't
 * resolved via a Room `LEFT JOIN` inside the paging query itself — instead each `PagingSource`
 * (`SearchMoviesPagingSource`/`DiscoverPagingSource`) takes a one-time snapshot of the favorites
 * table on every `load()` call and stamps `isFavorite` from that.
 *
 * [pagedSearchMovies] and [pagedFilteredMovies] used to `combine()` their raw [PagingData] with a
 * live [MovieRepository.observeFavorites] flow instead, re-mapping every item's `isFavorite` flag on
 * every favorites change — that crashed (`IllegalStateException: Attempt to collect twice from
 * pageEventFlow`): `combine()` re-invokes `.map{}` on the same underlying `PagingData` every time the
 * side flow emits, not just when the paging flow itself emits, so two overlapping generations ended
 * up trying to collect the same page-event flow at once. Do NOT reintroduce a `combine()` (or
 * `zip()`) of a live side flow against a `Flow<PagingData<*>>` before `cachedIn()` — see
 * `SearchMoviesPagingSource`'s class doc for the fix that replaced it. The trade-off: toggling a
 * favorite from these two screens doesn't flip the heart icon live like Trending/Genre do; it's
 * correct again next time the screen re-queries (new search text, or re-applying filters).
 *
 * The people row stays a small one-shot fetch per debounced query (see [SearchUiState.people]) —
 * only the first handful of people a query returns are ever shown, so there's nothing to paginate
 * there, same reasoning that keeps Cast & Crew and Similar/Recommendations out of Phase 4 entirely.
 * The first-open state still shows genre chips (tapping one navigates to a dedicated full-screen
 * genre browser) instead of a blank prompt. The results layout (list/grid/4-up grid) is a
 * persisted preference, not local state.
 */
@HiltViewModel
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")
    private val appliedFiltersFlow = MutableStateFlow<DiscoverFilters?>(null)
    private var peopleJob: Job? = null

    val pagedSearchMovies: Flow<PagingData<Movie>> = queryFlow
        .debounce(350)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(PagingData.empty()) else repository.getPagedSearchMovies(query)
        }
        .cachedIn(viewModelScope)

    val pagedFilteredMovies: Flow<PagingData<Movie>> = appliedFiltersFlow
        .flatMapLatest { filters ->
            if (filters == null) flowOf(PagingData.empty()) else repository.getPagedDiscoverMovies(filters)
        }
        .cachedIn(viewModelScope)

    init {
        queryFlow
            .debounce(350)
            .distinctUntilChanged()
            .onEach { query -> loadPeople(query) }
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

    /** Unlike Trending/Genre, this doesn't live-update the grid: [pagedSearchMovies] and
     *  [pagedFilteredMovies] stamp `isFavorite` from a one-time snapshot inside their
     *  `PagingSource`s (see the class doc), not a live favorites flow, so the heart icon here
     *  only reflects the change the next time this screen re-queries (new search text, or
     *  re-applying filters) — not instantly on tap. */
    fun toggleFavorite(movie: Movie) {
        viewModelScope.launch { repository.toggleFavorite(movie) }
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
        _uiState.update { it.copy(isFilterApplied = true) }
        appliedFiltersFlow.value = filters
    }

    /** Back to the filter panel (expanded, criteria kept) instead of the plain blank state. */
    fun onEditFilters() {
        _uiState.update { it.copy(isFilterApplied = false, isFilterPanelExpanded = true) }
    }

    fun onClearFilters() {
        appliedFiltersFlow.value = null
        _uiState.update { it.copy(filters = DiscoverFilters(), isFilterApplied = false) }
    }

    private fun loadPeople(query: String) {
        peopleJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(people = emptyList(), isSearchingPeople = false) }
            return
        }
        peopleJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearchingPeople = true) }
            repository.searchPeople(query)
                .onSuccess { people -> _uiState.update { it.copy(people = people, isSearchingPeople = false) } }
                .onFailure {
                    // Fails silently — the movie grid still has its own loadState-driven error/retry,
                    // and losing just the people row isn't worth a second error surface for one query.
                    _uiState.update { it.copy(people = emptyList(), isSearchingPeople = false) }
                }
        }
    }
}
