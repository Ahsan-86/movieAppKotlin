package com.ahsan.movieapp.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahsan.movieapp.data.repository.MovieRepository
import com.ahsan.movieapp.domain.model.Movie
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

data class CollectionUiState(
    // Seeded from the nav arg so the top bar has a real title immediately, before the fetch below
    // completes — overwritten with TMDB's own name once it comes back (they should always match).
    val name: String,
    val overview: String? = null,
    val movies: List<Movie> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

/**
 * Backs Phase 3 Round B's collection teaser destination — the full list of movies in a franchise,
 * reached from the Detail screen's "part of a collection" row. Re-fetches
 * [MovieRepository.getCollectionDetails] by collectionId rather than sharing
 * MovieDetailViewModel's state, same as every other full-screen destination in this app (Person,
 * Genre, CastCrewList) re-fetches its own data by id.
 */
@HiltViewModel
class CollectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MovieRepository
) : ViewModel() {

    private val collectionId: Int = checkNotNull(savedStateHandle["collectionId"])
    private val collectionNameArg: String =
        URLDecoder.decode(checkNotNull(savedStateHandle.get<String>("collectionName")), "UTF-8")

    private val _uiState = MutableStateFlow(CollectionUiState(name = collectionNameArg))
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getCollectionDetails(collectionId)
                .onSuccess { collection ->
                    _uiState.update {
                        it.copy(
                            name = collection.name.ifBlank { collectionNameArg },
                            overview = collection.overview,
                            movies = collection.movies,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = throwable.message)
                    }
                }
        }
    }

    fun toggleFavorite(movie: Movie) {
        viewModelScope.launch {
            repository.toggleFavorite(movie)
            _uiState.update { state ->
                state.copy(movies = state.movies.map { if (it.id == movie.id) it.copy(isFavorite = !it.isFavorite) else it })
            }
        }
    }
}
