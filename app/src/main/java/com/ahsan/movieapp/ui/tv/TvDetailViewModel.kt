package com.ahsan.movieapp.ui.tv

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahsan.movieapp.data.mapper.toMovie
import com.ahsan.movieapp.data.repository.MovieRepository
import com.ahsan.movieapp.domain.model.CastMember
import com.ahsan.movieapp.domain.model.MediaType
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.domain.model.Person
import com.ahsan.movieapp.domain.model.TvShowDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TvDetailUiState(
    val details: TvShowDetails? = null,
    val cast: List<CastMember> = emptyList(),
    val director: Person? = null,
    // One "More Like This" shelf — Recommendations only (Similar dropped 2026-09-20: not relevant
    // enough). The UI/title keep the shape from Review-queue item 1 → option C.
    val moreLikeThis: List<Movie> = emptyList(),
    // Phase 2.6 Session 2 — Watch Trailer button. Null means either still loading or TMDB has no
    // YouTube trailer for this show; either way the button just doesn't render.
    val trailerKey: String? = null,
    // Session 6 — this show's favorite state (keyed by (tvId, TV)) for the FloatingActionButton,
    // consumed live off the Favorites table exactly like MovieDetailScreen does.
    val isFavorite: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

/**
 * Phase 2.6's TV detail screen ViewModel — info section, Cast & Crew, Information, a
 * Recommendations-based "More Like This" shelf (Session 1; merged into one shelf per Review-queue
 * item 1 → option C, then Similar dropped 2026-09-20: not relevant enough), plus a Watch
 * Trailer button (Session 2). Network-only for every fetch, no Room cache — same one-shot,
 * failure-tolerant convention as [com.ahsan.movieapp.ui.detail.CastCrewListViewModel] and the
 * newer network-only calls on [MovieRepository] ([MovieRepository.getMovieCredits],
 * [MovieRepository.getCollectionDetails]), since TV detail data has no offline table of its own
 * (only the curated TV carousels are cached — see [MovieRepository.getCategoryTv]). Unlike
 * [com.ahsan.movieapp.ui.detail.MovieDetailViewModel], there's no networkBoundResource/offline-first
 * path here — but Session 6 added the favorite toggle: the FloatingActionButton mirrors
 * MovieDetailScreen's, driving [toggleFavorite] against the composite-key Favorites table through
 * the same `isFavorite(tvId, TV)` live flow.
 *
 * Credits, Recommendations, and the trailer key are each fetched in their own
 * `viewModelScope.launch` and deliberately have no `onFailure` handling — a failed fetch just
 * leaves that piece of state at its empty default so its section simply doesn't render, matching
 * com.ahsan.movieapp.ui.detail.MovieDetailViewModel's credits-failure behavior exactly. The
 * Seasons section (Session 2) needs no separate fetch here — it comes free on getTvDetails's
 * same call, already part of TvShowDetails.seasons.
 */
@HiltViewModel
class TvDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MovieRepository
) : ViewModel() {

    private val tvId: Int = checkNotNull(savedStateHandle["tvId"])

    private val _uiState = MutableStateFlow(TvDetailUiState())
    val uiState: StateFlow<TvDetailUiState> = _uiState.asStateFlow()

    // Session 6 — live favorite state for the FAB from Room's favorites table ((tvId, TV) row).
    val isFavorite: StateFlow<Boolean> = repository.isFavorite(tvId, MediaType.TV)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        viewModelScope.launch {
            repository.getTvDetails(tvId)
                .onSuccess { details ->
                    _uiState.update { it.copy(details = details, isLoading = false, errorMessage = null) }
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message) }
                }
        }
        viewModelScope.launch {
            repository.getTvCredits(tvId)
                .onSuccess { credits -> _uiState.update { it.copy(cast = credits.cast, director = credits.director) } }
            // Deliberately no onFailure handling — see the class doc above.
        }
        viewModelScope.launch {
            repository.getRecommendedTvShows(tvId)
                .onSuccess { shows -> _uiState.update { it.copy(moreLikeThis = shows) } }
            // Deliberately no onFailure handling — see the class doc above.
        }
        viewModelScope.launch {
            repository.getTvTrailerKey(tvId)
                .onSuccess { key -> _uiState.update { it.copy(trailerKey = key) } }
            // Deliberately no onFailure handling — see the class doc above.
        }
    }

    /** Session 6 — favorite/un-favorite this show via the composite-key Favorites table. The FAB's
     *  heart reads [isFavorite] live, so it flips on its own after a toggle; no optimistic update
     *  needed (the show is never in a Room cache flow this screen pages through). */
    fun toggleFavorite() {
        val details = _uiState.value.details ?: return
        viewModelScope.launch {
            repository.toggleFavorite(details.toMovie())
        }
    }
}
