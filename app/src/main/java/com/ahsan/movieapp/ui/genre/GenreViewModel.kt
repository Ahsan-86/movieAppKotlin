package com.ahsan.movieapp.ui.genre

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahsan.movieapp.data.repository.MovieRepository
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.ui.person.MediaTab
import com.ahsan.movieapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

data class GenreUiState(
    val genreName: String,
    val selectedMediaType: MediaTab,
    val showMoviesTab: Boolean,
    val showTvTab: Boolean,
    val movies: List<Movie> = emptyList(),
    val tvShows: List<Movie> = emptyList(),
    val isLoadingMovies: Boolean = false,
    val isLoadingTv: Boolean = false,
    val tvLoaded: Boolean = false,
    val errorMessage: String? = null
) {
    val displayedItems: List<Movie> get() = if (selectedMediaType == MediaTab.TV) tvShows else movies

    val isLoading: Boolean
        get() = if (selectedMediaType == MediaTab.TV) isLoadingTv && tvShows.isEmpty() else isLoadingMovies && movies.isEmpty()
}

/**
 * Backs the full-screen genre browse view opened by tapping a genre chip on the search screen.
 * Movies go through [MovieRepository.browseGenre] (offline-first, cached, same as everything
 * else in Explore). TV shows are fetched once via [MovieRepository.browseGenreTv] the first time
 * that tab is opened — network-only, no cache, mirroring how the person screen's TV credits work,
 * since this app doesn't persist TV data yet.
 */
@HiltViewModel
class GenreViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MovieRepository
) : ViewModel() {

    private val movieGenreId: Int? = (savedStateHandle.get<Int>("movieGenreId") ?: -1).takeIf { it != -1 }
    private val tvGenreId: Int? = (savedStateHandle.get<Int>("tvGenreId") ?: -1).takeIf { it != -1 }
    private val genreName: String = URLDecoder.decode(checkNotNull(savedStateHandle.get<String>("genreName")), "UTF-8")

    private val _uiState = MutableStateFlow(
        GenreUiState(
            genreName = genreName,
            selectedMediaType = if (movieGenreId != null) MediaTab.MOVIES else MediaTab.TV,
            showMoviesTab = movieGenreId != null,
            showTvTab = tvGenreId != null,
            isLoadingMovies = movieGenreId != null,
            isLoadingTv = movieGenreId == null && tvGenreId != null
        )
    )
    val uiState: StateFlow<GenreUiState> = _uiState.asStateFlow()

    init {
        if (movieGenreId != null) {
            repository.browseGenre(movieGenreId)
                .onEach { resource ->
                    _uiState.update {
                        it.copy(
                            movies = resource.data.orEmpty(),
                            isLoadingMovies = resource is Resource.Loading && resource.data.isNullOrEmpty(),
                            errorMessage = (resource as? Resource.Error)?.message?.takeIf { resource.data.isNullOrEmpty() }
                        )
                    }
                }
                .launchIn(viewModelScope)
        } else {
            // No movie genre for this chip — TV is the only tab, so load it right away.
            tvGenreId?.let { loadTv(it) }
        }
    }

    fun onMediaTabSelected(tab: MediaTab) {
        _uiState.update { it.copy(selectedMediaType = tab) }
        if (tab == MediaTab.TV && !_uiState.value.tvLoaded) {
            tvGenreId?.let { loadTv(it) }
        }
    }

    fun toggleFavorite(movie: Movie) {
        viewModelScope.launch {
            repository.toggleFavorite(movie)
            // TV shows can't be favorited (no schema support yet) so this only ever touches movies.
            _uiState.update { state ->
                state.copy(movies = state.movies.map { if (it.id == movie.id) it.copy(isFavorite = !it.isFavorite) else it })
            }
        }
    }

    private fun loadTv(genreId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTv = true) }
            repository.browseGenreTv(genreId)
                .onSuccess { shows ->
                    _uiState.update { it.copy(tvShows = shows, isLoadingTv = false, tvLoaded = true, errorMessage = null) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isLoadingTv = false, tvLoaded = true, errorMessage = throwable.message ?: "Couldn't load TV shows")
                    }
                }
        }
    }
}
