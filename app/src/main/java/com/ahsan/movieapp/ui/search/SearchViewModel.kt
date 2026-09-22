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
import com.ahsan.movieapp.domain.model.MediaType
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.domain.model.Person
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    // Session 6 — `query` is only the live field text (every keystroke updates it);
    // `committedQuery` is the last query actually run against TMDB (written by [onSearchSubmit] /
    // [onRecentSearchClick]). The screen's results branch keys off THIS, not the raw text — so
    // typing alone never slides the UI into the results/loading layout. That was the source of
    // the full-screen loader appearing while typing on the first ever search: a first keystroke
    // made `query` non-blank, the screen entered the results branch, and the still-unloaded
    // Paging flow tripped `nothingLoadedYet && refresh is Loading`. Later searches never showed
    // it because the grid already held items from the previous committed query. Clearing the
    // field also drops the committed query (back to the blank home content).
    val committedQuery: String = "",
    // The movie grid is now Paging 3 (see pagedSearchMovies below) — this only tracks the people
    // row, which stays a small one-shot fetch per query (never paginated, see the class doc).
    val people: List<Person> = emptyList(),
    val isSearchingPeople: Boolean = false,
    // The "TV Shows" result row — a capped one-shot [Movie] list per submitted query (see
    // MovieRepository.searchTvShows), grouped separately from the movie grid the same way
    // [people] is. TV rows ARE favoritable since Session 6 (the composite-key Favorites table),
    // so like the movie grid they render a heart and re-stamp against the live favorites set.
    val tvShows: List<Movie> = emptyList(),
    val isSearchingTv: Boolean = false,
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
 * [MovieRepository.getPagedSearchMovies]/[MovieRepository.getPagedDiscoverMovies]. Arbitrary search
 * text still has no Room cache table, so only [MovieRepository.getPagedSearchMovies] resolves favorite
 * status via a per-load snapshot inside its `PagingSource` (`SearchMoviesPagingSource`).
 *
 * Session 7 — the filter panel's Discover results are different now: [MovieRepository.getPagedDiscoverMovies]
 * is Room-cached per applied filter combination (`comboKey`), so favorites on that grid are computed
 * by the same live `LEFT JOIN favorites` as the Trending/Genre carousels — hearts flip without any
 * overlay. The stamped-snapshot trade-off and the combine-vs-`cachedIn` crash it caused only apply
 * to [pagedSearchMovies]'s text results now: `combine()` re-invokes `.map{}` on the same underlying
 * `PagingData` every time the side flow emits, so two overlapping generations ended up trying to
 * collect the same page-event flow at once (`IllegalStateException: Attempt to collect twice from
 * pageEventFlow`). Do NOT reintroduce a `combine()` (or `zip()`) of a live side flow against a
 * `Flow<PagingData<*>>` before `cachedIn()` — see `SearchMoviesPagingSource`'s class doc for the
 * fix that replaced it. The remaining visible trade-off — toggling a favorite from the text-search
 * grid didn't flip the heart icon live — is closed with a render-time overlay ([favoriteIds]): each
 * grid item re-stamps its `isFavorite` against the live favorites set as it's composed, so hearts
 * flip immediately even though the underlying page was stamped from a snapshot.
 *
 * The people row stays a small one-shot fetch per submitted query (see [SearchUiState.people]) —
 * only the first handful of people a query returns are ever shown, so there's nothing to paginate
 * there, same reasoning that keeps Cast & Crew and Similar/Recommendations out of Phase 4 entirely.
 * The TV Shows row follows the exact same pattern (see [SearchUiState.tvShows] and
 * [MovieRepository.searchTvShows]): capped, non-paginated, network-only, grouped separately from
 * the movie grid. The first-open state still shows genre chips (tapping one navigates to a
 * dedicated full-screen genre browser) instead of a blank prompt. The results layout
 * (list/grid/4-up grid) is a persisted preference, not local state.
 *
 * Session 6 — search is now explicit-submit: typing only edits the field text ([onQueryInput]),
 * and the query actually executes only when the user commits it ([onSearchSubmit] — the IME
 * search key or the trailing button). No more live search as you type; recent-search chips.
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")
    private val appliedFiltersFlow = MutableStateFlow<DiscoverFilters?>(null)
    private var peopleJob: Job? = null
    private var tvJob: Job? = null

    // Clear the committed text query only; the discover filter flow is cleared separately.
    // No longer a MutableStateFlow-bound filtered total — the "N results found" line for the
    // filter panel's Discover results comes straight from the combo's Room cache (Session 7).
    val filteredResultCount: StateFlow<Int?> = appliedFiltersFlow
        .flatMapLatest { filters ->
            if (filters == null) flowOf(null) else repository.observeDiscoverResultTotal(filters)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Same count pattern for text-search results ([SearchMoviesPagingSource] writes its total
    // here); the screen shows the line above the results grid. Reset to null whenever the query
    // changes so a stale count never lingers until the next page 1 arrives.
    private val searchTotal = MutableStateFlow<Int?>(null)
    val searchResultCount: StateFlow<Int?> = searchTotal.asStateFlow()

    // Live set of favorited (id, mediaType) pairs (from Room's favorites table). The UI re-stamps
    // the text-search grid items' AND the TV row's isFavorite against this set at render time,
    // which is what makes toggling a favorite here flip the heart immediately instead of waiting
    // for the next re-page (see SearchMoviesPagingSource's class doc for why that snapshot can't).
    // The filter-panel Discover grid needs no such overlay since Session 7 — the paging query joins
    // Room's favorites table live. Keyed by mediaType too — a favorite movie and a same-numbered
    // favorite TV show are separate rows (Session 6), so re-stamping must distinguish them.
    val favoriteIds: StateFlow<Set<Pair<Int, MediaType>>> = repository.observeFavorites()
        .map { favorites -> favorites.mapTo(mutableSetOf()) { it.id to it.mediaType } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val pagedSearchMovies: Flow<PagingData<Movie>> = queryFlow
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(PagingData.empty()) else repository.getPagedSearchMovies(query, searchTotal)
        }
        .cachedIn(viewModelScope)

    val pagedFilteredMovies: Flow<PagingData<Movie>> = appliedFiltersFlow
        .flatMapLatest { filters ->
            if (filters == null) flowOf(PagingData.empty()) else repository.getPagedDiscoverMovies(filters)
        }
        .cachedIn(viewModelScope)

    init {
        // Session 6 — the query executes only on [onSearchSubmit]/[onRecentSearchClick]; both write
        // queryFlow, and this block fans the committed query out to the people + TV one-shot rows.
        queryFlow
            .onEach { query ->
                loadPeople(query)
                loadTvShows(query)
            }
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

    /** Session 6 — every keystroke just updates the field text for display. It does NOT execute the
     *  search: that happens once on [onSearchSubmit] (IME search action or the trailing button).
     *  Clearing the field also clears the committed query, returning the screen to the blank home
     *  content (see [SearchUiState.committedQuery] for why the two are separate). */
    fun onQueryInput(query: String) {
        _uiState.update {
            it.copy(query = query, committedQuery = if (query.isBlank()) "" else it.committedQuery)
        }
    }

    /** The user explicitly committed a search (IME search action / trailing button). The ONLY place
     *  a typed query turns into results — never runs while typing (Session 6). Also records the
     *  trimmed query to history; [onRecentSearchClick] is the other writer of queryFlow. */
    fun onSearchSubmit() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return
        _uiState.update { it.copy(query = query, committedQuery = query) }
        queryFlow.value = query
        searchTotal.value = null
        viewModelScope.launch { repository.recordSearch(query) }
    }

    fun onRecentSearchClick(query: String) {
        _uiState.update { it.copy(query = query, committedQuery = query) }
        queryFlow.value = query
        searchTotal.value = null
        viewModelScope.launch { repository.recordSearch(query) }
    }

    fun clearSearchHistory() {
        viewModelScope.launch { repository.clearSearchHistory() }
    }

    /** Removes a single query from the recent-searches chips (per-chip delete button). */
    fun onDeleteRecentSearch(query: String) {
        viewModelScope.launch { repository.deleteSearchHistory(query) }
    }

    fun onViewModeSelected(mode: SearchViewMode) {
        viewModelScope.launch { preferencesRepository.setSearchViewMode(mode) }
    }

    /** Only the text-search grid ([pagedSearchMovies]) stamps `isFavorite` from a one-time snapshot
     *  inside its `PagingSource` — so its heart icon reflects a change the next time the query
     *  re-runs, not instantly (re-stamped visually by [favoriteIds] at render time). The filter
     *  panel's Discover grid is Room-backed with a live favorites join (Session 7), so its hearts
     *  follow toggles immediately. */
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

    /** Same pattern as [loadPeople] for the "TV Shows" row — capped one-shot, silent failure, no
     *  Room involvement (TV data isn't persisted in this app). */
    private fun loadTvShows(query: String) {
        tvJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(tvShows = emptyList(), isSearchingTv = false) }
            return
        }
        tvJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearchingTv = true) }
            repository.searchTvShows(query)
                .onSuccess { tvShows -> _uiState.update { it.copy(tvShows = tvShows, isSearchingTv = false) } }
                .onFailure {
                    _uiState.update { it.copy(tvShows = emptyList(), isSearchingTv = false) }
                }
        }
    }
}
