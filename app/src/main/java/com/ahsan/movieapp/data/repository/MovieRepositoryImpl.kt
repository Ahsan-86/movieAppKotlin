package com.ahsan.movieapp.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.ahsan.movieapp.data.local.AppDatabase
import com.ahsan.movieapp.data.local.dao.FavoriteDao
import com.ahsan.movieapp.data.local.dao.MovieDao
import com.ahsan.movieapp.data.local.dao.SearchHistoryDao
import com.ahsan.movieapp.data.local.entity.FavoriteEntity
import com.ahsan.movieapp.data.local.entity.MovieEntity
import com.ahsan.movieapp.data.local.entity.SearchHistoryEntity
import com.ahsan.movieapp.data.mapper.combineToMovieDetails
import com.ahsan.movieapp.data.mapper.toDomain
import com.ahsan.movieapp.data.mapper.toEntity
import com.ahsan.movieapp.data.mapper.toMovie
import com.ahsan.movieapp.data.mapper.toMovieEntity
import com.ahsan.movieapp.data.mapper.toPersonDto
import com.ahsan.movieapp.data.mapper.toPerson
import com.ahsan.movieapp.data.paging.CategoryRemoteMediator
import com.ahsan.movieapp.data.paging.DiscoverPagingSource
import com.ahsan.movieapp.data.paging.SearchMoviesPagingSource
import com.ahsan.movieapp.data.paging.TvGenrePagingSource
import com.ahsan.movieapp.data.remote.TmdbApi
import com.ahsan.movieapp.data.remote.dto.MovieDto
import com.ahsan.movieapp.data.remote.dto.PagedResponseDto
import com.ahsan.movieapp.domain.model.CastMember
import com.ahsan.movieapp.domain.model.DiscoverFilters
import com.ahsan.movieapp.domain.model.GenreChip
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.domain.model.MovieCategory
import com.ahsan.movieapp.domain.model.MovieCollection
import com.ahsan.movieapp.domain.model.MovieCredits
import com.ahsan.movieapp.domain.model.MovieDetails
import com.ahsan.movieapp.domain.model.Person
import com.ahsan.movieapp.domain.model.PersonCredits
import com.ahsan.movieapp.data.mapper.bestYoutubeTrailerKey
import com.ahsan.movieapp.domain.model.PersonDetails
import com.ahsan.movieapp.domain.model.SeasonDetails
import com.ahsan.movieapp.domain.model.TvShowDetails
import com.ahsan.movieapp.domain.model.WatchProviders
import com.ahsan.movieapp.util.Constants
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
    private val database: AppDatabase,
    private val movieDao: MovieDao,
    private val favoriteDao: FavoriteDao,
    private val searchHistoryDao: SearchHistoryDao
) : MovieRepository {

    override fun getCategory(category: MovieCategory): Flow<Resource<List<Movie>>> =
        cachedCategoryFlow(
            storageKey = category.storageKey,
            fetch = { fetchCategoryFromNetwork(category) }
        )

    /**
     * Phase 4 (pagination) Round 1 — only [MovieCategory.TRENDING_TODAY] calls this so far (see
     * [TrendingViewModel][com.ahsan.movieapp.ui.trending.TrendingViewModel]). Forwards to
     * [pagedCategoryFlow], the shared plumbing every paginated category/genre listing uses.
     */
    override fun getPagedCategory(category: MovieCategory): Flow<PagingData<Movie>> =
        pagedCategoryFlow(
            storageKey = category.storageKey,
            fetchPage = { page -> fetchCategoryFromNetworkPaged(category, page) }
        )

    /**
     * Phase 4 (pagination) Round 2 — the genre screen's Movies tab (see
     * [GenreViewModel][com.ahsan.movieapp.ui.genre.GenreViewModel]). [browseGenre] below is this
     * same TMDB `with_genres` discover call's single-page counterpart, kept for anything that
     * still wants a plain one-shot list.
     */
    override fun getPagedGenre(genreId: Int): Flow<PagingData<Movie>> =
        pagedCategoryFlow(
            storageKey = "genre_$genreId",
            fetchPage = { page -> api.discoverByGenres(genreId.toString(), page = page) }
        )

    /**
     * Phase 4 (pagination) Round 3 — the genre screen's TV tab. No Room table for TV data, so
     * unlike [getPagedGenre] this [Pager] has no [androidx.paging.RemoteMediator]: just
     * [TvGenrePagingSource] reading TMDB pages directly, one TMDB page per Paging 3 page.
     */
    override fun getPagedGenreTv(genreId: Int): Flow<PagingData<Movie>> =
        Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, prefetchDistance = PAGE_SIZE / 2, enablePlaceholders = false),
            pagingSourceFactory = { TvGenrePagingSource(api, genreId) }
        ).flow

    /**
     * Phase 4 (pagination) Round 4 — the search screen's movie results. Same no-`RemoteMediator`
     * shape as [getPagedGenreTv]: [SearchMoviesPagingSource] reads TMDB pages (with a page-1-only
     * local fallback) directly, since there's no Room cache table keyed by arbitrary search text.
     */
    override fun getPagedSearchMovies(query: String): Flow<PagingData<Movie>> =
        Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, prefetchDistance = PAGE_SIZE / 2, enablePlaceholders = false),
            pagingSourceFactory = { SearchMoviesPagingSource(api, movieDao, favoriteDao, query) }
        ).flow

    /**
     * The people half of a search query — one-shot, capped at [MAX_PEOPLE_RESULTS], not part of
     * [getPagedSearchMovies]'s pager. See the interface doc for why.
     */
    override suspend fun searchPeople(query: String): Result<List<Person>> = runCatching {
        api.searchMulti(query).results
            .filter { it.mediaType == "person" }
            .take(MAX_PEOPLE_RESULTS)
            .map { it.toPersonDto().toDomain() }
    }

    /**
     * Phase 4 (pagination) Round 4 — the search screen's filter-panel Discover results. Same
     * no-`RemoteMediator` shape as [getPagedSearchMovies]/[getPagedGenreTv]: [DiscoverPagingSource]
     * reads TMDB pages directly, since filter combinations aren't cached by key.
     */
    override fun getPagedDiscoverMovies(filters: DiscoverFilters): Flow<PagingData<Movie>> =
        Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, prefetchDistance = PAGE_SIZE / 2, enablePlaceholders = false),
            pagingSourceFactory = { DiscoverPagingSource(api, movieDao, favoriteDao, filters) }
        ).flow

    /**
     * Shared plumbing for every paginated "just a list of movies under some cache key" screen —
     * the [Pager] equivalent of [cachedCategoryFlow] below. [CategoryRemoteMediator] handles the
     * offline-first staleness check and TMDB paging; [MovieDao.pagingSourceForCategory] is what
     * [Pager] actually reads from and re-emits on every Room change (including favorite toggles,
     * since that query's `LEFT JOIN favorites` is part of what Room tracks for invalidation).
     */
    @OptIn(ExperimentalPagingApi::class)
    private fun pagedCategoryFlow(
        storageKey: String,
        fetchPage: suspend (page: Int) -> PagedResponseDto<MovieDto>
    ): Flow<PagingData<Movie>> =
        Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, prefetchDistance = PAGE_SIZE / 2, enablePlaceholders = false),
            remoteMediator = CategoryRemoteMediator(
                category = storageKey,
                staleThresholdMs = STALE_THRESHOLD_MS,
                fetchPage = fetchPage,
                database = database,
                movieDao = movieDao
            ),
            pagingSourceFactory = { movieDao.pagingSourceForCategory(storageKey) }
        ).flow.map { pagingData -> pagingData.map { row -> row.toDomain() } }

    override fun getForYou(): Flow<Resource<List<Movie>>> =
        cachedCategoryFlow(
            storageKey = MovieCategory.FOR_YOU.storageKey,
            fetch = {
                val genreIds = favoriteDao.getFavoriteGenreIdsRaw()
                    .flatMap { raw -> raw.split(",").mapNotNull { it.trim().toIntOrNull() } }
                    .distinct()
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

    /**
     * Cast + director for the Detail screen's cast/crew section (Phase 3 Round A). A single
     * `/movie/{id}/credits` call already returns both cast and crew — this just reads the crew
     * half (previously ignored, see [getCast]) to find the director, rather than firing a second
     * network request. Network-only, no Room cache — see the interface doc for why.
     */
    override suspend fun getMovieCredits(movieId: Int): Result<MovieCredits> = runCatching {
        val dto = api.getMovieCredits(movieId)
        val cast = dto.cast.sortedBy { it.order }.map { it.toDomain() }
        val director = dto.crew.firstOrNull { it.job == "Director" }?.toPerson()
        MovieCredits(cast = cast, director = director)
    }

    override fun getSimilarMovies(movieId: Int): Flow<Resource<List<Movie>>> =
        cachedCategoryFlow(
            storageKey = "similar_$movieId",
            fetch = { api.getSimilarMovies(movieId).results }
        )

    override fun getRecommendedMovies(movieId: Int): Flow<Resource<List<Movie>>> =
        cachedCategoryFlow(
            storageKey = "recommendations_$movieId",
            fetch = { api.getMovieRecommendations(movieId).results }
        )

    /**
     * Backs the Detail screen's collection teaser (Phase 3 Round B). One-shot, network-only — same
     * convention as [getMovieCredits] for newer data with no offline table of its own yet. Every
     * movie in the collection still gets upserted into the shared `movies` table (like
     * [getPagedDiscoverMovies] does), so each one is just as available offline afterward as a movie
     * found any other way, even though the collection listing itself isn't cached by collection id.
     */
    override suspend fun getCollectionDetails(collectionId: Int): Result<MovieCollection> = runCatching {
        val dto = api.getCollectionDetails(collectionId)
        val favoriteIds = favoriteDao.observeFavoriteMovies().first().map { it.id }.toSet()
        val now = System.currentTimeMillis()
        val parts = dto.parts.orEmpty()
        if (parts.isNotEmpty()) {
            movieDao.upsertMovies(parts.map { it.toEntity(now) })
        }
        dto.toDomain(favoriteIds)
    }

    /**
     * Backs the Detail screen's streaming-availability section (Phase 3 Round C). One-shot,
     * network-only — same convention as [getMovieCredits]/[getCollectionDetails] for newer data
     * with no offline table of its own. TMDB's response already covers every region in one call,
     * so there's nothing region-specific to fetch again when the user switches the dropdown.
     */
    override suspend fun getWatchProviders(movieId: Int): Result<WatchProviders> = runCatching {
        api.getWatchProviders(movieId).toDomain()
    }

    override suspend fun getPersonDetails(personId: Int): Result<PersonDetails> = runCatching {
        api.getPersonDetails(personId).toDomain()
    }

    override suspend fun getPersonCredits(personId: Int): Result<PersonCredits> = runCatching {
        val favoriteIds = favoriteDao.observeFavoriteMovies().first().map { it.id }.toSet()
        api.getPersonCombinedCredits(personId).toDomain(favoriteIds)
    }

    override suspend fun getGenreChips(): Result<List<GenreChip>> = runCatching {
        // Reuses whatever's already cached from Explore's Popular carousel — zero extra image
        // network calls, and every image is guaranteed to be a real, valid TMDB poster.
        val popularMovies = movieDao.observeCategory(MovieCategory.POPULAR.storageKey).first()
        // Popular is dominated by a handful of multi-genre blockbusters (a single tentpole is
        // routinely tagged Action + Adventure + Science Fiction all at once), so always taking
        // the *first* match per genre made those genres' chips all pick the same movie, and so
        // the same poster. Track which movies have already been used as a representative image
        // and prefer an unused one, only repeating a movie if every match for that genre is
        // already spoken for (better than an empty chip).
        val usedMovieIds = mutableSetOf<Int>()
        CURATED_GENRES.map { curated ->
            val candidates = curated.movieGenreId?.let { movieGenreId ->
                popularMovies.filter { movieGenreId in it.genreIds }
            }.orEmpty()
            val representative = candidates.firstOrNull { it.id !in usedMovieIds } ?: candidates.firstOrNull()
            representative?.let { usedMovieIds += it.id }
            GenreChip(
                movieGenreId = curated.movieGenreId,
                tvGenreId = curated.tvGenreId,
                name = curated.name,
                imageUrl = representative?.posterPath?.let { Constants.posterUrl(it, Constants.POSTER_WIDTH_SMALL) }
            )
        }
    }

    override fun browseGenre(genreId: Int): Flow<Resource<List<Movie>>> =
        cachedCategoryFlow(
            storageKey = "genre_$genreId",
            fetch = { api.discoverByGenres(genreId.toString()).results }
        )

    override suspend fun getPopularTv(): Result<List<Movie>> = runCatching {
        api.getPopularTv().results.map { it.toMovie() }
    }

    override fun observeRecentSearches(limit: Int): Flow<List<String>> =
        searchHistoryDao.observeRecent(limit).map { entries -> entries.map { it.query } }

    override suspend fun recordSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        searchHistoryDao.recordSearch(SearchHistoryEntity(query = trimmed, searchedAt = System.currentTimeMillis()))
    }

    override suspend fun clearSearchHistory() {
        searchHistoryDao.clearAll()
    }

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

    /**
     * Phase 2.6 Session 1 — the TV detail screen's base info section. One-shot, network-only, no
     * Room cache — same convention as [getMovieCredits]/[getCollectionDetails]/[getWatchProviders].
     */
    override suspend fun getTvDetails(tvId: Int): Result<TvShowDetails> = runCatching {
        api.getTvDetails(tvId).toDomain()
    }

    /**
     * TV counterpart of [getMovieCredits] — same "read the crew list already returned by the
     * credits call to find the director" approach, no second network request.
     */
    override suspend fun getTvCredits(tvId: Int): Result<MovieCredits> = runCatching {
        val dto = api.getTvCredits(tvId)
        val cast = dto.cast.sortedBy { it.order }.map { it.toDomain() }
        val director = dto.crew.firstOrNull { it.job == "Director" }?.toPerson()
        MovieCredits(cast = cast, director = director)
    }

    /** TV detail screen's Similar section, added on Ahsan's post-build feedback. Network-only, no
     *  Room cache — see the interface doc. */
    override suspend fun getSimilarTvShows(tvId: Int): Result<List<Movie>> = runCatching {
        api.getSimilarTv(tvId).results.map { it.toMovie() }
    }

    /** TV detail screen's Recommendations section — see the interface doc. */
    override suspend fun getRecommendedTvShows(tvId: Int): Result<List<Movie>> = runCatching {
        api.getRecommendedTv(tvId).results.map { it.toMovie() }
    }

    /** Movie detail screen's Watch Trailer button (Phase 2.6 Session 2) — see the interface doc. */
    override suspend fun getMovieTrailerKey(movieId: Int): Result<String?> = runCatching {
        api.getMovieVideos(movieId).bestYoutubeTrailerKey()
    }

    /** TV detail screen's Watch Trailer button — see the interface doc. */
    override suspend fun getTvTrailerKey(tvId: Int): Result<String?> = runCatching {
        api.getTvVideos(tvId).bestYoutubeTrailerKey()
    }

    /** TV detail screen's Seasons section drill-down — see the interface doc. */
    override suspend fun getSeasonDetails(tvId: Int, seasonNumber: Int): Result<SeasonDetails> = runCatching {
        api.getSeasonDetails(tvId, seasonNumber).toDomain()
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

    /** [fetchCategoryFromNetwork]'s paged counterpart for [getPagedCategory] — same per-category
     * endpoint mapping, but keeps the full [PagedResponseDto] (page/totalPages) that
     * [CategoryRemoteMediator] needs instead of just the results list. */
    private suspend fun fetchCategoryFromNetworkPaged(category: MovieCategory, page: Int): PagedResponseDto<MovieDto> =
        when (category) {
            MovieCategory.TRENDING_TODAY -> api.getTrendingToday(page)
            MovieCategory.POPULAR -> api.getPopular(page)
            MovieCategory.TOP_RATED -> api.getTopRated(page)
            MovieCategory.NOW_PLAYING -> api.getNowPlaying(page)
            MovieCategory.UPCOMING -> api.getUpcoming(page)
            MovieCategory.FOR_YOU -> api.getPopular(page)
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

    /** One curated genre chip's stable TMDB ids — hardcoded rather than name-matched off the live
     * genre lists, since movie and TV genre *names* don't line up 1:1 (TV's "Action & Adventure"
     * covers both this app's Action and Adventure chips, TV's "Sci-Fi & Fantasy" covers both
     * Science Fiction and Fantasy) while the numeric ids are stable and well documented. */
    private data class CuratedGenre(val name: String, val movieGenreId: Int?, val tvGenreId: Int?)

    companion object {
        private const val STALE_THRESHOLD_MS = 2 * 60 * 60 * 1000L // 2 hours
        private const val MAX_PEOPLE_RESULTS = 10
        // Matches TMDB's own fixed page size, so one Paging 3 "page" load is exactly one TMDB
        // request — no partial-page bookkeeping needed.
        private const val PAGE_SIZE = 20

        // Curated first-open genre chips. Most genres share the same id between movie and TV;
        // a few don't exist for one media type at all (Horror/Romance/Thriller have no TV
        // counterpart) — those get a null id and the genre screen hides the tab that doesn't
        // apply. "Spy" isn't a real TMDB genre, so it's not included. "Kids" (TV-only) was removed
        // on request — it doesn't have a movie counterpart, which made it awkward on a
        // movie-first genre list.
        private val CURATED_GENRES = listOf(
            CuratedGenre("Action", movieGenreId = 28, tvGenreId = 10759),
            CuratedGenre("Adventure", movieGenreId = 12, tvGenreId = 10759),
            CuratedGenre("Animation", movieGenreId = 16, tvGenreId = 16),
            CuratedGenre("Comedy", movieGenreId = 35, tvGenreId = 35),
            CuratedGenre("Crime", movieGenreId = 80, tvGenreId = 80),
            CuratedGenre("Drama", movieGenreId = 18, tvGenreId = 18),
            CuratedGenre("Family", movieGenreId = 10751, tvGenreId = 10751),
            CuratedGenre("Fantasy", movieGenreId = 14, tvGenreId = 10765),
            CuratedGenre("Horror", movieGenreId = 27, tvGenreId = null),
            CuratedGenre("Romance", movieGenreId = 10749, tvGenreId = null),
            CuratedGenre("Science Fiction", movieGenreId = 878, tvGenreId = 10765),
            CuratedGenre("Thriller", movieGenreId = 53, tvGenreId = null)
        )
    }
}
