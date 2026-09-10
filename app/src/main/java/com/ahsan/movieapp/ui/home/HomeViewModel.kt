package com.ahsan.movieapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahsan.movieapp.data.repository.MovieRepository
import com.ahsan.movieapp.domain.model.GenreChip
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.domain.model.MovieCategory
import com.ahsan.movieapp.util.NetworkConnectivityObserver
import com.ahsan.movieapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeSection(
    val title: String,
    val movies: List<Movie>,
    val isLoading: Boolean,
    val errorMessage: String? = null,
    // False for the TV shows row — TV items can't be favorited yet (no schema support), same
    // one-screen exception as the genre screen's TV tab.
    val allowFavoriting: Boolean = true,
    // True only for the "Popular TV Shows" row — its items reuse the Movie model (see
    // data/mapper/MovieMappers.kt's TvShowDto.toMovie()) but their id is a TV id, not a movie id,
    // so HomeScreen needs to know not to route a tap through the normal onMovieClick. See
    // util/TvNavigation.kt for the full explanation.
    val isTv: Boolean = false
)

data class HomeUiState(
    // Top-of-screen hero banner — the first [HERO_MOVIE_COUNT] movies from the "Popular" section,
    // reusing that section's already-fetched data rather than a second network call.
    val heroMovies: List<Movie> = emptyList(),
    val genreChips: List<GenreChip> = emptyList(),
    val sections: List<HomeSection> = emptyList(),
    val isOffline: Boolean = false
)

/** One row in [HomeViewModel.sectionSources] — where a carousel's movies come from. */
private data class HomeSectionSource(
    val title: String,
    val category: MovieCategory? = null,
    val isTv: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MovieRepository,
    connectivityObserver: NetworkConnectivityObserver
) : ViewModel() {

    // "Popular" doubles as the hero banner's source (see heroSectionIndex below) — its own row
    // still shows further down, same as a Netflix-style hero-plus-shelf layout. "Popular TV Shows"
    // (originally "Trending Today") was moved to the bottom and switched to real TV data on
    // request, rather than keeping a "TV Shows" label over trending-movie data.
    private val sectionSources = listOf(
        HomeSectionSource("Popular", category = MovieCategory.POPULAR),
        HomeSectionSource("For You"),
        HomeSectionSource("Now Playing", category = MovieCategory.NOW_PLAYING),
        HomeSectionSource("Top Rated", category = MovieCategory.TOP_RATED),
        HomeSectionSource("Upcoming", category = MovieCategory.UPCOMING),
        HomeSectionSource("Popular TV Shows", isTv = true)
    )

    private val heroSectionIndex = sectionSources.indexOfFirst { it.category == MovieCategory.POPULAR }

    // networkBoundResource never retries on its own — once a fetch() fails, that flow collection
    // stays wrapped in Resource.Error for the rest of its lifetime, permanently re-mapping every
    // later database change through the *same* stale error. Bumping this and re-deriving the
    // section flows through flatMapLatest is what actually restarts each pipeline from scratch
    // (fresh query().first(), fresh shouldFetch check, fresh fetch() attempt) — a real "Retry",
    // not just a fresh network call layered under an error that would never clear.
    private val refreshTrigger = MutableStateFlow(0)

    private fun currentSectionFlows(): List<Flow<Resource<List<Movie>>>> = sectionSources.map { source ->
        when {
            source.isTv -> popularTvFlow()
            source.category != null -> repository.getCategory(source.category)
            else -> repository.getForYou()
        }
    }

    /** Wraps the one-shot [MovieRepository.getPopularTv] call as a Resource flow so it can slot
     *  into the same combine/retry pipeline as every cached movie category. */
    private fun popularTvFlow(): Flow<Resource<List<Movie>>> = flow {
        emit(Resource.Loading())
        repository.getPopularTv().fold(
            onSuccess = { movies -> emit(Resource.Success(movies)) },
            onFailure = { throwable -> emit(Resource.Error(throwable.message ?: "Couldn't load popular TV shows")) }
        )
    }

    /** Fetched once — genre chips don't need to react to retry() or reload on every section refresh. */
    private fun genreChipsFlow(): Flow<List<GenreChip>> = flow {
        emit(repository.getGenreChips().getOrDefault(emptyList()))
    }

    val uiState: StateFlow<HomeUiState> = combine(
        refreshTrigger.flatMapLatest { combine(*currentSectionFlows().toTypedArray()) { results -> results.toList() } },
        connectivityObserver.isOnline,
        genreChipsFlow()
    ) { results, isOnline, genreChips ->
        val sections = sectionSources.mapIndexed { index, source ->
            val resource = results[index]
            HomeSection(
                title = source.title,
                movies = resource.data.orEmpty(),
                isLoading = resource is Resource.Loading && resource.data.isNullOrEmpty(),
                errorMessage = (resource as? Resource.Error)?.message,
                allowFavoriting = !source.isTv,
                isTv = source.isTv
            )
        }
        HomeUiState(
            heroMovies = results[heroSectionIndex].data.orEmpty().take(HERO_MOVIE_COUNT),
            genreChips = genreChips,
            sections = sections,
            isOffline = !isOnline
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(
            sections = sectionSources.map { source ->
                HomeSection(source.title, emptyList(), isLoading = true, allowFavoriting = !source.isTv, isTv = source.isTv)
            }
        )
    )

    fun toggleFavorite(movie: Movie) {
        viewModelScope.launch {
            repository.toggleFavorite(movie)
        }
    }

    /** Restarts every carousel's fetch from scratch — wired to the "Retry" button that shows up
     *  in a carousel that has nothing cached and just failed to load. */
    fun retry() {
        refreshTrigger.value++
    }

    companion object {
        private const val HERO_MOVIE_COUNT = 8
    }
}
