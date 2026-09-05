package com.ahsan.movieapp.data.repository

import com.ahsan.movieapp.data.local.dao.FavoriteDao
import com.ahsan.movieapp.data.local.dao.MovieDao
import com.ahsan.movieapp.data.local.entity.FavoriteEntity
import com.ahsan.movieapp.data.local.entity.MovieEntity
import com.ahsan.movieapp.data.mapper.combineToMovieDetails
import com.ahsan.movieapp.data.mapper.toDomain
import com.ahsan.movieapp.data.mapper.toEntity
import com.ahsan.movieapp.data.mapper.toMovieEntity
import com.ahsan.movieapp.data.remote.TmdbApi
import com.ahsan.movieapp.data.remote.dto.MovieDto
import com.ahsan.movieapp.domain.model.CastMember
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.domain.model.MovieCategory
import com.ahsan.movieapp.domain.model.MovieDetails
import com.ahsan.movieapp.domain.model.Person
import com.ahsan.movieapp.util.Resource
import com.ahsan.movieapp.util.networkBoundResource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieRepositoryImpl @Inject constructor(
    private val api: TmdbApi,
    private val movieDao: MovieDao,
    private val favoriteDao: FavoriteDao
) : MovieRepository {

    override fun getCategory(category: MovieCategory): Flow<Resource<List<Movie>>> =
        cachedCategoryFlow(
            storageKey = category.storageKey,
            fetch = { fetchCategoryFromNetwork(category) }
        )

    override fun getForYou(): Flow<Resource<List<Movie>>> =
        cachedCategoryFlow(
            storageKey = MovieCategory.FOR_YOU.storageKey,
            fetch = {
                val genreIds = favoriteDao.getFavoriteGenreIdLists().flatten().distinct()
                if (genreIds.isEmpty()) {
                    // No favorites yet to personalize from — fall back to what's popular.
                    api.getPopular().results
                } else {
                    api.discoverByGenres(genreIds.take(3).joinToString(",")).results
                }
            }
        )

    override fun getMovieDetails(movieId: Int): Flow<Resource<MovieDetails>> = networkBoundResource(
        query = {
            combine(
                movieDao.observeMovie(movieId),
                movieDao.observeMovieDetails(movieId),
                favoriteDao.isFavorite(movieId)
            ) { base, extra, favorite ->
                base?.let { combineToMovieDetails(it, extra, favorite) }
            }
        },
        fetch = { api.getMovieDetails(movieId) },
        saveFetchResult = { dto ->
            val now = System.currentTimeMillis()
            movieDao.upsertMovies(listOf(dto.toMovieEntity(now)))
            movieDao.upsertMovieDetails(dto.toEntity(now))
        },
        shouldFetch = { true }
    ).map { resource ->
        // The combine() above can legitimately produce a null (row not cached yet, first ever fetch
        // still in flight) — filter that down into Loading rather than surfacing a null payload.
        when (resource) {
            is Resource.Loading -> Resource.Loading(resource.data)
            is Resource.Success -> resource.data?.let { Resource.Success(it, resource.isFromCache) }
                ?: Resource.Loading(null)
            is Resource.Error -> resource.data?.let { Resource.Error(resource.message ?: "", it) }
                ?: Resource.Error(resource.message ?: "Something went wrong")
        }
    }

    override fun getCast(movieId: Int): Flow<Resource<List<CastMember>>> = networkBoundResource(
        query = { movieDao.observeCast(movieId).map { list -> list.map { it.toDomain() } } },
        fetch = { api.getMovieCredits(movieId).cast },
        saveFetchResult = { cast ->
            movieDao.replaceCast(movieId, cast.map { it.toEntity(movieId) })
        },
        shouldFetch = { it.isEmpty() }
    )

    override fun getSimilarMovies(movieId: Int): Flow<Resource<List<Movie>>> =
        cachedCategoryFlow(
            storageKey = "similar_$movieId",
            fetch = { api.getSimilarMovies(movieId).results }
        )

    override suspend fun searchMovies(query: String): Result<List<Movie>> = runCatching {
        val now = System.currentTimeMillis()
        val results = api.searchMovies(query).results
        movieDao.upsertMovies(results.map { it.toEntity(now) })
        val favoriteIds = favoriteDao.observeFavoriteMovies().first().map { it.id }.toSet()
        results.map { it.toEntity(now).toDomain(isFavorite = it.id in favoriteIds) }
    }

    override suspend fun searchPeople(query: String): Result<List<Person>> = runCatching {
        api.searchPeople(query).results.map { it.toDomain() }
    }

    override fun getPersonFilmography(personId: Int, personName: String): Flow<Resource<List<Movie>>> =
        cachedCategoryFlow(
            storageKey = "person_$personId",
            fetch = { api.discoverByCast(personId).results }
        )

    override fun observeFavorites(): Flow<List<Movie>> =
        favoriteDao.observeFavoriteMovies().map { list -> list.map { it.toDomain(isFavorite = true) } }

    override fun isFavorite(movieId: Int): Flow<Boolean> = favoriteDao.isFavorite(movieId)

    override suspend fun toggleFavorite(movie: Movie) {
        val currentlyFavorite = favoriteDao.isFavorite(movie.id).first()
        if (currentlyFavorite) {
            favoriteDao.removeFavorite(movie.id)
        } else {
            movieDao.upsertMovies(
                listOf(
                    MovieEntity(
                        id = movie.id,
                        title = movie.title,
                        overview = movie.overview,
                        posterPath = movie.posterUrl?.substringAfterLast("/")?.let { "/$it" },
                        backdropPath = movie.backdropUrl?.substringAfterLast("/")?.let { "/$it" },
                        releaseDate = movie.releaseDate,
                        voteAverage = movie.voteAverage,
                        voteCount = movie.voteCount,
                        genreIds = movie.genreIds,
                        cachedAt = System.currentTimeMillis()
                    )
                )
            )
            favoriteDao.addFavorite(FavoriteEntity(movieId = movie.id, addedAt = System.currentTimeMillis()))
        }
    }

    override suspend fun refreshAllCategories(): Result<Unit> = runCatching {
        MovieCategory.entries.filter { it != MovieCategory.FOR_YOU }.forEach { category ->
            val movies = fetchCategoryFromNetwork(category)
            movieDao.replaceCategory(category.storageKey, movies.map { it.toEntity(System.currentTimeMillis()) }, System.currentTimeMillis())
        }
    }

    private suspend fun fetchCategoryFromNetwork(category: MovieCategory): List<MovieDto> = when (category) {
        MovieCategory.TRENDING_TODAY -> api.getTrendingToday().results
        MovieCategory.POPULAR -> api.getPopular().results
        MovieCategory.TOP_RATED -> api.getTopRated().results
        MovieCategory.NOW_PLAYING -> api.getNowPlaying().results
        MovieCategory.UPCOMING -> api.getUpcoming().results
        MovieCategory.FOR_YOU -> api.getPopular().results
    }

    /** Shared plumbing for every screen that's "just a list of movies under some cache key". */
    private fun cachedCategoryFlow(
        storageKey: String,
        fetch: suspend () -> List<MovieDto>
    ): Flow<Resource<List<Movie>>> = networkBoundResource(
        query = {
            combine(
                movieDao.observeCategory(storageKey),
                favoriteDao.observeFavoriteMovies()
            ) { movies, favorites ->
                val favoriteIds = favorites.map { it.id }.toSet()
                movies.map { it.toDomain(isFavorite = it.id in favoriteIds) }
            }
        },
        fetch = fetch,
        saveFetchResult = { dtos ->
            val now = System.currentTimeMillis()
            movieDao.replaceCategory(storageKey, dtos.map { it.toEntity(now) }, now)
        },
        shouldFetch = { cached ->
            cached.isEmpty() || isStale(storageKey)
        }
    )

    private suspend fun isStale(storageKey: String): Boolean {
        val fetchedAt = movieDao.categoryFetchedAt(storageKey) ?: return true
        return System.currentTimeMillis() - fetchedAt > STALE_THRESHOLD_MS
    }

    companion object {
        private const val STALE_THRESHOLD_MS = 2 * 60 * 60 * 1000L // 2 hours
    }
}
