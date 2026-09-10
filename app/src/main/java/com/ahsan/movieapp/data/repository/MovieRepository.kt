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
import com.ahsan.movieapp.domain.model.WatchProviders
import com.ahsan.movieapp.util.Resource
import kotlinx.coroutines.flow.Flow

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
     * The TV tab's counterpart is [getPagedGenreTv], which pages differently since this app has
     * no offline table for TV data.
     */
    fun getPagedGenre(genreId: Int): Flow<PagingData<Movie>>

    /**
     * Phase 4 (pagination) Round 3 — TV equivalent of [getPagedGenre], backing the genre screen's
     * TV tab. This app doesn't persist TV data (same one-screen exception as [getPopularTv]), so
     * there's no Room table for a [androidx.paging.RemoteMediator] to page into: this is a plain
     * network-only [androidx.paging.PagingSource] (see
     * [com.ahsan.movieapp.data.paging.TvGenrePagingSource]) instead of [getPagedGenre]'s
     * Room-backed mechanism.
     */
    fun getPagedGenreTv(genreId: Int): Flow<PagingData<Movie>>

    /** "For You" — discovers movies from the genres of the person's current favorites. */
    fun getForYou(): Flow<Resource<List<Movie>>>

    fun getMovieDetails(movieId: Int): Flow<Resource<MovieDetails>>

    fun getCast(movieId: Int): Flow<Resource<List<CastMember>>>

    /**
     * Cast + director for the Detail screen's cast/crew section (Phase 3 Round A) and its "view
     * all" screen — one-shot, network-only (no Room cache; same convention as [getPersonCredits]
     * and [getPopularTv] for newer data that doesn't have its own offline table yet). Cast +
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
     * Similar/Recommendations out of Phase 4's scope entirely. TV results are dropped, same as the
     * movies half.
     */
    suspend fun searchPeople(query: String): Result<List<Person>>

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
     * TV equivalent of [getCategory]'s "Popular" — backs the Explore screen's "Popular TV Shows"
     * carousel. Network-only, no offline cache — this app doesn't persist TV data yet (same
     * one-screen-exception convention as [getPersonCredits] and [getPagedGenreTv]).
     */
    suspend fun getPopularTv(): Result<List<Movie>>

    /**
     * Phase 4 (pagination) Round 4 — infinite-scroll counterpart of the search screen's filter
     * panel (genre/year/language/minimum rating — any subset), driven by TMDB `/discover/movie`.
     * Network-only, no offline cache by filter combination (there are too many combinations to
     * usefully cache each one) and so no [androidx.paging.RemoteMediator] — every returned movie
     * still gets upserted into the shared `movies` table like any other fetch. See
     * [com.ahsan.movieapp.data.paging.DiscoverPagingSource].
     */
    fun getPagedDiscoverMovies(filters: DiscoverFilters): Flow<PagingData<Movie>>

    fun observeFavorites(): Flow<List<Movie>>

    fun isFavorite(movieId: Int): Flow<Boolean>

    suspend fun toggleFavorite(movie: Movie)

    /** Refreshes every list category from the network; used by the background sync worker. */
    suspend fun refreshAllCategories(): Result<Unit>
}
