package com.ahsan.movieapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahsan.movieapp.data.repository.MovieRepository
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.domain.model.MovieCategory
import com.ahsan.movieapp.util.NetworkConnectivityObserver
import com.ahsan.movieapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeSection(
    val title: String,
    val movies: List<Movie>,
    val isLoading: Boolean,
    val errorMessage: String? = null
)

data class HomeUiState(
    val sections: List<HomeSection> = emptyList(),
    val isOffline: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MovieRepository,
    connectivityObserver: NetworkConnectivityObserver
) : ViewModel() {

    private val sectionTitles = listOf(
        "Trending Today" to MovieCategory.TRENDING_TODAY,
        "Popular" to MovieCategory.POPULAR,
        "For You" to null, // special-cased below to call getForYou()
        "Now Playing" to MovieCategory.NOW_PLAYING,
        "Top Rated" to MovieCategory.TOP_RATED,
        "Upcoming" to MovieCategory.UPCOMING
    )

    private val sectionFlows = sectionTitles.map { (_, category) ->
        if (category == null) repository.getForYou() else repository.getCategory(category)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        combine(*sectionFlows.toTypedArray()) { results -> results.toList() },
        connectivityObserver.isOnline
    ) { results, isOnline ->
        val sections = sectionTitles.mapIndexed { index, (title, _) ->
            val resource = results[index]
            HomeSection(
                title = title,
                movies = resource.data.orEmpty(),
                isLoading = resource is Resource.Loading && resource.data.isNullOrEmpty(),
                errorMessage = (resource as? Resource.Error)?.message
            )
        }
        HomeUiState(sections = sections, isOffline = !isOnline)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(
            sections = sectionTitles.map { (title, _) -> HomeSection(title, emptyList(), isLoading = true) }
        )
    )

    fun toggleFavorite(movie: Movie) {
        viewModelScope.launch {
            repository.toggleFavorite(movie)
        }
    }
}
