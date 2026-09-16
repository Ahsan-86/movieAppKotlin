package com.ahsan.movieapp.ui.tv

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahsan.movieapp.data.repository.MovieRepository
import com.ahsan.movieapp.domain.model.CastMember
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.domain.model.Person
import com.ahsan.movieapp.domain.model.TvShowDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TvDetailUiState(
    val details: TvShowDetails? = null,
    val cast: List<CastMember> = emptyList(),
    val director: Person? = null,
    val similarTvShows: List<Movie> = emptyList(),
    val recommendedTvShows: List<Movie> = emptyList(),
    // Phase 2.6 Session 2 — Watch Trailer button. Null means either still loading or TMDB has no
    // YouTube trailer for this show; either way the button just doesn't render.
    val trailerKey: String? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

/**
 * Phase 2.6's TV detail screen ViewModel — info section, Cast & Crew, Information, Similar, and
 * Recommendations (Session 1, the last three added on Ahsan's post-build feedback), plus a Watch
 * Trailer button (Session 2). Network-only for every fetch, no Room cache — same one-shot,
 * failure-tolerant convention as [com.ahsan.movieapp.ui.detail.CastCrewListViewModel] and the
 * newer network-only calls on [MovieRepository] ([MovieRepository.getMovieCredits],
 * [MovieRepository.getCollectionDetails]), since this app doesn't persist TV data yet (see
 * [MovieRepository.getPopularTv]'s doc). Unlike
 * [com.ahsan.movieapp.ui.detail.MovieDetailViewModel], there's no networkBoundResource/offline-first
 * path and no favorite toggle here — TV favoriting needs the Favorites schema migration, which is
 * Session 6, not this round.
 *
 * Credits, Similar, Recommendations, and the trailer key are each fetched in their own
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

    init {
        viewModelScope.launch {
            repository.getTvDetails(tvId)
                .onSuccess { details ->
                    _uiState.update { it.copy(details = details, isLoading = false, errorMessage = null) }
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message ?: "Couldn't load this show") }
                }
        }
        viewModelScope.launch {
            repository.getTvCredits(tvId)
                .onSuccess { credits -> _uiState.update { it.copy(cast = credits.cast, director = credits.director) } }
            // Deliberately no onFailure handling — see the class doc above.
        }
        viewModelScope.launch {
            repository.getSimilarTvShows(tvId)
                .onSuccess { shows -> _uiState.update { it.copy(similarTvShows = shows) } }
            // Deliberately no onFailure handling — see the class doc above.
        }
        viewModelScope.launch {
            repository.getRecommendedTvShows(tvId)
                .onSuccess { shows -> _uiState.update { it.copy(recommendedTvShows = shows) } }
            // Deliberately no onFailure handling — see the class doc above.
        }
        viewModelScope.launch {
            repository.getTvTrailerKey(tvId)
                .onSuccess { key -> _uiState.update { it.copy(trailerKey = key) } }
            // Deliberately no onFailure handling — see the class doc above.
        }
    }
}
