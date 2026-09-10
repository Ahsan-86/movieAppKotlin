package com.ahsan.movieapp.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahsan.movieapp.data.repository.MovieRepository
import com.ahsan.movieapp.domain.model.CastMember
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.domain.model.MovieDetails
import com.ahsan.movieapp.domain.model.Person
import com.ahsan.movieapp.domain.model.WatchProviderRegion
import com.ahsan.movieapp.domain.model.WatchProviders
import com.ahsan.movieapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class MovieDetailUiState(
    val details: MovieDetails? = null,
    val cast: List<CastMember> = emptyList(),
    val director: Person? = null,
    val similarMovies: List<Movie> = emptyList(),
    val recommendedMovies: List<Movie> = emptyList(),
    // Round C — streaming availability. watchProviders is the SELECTED region's Stream/Rent/Buy
    // lists (null while loading, or if TMDB has no data for the selected region); watchRegions is
    // every region TMDB returned data for, for the region dropdown; selectedRegion drives both.
    val watchProviders: WatchProviderRegion? = null,
    val watchRegions: List<String> = emptyList(),
    val selectedRegion: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

/** Cast/director slice fetched separately from [MovieDetails] — see [MovieDetailViewModel.uiState]. */
private data class CreditsState(
    val cast: List<CastMember> = emptyList(),
    val director: Person? = null
)

/**
 * Streaming-availability slice fetched separately from [MovieDetails] (Phase 3 Round C) — same
 * failure-tolerant, separately-fetched pattern as [CreditsState]. [selectedRegion] is the Detail
 * screen's own local, non-persistent region picker (Ahsan's confirmed Round C scope): it starts at
 * [defaultRegionCode] every time this ViewModel is created (i.e. every time the Detail screen is
 * opened for a movie) and is never saved, so it never carries over between movies or app sessions.
 * A single `/watch/providers` fetch covers every region, so changing [selectedRegion] is pure
 * client-side lookup — never a re-fetch.
 */
private data class WatchState(
    val providers: WatchProviders? = null,
    val selectedRegion: String = defaultRegionCode()
)

/**
 * The device's own region as an ISO 3166-1 country code (e.g. "US", "GB"), falling back to "US"
 * when the locale doesn't resolve to one — per Ahsan's confirmed Round C scope ("locale-derived
 * default, US fallback"). If TMDB turns out to have no watch-provider data for this region either,
 * [MovieDetailViewModel.init] falls back to "US" a second time once the fetch actually completes.
 */
private fun defaultRegionCode(): String = Locale.getDefault().country.takeIf { it.isNotBlank() } ?: "US"

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MovieRepository
) : ViewModel() {

    private val movieId: Int = checkNotNull(savedStateHandle["movieId"])

    // Cast + director come from a separate one-shot fetch (MovieRepository.getMovieCredits, Phase
    // 3 Round A) rather than the offline-first getMovieDetails() flow above, so they're folded in
    // via combine() rather than another field on MovieDetails itself. A failed credits fetch just
    // leaves this at its default (empty cast, no director) — the cast/crew section simply doesn't
    // render rather than blocking the rest of the screen, since MovieDetails is the primary thing
    // this screen needs to show.
    private val creditsState = MutableStateFlow(CreditsState())
    private val watchState = MutableStateFlow(WatchState())

    val uiState: StateFlow<MovieDetailUiState> = combine(
        repository.getMovieDetails(movieId),
        creditsState,
        repository.getSimilarMovies(movieId),
        repository.getRecommendedMovies(movieId),
        watchState
    ) { resource, credits, similar, recommended, watch ->
        MovieDetailUiState(
            details = resource.data,
            cast = credits.cast,
            director = credits.director,
            // Similar/Recommendations are supplementary — a Loading or Error state for either just
            // means "nothing to show there yet", never blocks the rest of the screen (same
            // failure-tolerant treatment as credits above).
            similarMovies = similar.data.orEmpty(),
            recommendedMovies = recommended.data.orEmpty(),
            // Watch providers are supplementary too — same failure-tolerant treatment. Only the
            // SELECTED region's lists are exposed; watchRegions backs the dropdown itself.
            watchProviders = watch.providers?.forRegion(watch.selectedRegion),
            watchRegions = watch.providers?.regionsAvailable.orEmpty(),
            selectedRegion = watch.selectedRegion,
            isLoading = resource is Resource.Loading && resource.data == null,
            errorMessage = (resource as? Resource.Error)?.message?.takeIf { resource.data == null }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MovieDetailUiState(selectedRegion = defaultRegionCode()))

    init {
        viewModelScope.launch {
            repository.getMovieCredits(movieId)
                .onSuccess { credits -> creditsState.value = CreditsState(cast = credits.cast, director = credits.director) }
            // Deliberately no onFailure handling here — see the comment on creditsState above.
        }
        viewModelScope.launch {
            repository.getWatchProviders(movieId)
                .onSuccess { providers ->
                    watchState.update { current ->
                        // Prefer the device-locale region if TMDB actually has data for it; else
                        // fall back to US; else leave the locale default selected as-is (the
                        // section then just shows "not available" for that region, which is
                        // correct — nothing to silently substitute).
                        val resolvedRegion = when {
                            current.selectedRegion in providers.regionsAvailable -> current.selectedRegion
                            "US" in providers.regionsAvailable -> "US"
                            else -> current.selectedRegion
                        }
                        current.copy(providers = providers, selectedRegion = resolvedRegion)
                    }
                }
            // Deliberately no onFailure handling here — see the comment on WatchState above.
        }
    }

    /** Backs the Detail screen's region dropdown (Round C) — pure client-side switch, no re-fetch. */
    fun setWatchRegion(regionCode: String) {
        watchState.update { it.copy(selectedRegion = regionCode) }
    }

    fun toggleFavorite() {
        val details = uiState.value.details ?: return
        viewModelScope.launch {
            repository.toggleFavorite(
                Movie(
                    id = details.id,
                    title = details.title,
                    overview = details.overview,
                    posterUrl = details.posterUrl,
                    backdropUrl = details.backdropUrl,
                    releaseDate = details.releaseDate,
                    voteAverage = details.voteAverage,
                    voteCount = details.voteCount,
                    isFavorite = details.isFavorite
                )
            )
        }
    }
}
