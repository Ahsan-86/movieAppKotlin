package com.ahsan.movieapp.ui.trending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahsan.movieapp.data.repository.MovieRepository
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.domain.model.MovieCategory
import com.ahsan.movieapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrendingUiState(
    val movies: List<Movie> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TrendingViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    // Same reasoning as HomeViewModel.refreshTrigger: networkBoundResource never retries a failed
    // fetch on its own, so a real "Retry" has to restart the whole flow via flatMapLatest rather
    // than just poking the repository and hoping a stale Resource.Error clears itself.
    private val refreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<TrendingUiState> = refreshTrigger
        .flatMapLatest { repository.getCategory(MovieCategory.TRENDING_TODAY) }
        .map { resource ->
            TrendingUiState(
                movies = resource.data.orEmpty(),
                isLoading = resource is Resource.Loading && resource.data.isNullOrEmpty(),
                errorMessage = (resource as? Resource.Error)?.message?.takeIf { resource.data.isNullOrEmpty() }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrendingUiState())

    fun toggleFavorite(movie: Movie) {
        viewModelScope.launch { repository.toggleFavorite(movie) }
    }

    fun retry() {
        refreshTrigger.value++
    }
}
