package com.ahsan.movieapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahsan.movieapp.R
import com.ahsan.movieapp.data.repository.MovieRepository
import com.ahsan.movieapp.domain.model.GenreChip
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.domain.model.MovieCategory
import com.ahsan.movieapp.domain.model.TvCategory
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
    val labelRes: Int,
    val movies: List<Movie>,
    val isLoading: Boolean,
    val errorMessage: String? = null,
    // True only for the TV rows — their items reuse the Movie model (see TvShowEntity.toDomain)
    // but their id is a TV id, not a movie id, so HomeScreen needs to know not to route a tap
    // through the normal onMovieClick. Favoriting is available on every row since Session 6: the
    // composite-key Favorites table holds TV ids separately, and getCategoryTv already re-stamps
    // each row's heart against the live favorites flow (see MovieRepositoryImpl).
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

/** One row in [HomeViewModel.sectionSources] — where a carousel's movies come from. [genreMovieId]
 *  and [genreTvId] back the curated genre rows (TMDB `/discover`), mutually exclusive with [category]/
 *  [categoryTv] and with each other. */
private data class HomeSectionSource(
    val labelRes: Int,
    val category: MovieCategory? = null,
    val categoryTv: TvCategory? = null,
    val genreMovieId: Int? = null,
    val genreTvId: Int? = null,
    val isTv: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MovieRepository,
    connectivityObserver: NetworkConnectivityObserver
) : ViewModel() {

    // Session 4's agreed Explore reorder: movies on top (hero + Popular row, For You, Upcoming,
    // then a curated Sci-Fi movie row), TV below (Popular, On The Air — the natural TV analog of
    // Upcoming, since there's no `/tv/upcoming` — then curated Sci-Fi TV). Now Playing, Top Rated
    // and the Trending TV row dropped from Explore (Trending keeps its own screen), and For You
    // stays movies-only — there's no mixed movie+TV data source to back a hybrid row, and we don't
    // fake one. The TV rows are backed by the offline-first `tv_shows` Room cache (see
    // MovieRepository.getCategoryTv), same as every movie carousel.
    private val sectionSources = listOf(
        HomeSectionSource(R.string.section_popular, category = MovieCategory.POPULAR),
        HomeSectionSource(R.string.section_for_you),
        HomeSectionSource(R.string.section_upcoming, category = MovieCategory.UPCOMING),
        HomeSectionSource(R.string.section_sci_fi_movies, genreMovieId = SCI_FI_MOVIE_GENRE_ID),
        HomeSectionSource(R.string.home_popular_tv_shows, categoryTv = TvCategory.POPULAR_TV, isTv = true),
        HomeSectionSource(R.string.home_on_the_air_tv_shows, categoryTv = TvCategory.ON_THE_AIR, isTv = true),
        HomeSectionSource(R.string.home_sci_fi_tv_shows, genreTvId = SCI_FI_TV_GENRE_ID, isTv = true)
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
            source.genreTvId != null -> repository.browseGenreTv(source.genreTvId)
            source.isTv -> repository.getCategoryTv(source.categoryTv!!)
            source.category != null -> repository.getCategory(source.category)
            source.genreMovieId != null -> repository.browseGenre(source.genreMovieId)
            else -> repository.getForYou()
        }
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
                labelRes = source.labelRes,
                movies = resource.data.orEmpty(),
                isLoading = resource is Resource.Loading && resource.data.isNullOrEmpty(),
                errorMessage = (resource as? Resource.Error)?.message,
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
                HomeSection(source.labelRes, emptyList(), isLoading = true, isTv = source.isTv)
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
        // The Sci-Fi movie and TV rows reuse the same curated genre ids as the Science Fiction
        // genre chip (see MovieRepositoryImpl.CURATED_GENRES) — "/discover" with genre 878 (movies)
        // and 10765 (TV).
        private const val SCI_FI_MOVIE_GENRE_ID = 878
        private const val SCI_FI_TV_GENRE_ID = 10765
        private const val HERO_MOVIE_COUNT = 8
    }
}
