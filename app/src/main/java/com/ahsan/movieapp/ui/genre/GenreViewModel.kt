package com.ahsan.movieapp.ui.genre

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ahsan.movieapp.data.repository.MovieRepository
import com.ahsan.movieapp.domain.model.DiscoverFilters
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
 * browse; filters applied → Movies pages network-only via [MovieRepository.getPagedDiscoverMovies]
 * (the fixed genre is folded into the filters as `genreId`), TV pages via
 * [MovieRepository.getPagedGenreTv]'s filter param. Empty [DiscoverFilters] on the TV side pages
 * plain, so the same `TvGenrePagingSource` covers both. Because the filtered Movies path goes
 * through [com.ahsan.movieapp.data.paging.DiscoverPagingSource], favorite hearts stamp from a
 * one-time snapshot per page (not the live Room join the unfiltered path uses) — heart flips only
 * land the next time this screen re-pages, same documented trade-off as search's Discover results.
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

    // Per-tab filtered-total flows ([DiscoverPagingSource]/[TvGenrePagingSource] write page-1
    // totals into the active tab's). Separate per tab because both tabs are collected at once even
    // though only one grid ever composes — a single shared flow could end up showing the inactive
    // tab's count. Reset to null whenever the filter set changes.
    private val movieTotal = MutableStateFlow<Int?>(null)
    private val tvTotal = MutableStateFlow<Int?>(null)
    val movieFilteredTotal: StateFlow<Int?> = movieTotal.asStateFlow()
    val tvFilteredTotal: StateFlow<Int?> = tvTotal.asStateFlow()

    // Live set of favorited movie ids — the UI re-stamps each paginated movie's isFavorite against
    // this at render time so hearts flip immediately on toggle (see class doc / SearchViewModel).
    val favoriteIds: StateFlow<Set<Int>> = repository.observeFavorites()
        .map { favorites -> favorites.mapTo(mutableSetOf()) { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    // Null when this chip has no movie genre at all (a TV-only chip) — nothing to page there.
    val pagedMovies: Flow<PagingData<Movie>>? =
        movieGenreId?.let { id ->
            appliedFiltersFlow.flatMapLatest { filters ->
                if (filters == null) {
                    repository.getPagedGenre(id)
                } else {
                    repository.getPagedDiscoverMovies(filters.copy(genreId = id), movieTotal)
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
        // No optimistic state update needed here: the unfiltered Movies tab reads through Room,
        // whose PagingSource query already joins `favorites` — a toggle re-invalidates it
        // automatically and Paging 3 diffs in just the changed row. (TV shows can't be favorited
        // yet; the filtered Movies path follows DiscoverPagingSource's snapshot trade-off, noted
        // in the class doc.)
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
        movieTotal.value = null
        tvTotal.value = null
        appliedFiltersFlow.value = filters
    }

    /** Summaries' "Edit" — back to the expanded panel (draft kept) instead of the applied view. */
    fun onEditFilters() {
        _uiState.update { it.copy(isFilterApplied = false, isFilterPanelExpanded = true) }
    }

    fun onClearFilters() {
        appliedFiltersFlow.value = null
        movieTotal.value = null
        tvTotal.value = null
        _uiState.update { it.copy(filters = DiscoverFilters(), isFilterApplied = false) }
    }
}