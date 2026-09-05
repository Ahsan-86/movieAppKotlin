package com.ahsan.movieapp.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahsan.movieapp.data.repository.MovieRepository
import com.ahsan.movieapp.domain.model.MovieDetails
import com.ahsan.movieapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MovieDetailUiState(
    val details: MovieDetails? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MovieRepository
) : ViewModel() {

    private val movieId: Int = checkNotNull(savedStateHandle["movieId"])

    val uiState: StateFlow<MovieDetailUiState> = repository.getMovieDetails(movieId)
        .map { resource ->
            MovieDetailUiState(
                details = resource.data,
                isLoading = resource is Resource.Loading && resource.data == null,
                errorMessage = (resource as? Resource.Error)?.message?.takeIf { resource.data == null }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MovieDetailUiState())

    fun toggleFavorite() {
        val details = uiState.value.details ?: return
        viewModelScope.launch {
            repository.toggleFavorite(
                com.ahsan.movieapp.domain.model.Movie(
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
