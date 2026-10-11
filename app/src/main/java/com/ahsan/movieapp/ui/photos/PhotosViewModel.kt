package com.ahsan.movieapp.ui.photos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahsan.movieapp.data.repository.MovieRepository
import com.ahsan.movieapp.domain.model.Photo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PhotosUiState(
    val photos: List<Photo> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

/**
 * Backs both the "See all" photos grid and the fullscreen photo viewer (Phase 5): each is its own
 * nav destination carrying `mediaType` ("movie" / "tv") + `mediaId`, and re-requests the list by id
 * like every other drill-down in this app. The repository keeps recent results in memory, so these
 * calls normally answer instantly from the fetch the detail screen's Photos row already made.
 * [startIndex] is only present on the viewer's route.
 */
@HiltViewModel
class PhotosViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MovieRepository
) : ViewModel() {

    private val mediaType: String = checkNotNull(savedStateHandle["mediaType"])
    private val mediaId: Int = checkNotNull(savedStateHandle["mediaId"])
    val startIndex: Int = savedStateHandle.get<Int>("startIndex") ?: 0
    val isTv: Boolean = mediaType == MEDIA_TV

    private val _uiState = MutableStateFlow(PhotosUiState())
    val uiState: StateFlow<PhotosUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() {
        _uiState.update { PhotosUiState() }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val result = if (mediaType == MEDIA_TV) repository.getTvImages(mediaId) else repository.getMovieImages(mediaId)
            result
                .onSuccess { photos -> _uiState.update { it.copy(photos = photos, isLoading = false, errorMessage = null) } }
                .onFailure { throwable -> _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message) } }
        }
    }

    companion object {
        const val MEDIA_MOVIE = "movie"
        const val MEDIA_TV = "tv"
    }
}
