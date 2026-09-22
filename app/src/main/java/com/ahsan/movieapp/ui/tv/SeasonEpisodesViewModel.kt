package com.ahsan.movieapp.ui.tv

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahsan.movieapp.data.repository.MovieRepository
import com.ahsan.movieapp.domain.model.Episode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

data class SeasonEpisodesUiState(
    val seasonName: String,
    val episodes: List<Episode> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

/**
 * Phase 2.6 Session 2 — the TV detail screen's Seasons section drill-down: one season's full
 * episode list. One-shot, network-only, no Room cache (same convention as [TvDetailViewModel]).
 * [seasonName] seeds the initial title from the nav arg (same "show a title immediately, before
 * the fetch completes" convention as [com.ahsan.movieapp.ui.detail.CollectionScreen]'s
 * collectionName arg) and is never overwritten by the fetch result — TMDB's season name would be
 * identical anyway. Purely informational — tapping an episode does nothing yet (parked for Phase 6
 * discussion, per the project doc's Open items).
 */
@HiltViewModel
class SeasonEpisodesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MovieRepository
) : ViewModel() {

    private val tvId: Int = checkNotNull(savedStateHandle["tvId"])
    private val seasonNumber: Int = checkNotNull(savedStateHandle["seasonNumber"])
    private val seasonName: String = URLDecoder.decode(checkNotNull(savedStateHandle["seasonName"]), "UTF-8")

    private val _uiState = MutableStateFlow(SeasonEpisodesUiState(seasonName = seasonName))
    val uiState: StateFlow<SeasonEpisodesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getSeasonDetails(tvId, seasonNumber)
                .onSuccess { season ->
                    _uiState.update { it.copy(episodes = season.episodes, isLoading = false, errorMessage = null) }
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message) }
                }
        }
    }
}
