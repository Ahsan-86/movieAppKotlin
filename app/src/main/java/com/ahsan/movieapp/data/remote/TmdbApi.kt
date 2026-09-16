package com.ahsan.movieapp.data.remote

import com.ahsan.movieapp.data.remote.dto.CollectionDetailsDto
import com.ahsan.movieapp.data.remote.dto.CombinedCreditsDto
import com.ahsan.movieapp.data.remote.dto.CreditsDto
import com.ahsan.movieapp.data.remote.dto.GenreListDto
import com.ahsan.movieapp.data.remote.dto.MovieDetailsDto
import com.ahsan.movieapp.data.remote.dto.MovieDto
import com.ahsan.movieapp.data.remote.dto.MultiSearchResultDto
import com.ahsan.movieapp.data.remote.dto.PagedResponseDto
import com.ahsan.movieapp.data.remote.dto.PersonDetailsDto
import com.ahsan.movieapp.data.remote.dto.SeasonDetailsDto
import com.ahsan.movieapp.data.remote.dto.TvDetailsDto
import com.ahsan.movieapp.data.remote.dto.TvShowDto
import com.ahsan.movieapp.data.remote.dto.VideosResponseDto
import com.ahsan.movieapp.data.remote.dto.WatchProvidersResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * TMDB v3 REST API. The API key is attached automatically for every call by
 * [com.ahsan.movieapp.di.NetworkModule]'s auth interceptor, so it's never passed here explicitly.
 * Docs: https://developer.themoviedb.org/reference/intro/getting-started
 */
interface TmdbApi {

    @GET("3/trending/movie/day")
    suspend fun getTrendingToday(@Query("page") page: Int = 1): PagedResponseDto<MovieDto>

    @GET("3/movie/popular")
    suspend fun getPopular(@Query("page") page: Int = 1): PagedResponseDto<MovieDto>

    /** Backs the Explore screen's "Popular TV Shows" carousel — network-only, same as [discoverTvByGenres]. */
    @GET("3/tv/popular")
    suspend fun getPopularTv(@Query("page") page: Int = 1): PagedResponseDto<TvShowDto>

    @GET("3/movie/top_rated")
    suspend fun getTopRated(@Query("page") page: Int = 1): PagedResponseDto<MovieDto>

    @GET("3/movie/now_playing")
    suspend fun getNowPlaying(@Query("page") page: Int = 1): PagedResponseDto<MovieDto>

    @GET("3/movie/upcoming")
    suspend fun getUpcoming(@Query("page") page: Int = 1): PagedResponseDto<MovieDto>

    @GET("3/discover/movie")
    suspend fun discoverByGenres(
        @Query("with_genres") genreIds: String,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("page") page: Int = 1
    ): PagedResponseDto<MovieDto>

    @GET("3/genre/movie/list")
    suspend fun getGenres(): GenreListDto

    /**
     * Backs the search screen's collapsible filter panel (Phase 2.5) — every param is optional so
     * Retrofit omits whatever the user hasn't picked, letting one endpoint cover any combination
     * of genre/year/language/rating instead of needing a variant per filter.
     */
    @GET("3/discover/movie")
    suspend fun discoverMovies(
        @Query("with_genres") genreIds: String? = null,
        @Query("primary_release_year") year: Int? = null,
        @Query("with_original_language") language: String? = null,
        @Query("vote_average.gte") minRating: Float? = null,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("page") page: Int = 1
    ): PagedResponseDto<MovieDto>

    /** TV counterpart of [discoverByGenres] — backs the genre screen's TV tab. */
    @GET("3/discover/tv")
    suspend fun discoverTvByGenres(
        @Query("with_genres") genreIds: String,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("page") page: Int = 1
    ): PagedResponseDto<TvShowDto>

    @GET("3/movie/{id}")
    suspend fun getMovieDetails(@Path("id") movieId: Int): MovieDetailsDto

    @GET("3/movie/{id}/credits")
    suspend fun getMovieCredits(@Path("id") movieId: Int): CreditsDto

    @GET("3/movie/{id}/similar")
    suspend fun getSimilarMovies(@Path("id") movieId: Int, @Query("page") page: Int = 1): PagedResponseDto<MovieDto>

    /**
     * A separate TMDB algorithm from [getSimilarMovies] (genre/keyword similarity vs. TMDB's own
     * recommendation model) — the Detail screen's Similar and Recommendations sections are
     * deliberately not deduped against each other, per Ahsan's request, since they're different
     * data.
     */
    @GET("3/movie/{id}/recommendations")
    suspend fun getMovieRecommendations(@Path("id") movieId: Int, @Query("page") page: Int = 1): PagedResponseDto<MovieDto>

