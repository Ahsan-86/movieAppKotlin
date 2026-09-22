package com.ahsan.movieapp.ui.genre

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ahsan.movieapp.data.repository.MovieRepository
import com.ahsan.movieapp.domain.model.DiscoverFilters
import com.ahsan.movieapp.domain.model.MediaType
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.ui.person.MediaTab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

data class GenreUiState(
    val genreName: String,
    val selectedMediaType: MediaTab,
    val showMoviesTab: Boolean,
    val showTvTab: Boolean,
    // Same collapsible filter panel shape as the search screen, minus the genre dropdown (this
    // screen pins its genre): `filters` is the panel's in-progress draft; pagedMovies/
    // pagedTvShows only re-page once a non-empty draft is Applied via onApplyFilters().
    val filters: DiscoverFilters = DiscoverFilters(),
    val isFilterPanelExpanded: Boolean = false,
    val isFilterApplied: Boolean = false
)

/**
 * Backs the full-screen genre browse view opened by tapping a genre chip on the search screen.
 * Both tabs are Paging 3 infinite-scroll as of Phase 4 Round 3: Movies goes through
 * [MovieRepository.getPagedGenre] (Room + [androidx.paging.RemoteMediator], Round 2); TV goes
 * through [MovieRepository.getPagedGenreTv] (a plain network-only
 * [androidx.paging.PagingSource], Round 3 — a genre+filter TV search has no cache table of its
 * own, so there's no Room table for a `RemoteMediator` to page into; only the curated TV carousels
 * are cached, see [MovieRepository.getCategoryTv]). Paging 3's own loadState carries all
 * loading/error/empty state for both tabs now, so [GenreUiState] only tracks which tab is
 * selected and which tabs this chip actually has.
 *
 * The filter section (year/language/minimum rating — the genre itself is fixed by this screen)
 * borrows the search screen's Phase 2.5 panel + summary-bar pattern, 2026-09-21. Each paged flow
 * `flatMapLatest`s over an applied-filters flow: no filters → Movies keeps its offline-first Room
 * browse; filters applied → Movies pages via [MovieRepository.getPagedDiscoverMovies]
 * (the fixed genre is folded into the filters as `genreId`), TV pages via
 * [MovieRepository.getPagedGenreTv]'s filter param. Since Session 7 the filtered Movies path has
 * its OWN Room cache (per `{genreId, filters} combos`, see [MovieRepository.observeDiscoverResultTotal]):
 * [MovieRepository.getPagedDiscoverMovies] pages through `DiscoverRemoteMediator` into
 * `discover_combo_movies`, so hearts come from the live `LEFT JOIN favorites` — flips land
 * immediately, same as the unfiltered browse. The TV tab stays a plain TvGenrePagingSource
 * (network-only, no table to page into) and re-stamps hearts against [favoriteIds] at render time.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GenreViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MovieRepository
) : ViewModel() {

    private val movieGenreId: Int? = (savedStateHandle.get<Int>("movieGenreId") ?: -1).takeIf { it != -1 }
    private val tvGenreId: Int? = (savedStateHandle.get<Int>("tvGenreId") ?: -1).takeIf { it != -1 }
    private val genreName: String = URLDecoder.decode(checkNotNull(savedStateHandle.get<String>("genreName")), "UTF-8")

    // Null when the applied filters are cleared; non-null once the user commits a non-empty draft
    // via onApplyFilters(). Drives both paged flows below (flatMapLatest, same pattern as SearchViewModel).
    private val appliedFiltersFlow = MutableStateFlow<DiscoverFilters?>(null)

    // Movie-total: since Session 7 the filtered Movies path's count comes straight from the combo's
    // Room cache keyed by `{genreId, filters}` (so it survives offline and needs no per-page flow
    // plumbing); null until that combo has been fetched at least once (and always null while no
    // filters are applied, since the screen only shows the count while isFilterApplied). The TV
    // total below is still a MutableStateFlow ([TvGenrePagingSource] writes page-1 totals into it).
    val movieFilteredTotal: StateFlow<Int?> =
        movieGenreId?.let { id ->
            appliedFiltersFlow
                .flatMapLatest { filters ->
                    if (filters == null) flowOf(null) else repository.observeDiscoverResultTotal(filters.copy(genreId = id))
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
        } ?: MutableStateFlow(null)

    // Same count pattern for the TV tab ([TvGenrePagingSource] writes its total here). Reset to
    // null whenever the filter set changes so a stale count never lingers until the next page 1.
    private val tvTotal = MutableStateFlow<Int?>(null)
    val tvFilteredTotal: StateFlow<Int?> = tvTotal.asStateFlow()

    // Live set of favorited (id, mediaType) pairs — the UI re-stamps each paginated item's
    // isFavorite against this at render time so hearts flip immediately on toggle (see class doc /
    // SearchViewModel). Keyed by mediaType too since Session 6 lets both tabs favorite: a movie and
    // a same-numbered TV show are separate favorites.
    val favoriteIds: StateFlow<Set<Pair<Int, MediaType>>> = repository.observeFavorites()
        .map { favorites -> favorites.mapTo(mutableSetOf()) { it.id to it.mediaType } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    // Null when this chip has no movie genre at all (a TV-only chip) — nothing to page there.
    val pagedMovies: Flow<PagingData<Movie>>? =
        movieGenreId?.let { id ->
            appliedFiltersFlow.flatMapLatest { filters ->
                if (filters == null) {
                    repository.getPagedGenre(id)
                } else {
                    repository.getPagedDiscoverMovies(filters.copy(genreId = id))
                }
            }.cachedIn(viewModelScope)
        }

    // Null when this chip has no TV genre at all (e.g. Horror/Romance/Thriller, which are
    // movie-only on TMDB) — same reasoning as pagedMovies above. Filters apply via the same
    // TvGenrePagingSource (empty DiscoverFilters = plain genre browse); tvTotal reads the
    // filtered-results count either way (the screen only shows it while isFilterApplied).
    val pagedTvShows: Flow<PagingData<Movie>>? =
        tvGenreId?.let { id ->
            appliedFiltersFlow.flatMapLatest { filters ->
                repository.getPagedGenreTv(id, filters ?: DiscoverFilters(), tvTotal)
            }.cachedIn(viewModelScope)
        }

    private val _uiState = MutableStateFlow(
        GenreUiState(
            genreName = genreName,
            selectedMediaType = if (movieGenreId != null) MediaTab.MOVIES else MediaTab.TV,
            showMoviesTab = movieGenreId != null,
            showTvTab = tvGenreId != null
        )
    )
    val uiState: StateFlow<GenreUiState> = _uiState.asStateFlow()

    fun onMediaTabSelected(tab: MediaTab) {
        _uiState.update { it.copy(selectedMediaType = tab) }
    }

    fun toggleFavorite(movie: Movie) {
        // No optimistic state update needed: the unfiltered Movies tab reads through Room, and the
        // filtered Movies path is itself Room-cached via a `LEFT JOIN favorites` since Session 7 —
        // either way a toggle re-invalidates the PagingSource query and Paging 3 diffs in just the
        // changed row. The TV tab (Session 6) re-stamps against the live [favoriteIds] set at
        // render time instead, so those hearts flip immediately too.
        viewModelScope.launch { repository.toggleFavorite(movie) }
    }

    // --- Collapsible filter panel (year/language/min rating — no genre, this screen pins it) ---

    fun onToggleFilterPanel() {
        _uiState.update { it.copy(isFilterPanelExpanded = !it.isFilterPanelExpanded) }
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

    fun resetFiltersDraft() {
        _uiState.update { it.copy(filters = DiscoverFilters()) }
    }

    /** Runs the current filter combination and re-pages the active tab (Movies and TV: whichever
     *  the user lands on next). */
    fun onApplyFilters() {
        val filters = _uiState.value.filters
        if (filters.isEmpty) return
        _uiState.update { it.copy(isFilterApplied = true, isFilterPanelExpanded = false) }
        tvTotal.value = null
        appliedFiltersFlow.value = filters
    }

    /** Summaries' "Edit" — back to the expanded panel (draft kept) instead of the applied view. */
    fun onEditFilters() {
        _uiState.update { it.copy(isFilterApplied = false, isFilterPanelExpanded = true) }
    }

    fun onClearFilters() {
        appliedFiltersFlow.value = null
        tvTotal.value = null
        _uiState.update { it.copy(filters = DiscoverFilters(), isFilterApplied = false) }
    }
}