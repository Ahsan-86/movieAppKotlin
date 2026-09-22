package com.ahsan.movieapp.data.repository

import androidx.paging.PagingData
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
import com.ahsan.movieapp.domain.model.PersonDetails
import com.ahsan.movieapp.domain.model.SeasonDetails
import com.ahsan.movieapp.domain.model.TvCategory
import com.ahsan.movieapp.domain.model.TvShowDetails
import com.ahsan.movieapp.domain.model.WatchProviders
import com.ahsan.movieapp.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface MovieRepository {

    /** Offline-first: emits cached movies immediately, refreshes from TMDB, re-emits on change. */
    fun getCategory(category: MovieCategory): Flow<Resource<List<Movie>>>

    /**
     * Phase 4 (pagination) — the same category, but as an infinite-scroll [PagingData] stream
     * backed by Room + a [androidx.paging.RemoteMediator] instead of [getCategory]'s single
     * cached snapshot. Currently only wired up for [MovieCategory.TRENDING_TODAY] (Round 1);
     * every other category still uses [getCategory]'s single-page behavior until its own round.
     * See [MovieRepositoryImpl.getPagedCategory].
     */
    fun getPagedCategory(category: MovieCategory): Flow<PagingData<Movie>>

    /**
     * Phase 4 (pagination) Round 2 — [browseGenre]'s infinite-scroll counterpart, backing the
     * genre screen's Movies tab. Same [androidx.paging.RemoteMediator]-over-Room mechanism as
     * [getPagedCategory], just keyed by `"genre_$genreId"` instead of a fixed [MovieCategory].
     * The TV tab's counterpart is [getPagedGenreTv], which pages differently — network-only, since
     * a genre+filter TV search has no cache table of its own (only the curated TV carousels do).
     */
    fun getPagedGenre(genreId: Int): Flow<PagingData<Movie>>

/**
     * Phase 4 (pagination) Round 3 — the genre screen's TV tab. No Room table keyed by genre+filter
     * for TV (same convention as [getPagedGenre]'s filter-only caveats on the movie side), so
     * unlike [getPagedGenre] this [Pager] has no [androidx.paging.RemoteMediator]: just
     * [com.ahsan.movieapp.data.paging.TvGenrePagingSource] reading TMDB pages directly, one TMDB
     * page per Paging 3 page. The genre screen's filtered state passes filters through to that
     * source (see its class doc); [totalResults] is forwarded so the screen can display a
     * filtered-results count.
     */
    fun getPagedGenreTv(
        genreId: Int,
        filters: DiscoverFilters = DiscoverFilters(),
        totalResults: MutableStateFlow<Int?>? = null
    ): Flow<PagingData<Movie>>

    /** "For You" — discovers movies from the genres of the person's current favorites. */
    fun getForYou(): Flow<Resource<List<Movie>>>

    fun getMovieDetails(movieId: Int): Flow<Resource<MovieDetails>>

    fun getCast(movieId: Int): Flow<Resource<List<CastMember>>>

    /**
     * Cast + director for the Detail screen's cast/crew section (Phase 3 Round A) and its "view
     * all" screen — one-shot, network-only (no Room cache; same convention as [getTvDetails]
     * and [getCollectionDetails] for data that has no offline table of its own). Cast +
     * director only, no other crew roles, per the round's confirmed scope.
     */
    suspend fun getMovieCredits(movieId: Int): Result<MovieCredits>

    fun getSimilarMovies(movieId: Int): Flow<Resource<List<Movie>>>

    /**
     * TMDB's own "Recommendations" algorithm — a separate endpoint/model from [getSimilarMovies],
     * deliberately not deduped against it. Offline-first via the same cachedCategoryFlow-style
     * caching as everything else in [getCategory]'s family, keyed per movieId.
     */
    fun getRecommendedMovies(movieId: Int): Flow<Resource<List<Movie>>>

    /**
     * The full "franchise" list opened from the Detail screen's collection teaser (Phase 3 Round
     * B) — every movie in the collection plus its own overview/poster/backdrop. One-shot,
     * network-only (no Room cache — same convention as [getMovieCredits] and [getPersonCredits]
     * for newer data that doesn't have its own offline table yet).
     */
    suspend fun getCollectionDetails(collectionId: Int): Result<MovieCollection>

    /**
     * Streaming availability for the Detail screen's Round C section — every region TMDB has
     * JustWatch data for in one call. One-shot, network-only (no Room cache — same convention as
     * [getMovieCredits] and [getCollectionDetails]; awkward to cache anyway since results vary by
     * the region the user has picked).
     */
    suspend fun getWatchProviders(movieId: Int): Result<WatchProviders>

    /**
     * Phase 4 (pagination) Round 4 — the search screen's movie results, infinite-scroll over TMDB
     * `/search/multi` filtered to movies. Network-only, no [androidx.paging.RemoteMediator] (same
     * reasoning as [getPagedGenreTv] — arbitrary search queries have no Room cache table of their
     * own), but still falls back to a capped local title/overview match on page 1 if the network
     * call fails outright, so search never goes fully blank just because the device is offline. See
     * [com.ahsan.movieapp.data.paging.SearchMoviesPagingSource].
     */
    fun getPagedSearchMovies(query: String): Flow<PagingData<Movie>>

    /**
     * The people half of a `/search/multi` query — one-shot, capped, and NOT paginated (unlike
     * [getPagedSearchMovies]): only the first handful of people a query returns are ever shown, so
     * there's nothing worth infinite-scrolling here, same reasoning that keeps Cast & Crew and
     * Similar/Recommendations out of Phase 4's scope entirely. TV results are surfaced separately
     * by [searchTvShows], not dropped.
     */
    suspend fun searchPeople(query: String): Result<List<Person>>

    /**
     * The TV half of a `/search/multi` query, rendered as the search screen's "TV Shows" row
     * (grouped separately from the movie grid the way people already are). Same convention as
     * [searchPeople]: one-shot, capped, NOT paginated — and network-only with no Room upsert
     * (arbitrary search queries have no cache table of their own, and a TV id in the movie-only
     * `movies` table would collide with a movie that happens to share the same numeric id — the
     * dedicated `tv_shows` cache is keyed by curated carousel, not searches, per Session 3).
     * Results come back as [Movie] via the same [com.ahsan.movieapp.data.mapper.toMovie] bridge
     * every other TV listing uses, so they render through the shared poster-card components with
     * `isFavorite = false`.
     */
    suspend fun searchTvShows(query: String): Result<List<Movie>>

    /** Bio, photo, and primary role for the person screen's header. */
    suspend fun getPersonDetails(personId: Int): Result<PersonDetails>

    /**
     * A person's movie + TV credits, pre-split by media type and acting-vs-directing. One network
     * call (`combined_credits`); the person screen's toggles just filter this in memory afterward.
     */
    suspend fun getPersonCredits(personId: Int): Result<PersonCredits>

    /** Most recent distinct search queries, newest first — backs the "recent searches" chips. */
    fun observeRecentSearches(limit: Int = 10): Flow<List<String>>

    suspend fun recordSearch(query: String)

    suspend fun clearSearchHistory()

    /** A curated set of genres with a real poster pulled from whatever's already cached for that genre. */
    suspend fun getGenreChips(): Result<List<GenreChip>>

    /** Offline-first browse of a single genre (TMDB `/discover/movie`) — used by the genre screen. */
    fun browseGenre(genreId: Int): Flow<Resource<List<Movie>>>

    /**
     * Phase 2.6 Session 3 — the TV counterpart of [getCategory], backing the Explore screen's TV
     * carousels (Trending TV Shows, Popular TV Shows). Offline-first the same way: emits the cached
     * page-1 listing from Room immediately, refreshes from TMDB in the background, re-emits on
     * change. Rows come back as [Movie] (via TvShowEntity.toDomain) always with `isFavorite =
     * false` — TV ids share TMDB's numeric range with movies, so they must never touch the
     * movie-only [getFavorites][observeFavorites] table until Session 6's composite-key migration.
     */
    fun getCategoryTv(category: TvCategory): Flow<Resource<List<Movie>>>

    /**
     * Phase 2.6 Session 3 — the TV counterpart of [getPagedCategory], the infinite-scroll form of
     * [getCategoryTv] backed by [com.ahsan.movieapp.data.paging.TvCategoryRemoteMediator] over the
     * new Room `tv_shows` tables. Shared plumbing, ready for a paged TV screen (Session 4's Explore
     * reorder); the Explore carousels themselves consume the non-paged [getCategoryTv].
     */
    fun getPagedCategoryTv(category: TvCategory): Flow<PagingData<Movie>>

    /**
     * Phase 4 (pagination) Round 4 — infinite-scroll counterpart of the search screen's filter
     * panel (genre/year/language/minimum rating — any subset), driven by TMDB `/discover/movie`.
     * Network-only, no offline cache by filter combination (there are too many combinations to
     * usefully cache each one) and so no [androidx.paging.RemoteMediator] — every returned movie
     * still gets upserted into the shared `movies` table like any other fetch. [totalResults]
     * (optional) receives each loaded page's total so screens can show a results count. See
     * [com.ahsan.movieapp.data.paging.DiscoverPagingSource].
     */
    fun getPagedDiscoverMovies(filters: DiscoverFilters, totalResults: MutableStateFlow<Int?>? = null): Flow<PagingData<Movie>>

    fun observeFavorites(): Flow<List<Movie>>

    fun isFavorite(movieId: Int): Flow<Boolean>

    suspend fun toggleFavorite(movie: Movie)

    /** Refreshes every list category from the network; used by the background sync worker. */
    suspend fun refreshAllCategories(): Result<Unit>

    /**
     * Phase 2.6 Session 1 — the TV detail screen's base info section. One-shot, network-only, no
     * Room cache — same convention as [getMovieCredits]/[getCollectionDetails]/[getWatchProviders]
     * for data that doesn't have its own offline table yet (the only TV data this app persists is
     * the two curated carousel lists — see [getCategoryTv]). A Favorites-aware `isFavorite` flag
     * isn't part of [TvShowDetails] yet; that needs Session 6's Favorites schema migration
     * (movie/TV id collision — see the project doc's Decisions).
     */
    suspend fun getTvDetails(tvId: Int): Result<TvShowDetails>

    /**
     * TV counterpart of [getMovieCredits] — cast + director (crew job == "Director"), same
     * one-shot, network-only, no-Room-cache convention. Reuses [MovieCredits] as the return shape
     * since TMDB's `/tv/{id}/credits` has the identical cast/crew fields as the movie side. When
     * that crew list has no Director (common for series), the impl falls back to
     * `/tv/{id}/aggregate_credits` to find one.
     */
    suspend fun getTvCredits(tvId: Int): Result<MovieCredits>

    /**
     * TV counterpart of [getSimilarMovies] — the TV detail screen's Similar section, added on
     * Ahsan's post-build feedback to Phase 2.6 Session 1. One-shot, network-only, no Room cache —
     * same convention as [getTvDetails]/[getTvCredits] (this TV-per-id kind of data has no offline
     * table of its own; only the curated carousel lists are cached — see [getCategoryTv]).
     * Results come back as [Movie] (via [com.ahsan.movieapp.data.mapper.toMovie]) — the same
     * bridge every other TV listing in this app uses so TV rows can render through the shared
     * poster-card components.
     */
    suspend fun getSimilarTvShows(tvId: Int): Result<List<Movie>>

    /**
     * TV counterpart of [getRecommendedMovies] — a separate TMDB algorithm from
     * [getSimilarTvShows], deliberately not deduped against it, same as the movie side. One-shot,
     * network-only, no Room cache.
     */
    suspend fun getRecommendedTvShows(tvId: Int): Result<List<Movie>>

    /**
     * Phase 2.6 Session 2 — the Watch Trailer button on the Movie detail screen. One-shot,
     * network-only (no Room cache — same convention as [getMovieCredits]). Null means TMDB has no
     * YouTube video attached for this movie, in which case the button doesn't render.
     */
    suspend fun getMovieTrailerKey(movieId: Int): Result<String?>

    /** TV counterpart of [getMovieTrailerKey] — backs the TV detail screen's Watch Trailer button. */
    suspend fun getTvTrailerKey(tvId: Int): Result<String?>

    /**
     * Phase 2.6 Session 2 — a season's full episode list, opened from the TV detail screen's
     * Seasons section. One-shot, network-only, no Room cache — same convention as [getTvDetails].
     */
    suspend fun getSeasonDetails(tvId: Int, seasonNumber: Int): Result<SeasonDetails>
}