    /**
     * Backs the Detail screen's collection teaser (Phase 3 Round B) — every movie belonging to a
     * TMDB "collection" (franchise), plus the collection's own overview/poster/backdrop. The
     * teaser itself needs no separate call (see [getMovieDetails]'s `belongs_to_collection`); this
     * is only fetched once the teaser is tapped.
     */
    @GET("3/collection/{id}")
    suspend fun getCollectionDetails(@Path("id") collectionId: Int): CollectionDetailsDto

    /**
     * Backs the Detail screen's streaming-availability section (Phase 3 Round C) — powered by
     * JustWatch. No query params: TMDB returns every region in one response (see
     * [WatchProvidersResponseDto]), so the region dropdown filters client-side instead of
     * re-fetching per region.
     */
    @GET("3/movie/{id}/watch/providers")
    suspend fun getWatchProviders(@Path("id") movieId: Int): WatchProvidersResponseDto

    /**
     * Single call covering movies, TV, and people — replaces the old separate
     * `/search/movie` + `/search/person` calls so one settled query is one network request,
     * not two in parallel. The repository filters by `media_type` and drops TV results.
     */
    @GET("3/search/multi")
    suspend fun searchMulti(@Query("query") query: String, @Query("page") page: Int = 1): PagedResponseDto<MultiSearchResultDto>

    @GET("3/person/{id}")
    suspend fun getPersonDetails(@Path("id") personId: Int): PersonDetailsDto

    /**
     * Movie + TV credits in one call, split into `cast` (acting) and `crew` (director, writer,
     * etc. — filter by `job`) with each item tagged `media_type`. Replaces the old
     * discover-by-cast approach, which could only ever surface acting credits in movies.
     */
    @GET("3/person/{id}/combined_credits")
    suspend fun getPersonCombinedCredits(@Path("id") personId: Int): CombinedCreditsDto

    /**
     * Phase 2.6 Session 1 — the TV detail screen's base info section. Scoped down from
     * [MovieDetailsDto]'s full field set to what that screen needs (see [TvDetailsDto]'s doc);
     * Seasons (Session 2) is a separate `/tv/{id}/season/{season_number}` call, not part of this
     * response's fields used here.
     */
    @GET("3/tv/{id}")
    suspend fun getTvDetails(@Path("id") tvId: Int): TvDetailsDto

    /**
     * TV counterpart of [getMovieCredits] — same cast/crew shape, reused via [CreditsDto] rather
     * than a separate TV-specific DTO. Backs the TV detail screen's Cast & Crew section.
     */
    @GET("3/tv/{id}/credits")
    suspend fun getTvCredits(@Path("id") tvId: Int): CreditsDto

    /**
     * TV counterpart of [getSimilarMovies] — backs the TV detail screen's Similar section, added
     * on Ahsan's post-build feedback to Phase 2.6 Session 1.
     */
    @GET("3/tv/{id}/similar")
    suspend fun getSimilarTv(@Path("id") tvId: Int, @Query("page") page: Int = 1): PagedResponseDto<TvShowDto>

    /**
     * TV counterpart of [getMovieRecommendations] — a separate TMDB algorithm from [getSimilarTv],
     * deliberately not deduped against it, same as the movie side.
     */
    @GET("3/tv/{id}/recommendations")
    suspend fun getRecommendedTv(@Path("id") tvId: Int, @Query("page") page: Int = 1): PagedResponseDto<TvShowDto>

    /**
     * Phase 2.6 Session 2 — backs the Watch Trailer button on the Movie detail screen. TMDB
     * returns every attached video (trailers, teasers, clips, etc.) across sites; see
     * [com.ahsan.movieapp.data.mapper.bestYoutubeTrailerKey] for how the one shown is picked.
     */
    @GET("3/movie/{id}/videos")
    suspend fun getMovieVideos(@Path("id") movieId: Int): VideosResponseDto

    /** TV counterpart of [getMovieVideos] — backs the Watch Trailer button on the TV detail screen. */
    @GET("3/tv/{id}/videos")
    suspend fun getTvVideos(@Path("id") tvId: Int): VideosResponseDto

    /**
     * Phase 2.6 Session 2 — one season's full episode list, opened by tapping a season in the TV
     * detail screen's Seasons section. Not part of `/tv/{id}` itself (that only returns the season
     * summaries — see [TvDetailsDto.seasons]).
     */
    @GET("3/tv/{id}/season/{season_number}")
    suspend fun getSeasonDetails(@Path("id") tvId: Int, @Path("season_number") seasonNumber: Int): SeasonDetailsDto
}
