package com.ahsan.movieapp.data.repository

import com.ahsan.movieapp.domain.model.CastMember
import com.ahsan.movieapp.domain.model.DiscoverFilters
import com.ahsan.movieapp.domain.model.GenreChip
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.domain.model.MovieCategory
import com.ahsan.movieapp.domain.model.MovieCollection
import com.ahsan.movieapp.domain.model.MovieCredits
import com.ahsan.movieapp.domain.model.MovieDetails
import com.ahsan.movieapp.domain.model.PersonCredits
import com.ahsan.movieapp.domain.model.PersonDetails
import com.ahsan.movieapp.domain.model.SearchResults
import com.ahsan.movieapp.domain.model.WatchProviders
import com.ahsan.movieapp.util.Resource
import kotlinx.coroutines.flow.Flow

interface MovieRepository {

    /** Offline-first: emits cached movies immediately, refreshes from TMDB, re-emits on change. */
    fun getCategory(category: MovieCategory): Flow<Resource<List<Movie>>>

    /** "For You" — discovers movies from the genres of the person's current favorites. */
    fun getForYou(): Flow<Resource<List<Movie>>>

    fun getMovieDetails(movieId: Int): Flow<Resource<MovieDetails>>

    fun getCast(movieId: Int): Flow<Resource<List<CastMember>>>

    /**
     * Cast + director for the Detail screen's cast/crew section (Phase 3 Round A) and its "view
     * all" screen — one-shot, network-only (no Room cache; same convention as [getPersonCredits],
     * [discoverMovies], and [getPopularTv] for newer data that doesn't have its own offline table
     * yet). Cast + director only, no other crew roles, per the round's confirmed scope.
     */
    suspend fun getMovieCredits(movieId: Int): Result<MovieCredits>

    fun getSimilarMovies(movieId: Int): Flow<Resource<List<Movie>>>

    /**
     * TMDB's own "Recommendations" algorithm — a separate endpoint/model from [getSimilarMovies],
     * deliberately not deduped against it. Offline-first via the same [cachedCategoryFlow]-style
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
     * One TMDB `/search/multi` call, split into movies and people (TV results are dropped).
     * Falls back to a capped local title/overview match if the network call fails, so search
     * never goes fully blank just because the device is offline.
     */
    suspend fun search(query: String): Result<SearchResults>

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
     * TV equivalent of [browseGenre]. Network-only, no offline cache — this app doesn't persist TV
     * data yet (same one-screen-exception convention as [getPersonCredits]), so this is a plain
     * suspend fetch rather than a cached Flow<Resource<...>>.
     */
    suspend fun browseGenreTv(genreId: Int): Result<List<Movie>>

    /**
     * TV equivalent of [getCategory]'s "Popular" — backs the Explore screen's "Popular TV Shows"
     * carousel. Network-only, no offline cache, same one-screen exception as [browseGenreTv].
     */
    suspend fun getPopularTv(): Result<List<Movie>>

    /**
     * Network-only `/discover/movie` call driven by the search screen's filter panel (genre,
     * year, language, minimum rating — any subset). Single page, no offline cache by combination
     * (there are too many combinations to usefully cache each one), though every returned movie
     * still gets upserted into the shared `movies` table like any other fetch.
     */
    suspend fun discoverMovies(filters: DiscoverFilters): Result<List<Movie>>

    fun observeFavorites(): Flow<List<Movie>>

    fun isFavorite(movieId: Int): Flow<Boolean>

    suspend fun toggleFavorite(movie: Movie)

    /** Refreshes every list category from the network; used by the background sync worker. */
    suspend fun refreshAllCategories(): Result<Unit>
}
