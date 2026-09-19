package com.ahsan.movieapp.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahsan.movieapp.data.repository.MovieRepository
import com.ahsan.movieapp.domain.model.CastMember
import com.ahsan.movieapp.domain.model.Person
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CastCrewUiState(
    val cast: List<CastMember> = emptyList(),
    val director: Person? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val isEmpty: Boolean get() = cast.isEmpty() && director == null
}

/**
 * Backs Phase 3 Round A's "view all" screen — the full cast list plus the director, reached from
 * MovieDetailScreen's (and now TvDetailScreen's) cast/crew section heading arrow. Re-fetches
 * [MovieRepository.getMovieCredits]/[MovieRepository.getTvCredits] by the route's media id rather
 * than sharing the detail ViewModel's state, same as every other full-screen destination in this
 * app (Person, Genre) re-fetches its own data by id. The movie and TV routes each pass exactly one
 * id, in their own key (`movieId` or `tvId`), so this VM picks whichever is present — the
 * [com.ahsan.movieapp.ui.detail.CastCrewListScreen] itself is media-agnostic.
 */
@HiltViewModel
class CastCrewListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MovieRepository
) : ViewModel() {

    private val movieId: Int? = savedStateHandle["movieId"]
    private val tvId: Int? = savedStateHandle["tvId"]

    private val _uiState = MutableStateFlow(CastCrewUiState())
    val uiState: StateFlow<CastCrewUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Check the movie flag (not `movieId != null`) so a movie route legitimately using id 0
            // still resolves to the movie fetch.
            val id = movieId ?: tvId ?: return@launch
            val result = if (savedStateHandle.contains("movieId")) {
                repository.getMovieCredits(id)
            } else {
                repository.getTvCredits(id)
            }
            result
                .onSuccess { credits ->
                    _uiState.update {
                        it.copy(cast = credits.cast, director = credits.director, isLoading = false, errorMessage = null)
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = throwable.message ?: "Couldn't load cast & crew")
                    }
                }
        }
    }
}
