package com.ahsan.movieapp.data.remote

import com.ahsan.movieapp.data.remote.dto.CreditsDto
import com.ahsan.movieapp.data.remote.dto.GenreListDto
import com.ahsan.movieapp.data.remote.dto.MovieDetailsDto
import com.ahsan.movieapp.data.remote.dto.MovieDto
import com.ahsan.movieapp.data.remote.dto.PagedResponseDto
import com.ahsan.movieapp.data.remote.dto.PersonDto
import com.ahsan.movieapp.data.remote.dto.PersonMovieCreditsDto
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

    @GET("3/discover/movie")
    suspend fun discoverByCast(
        @Query("with_cast") personId: Int,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("page") page: Int = 1
    ): PagedResponseDto<MovieDto>

    @GET("3/genre/movie/list")
    suspend fun getGenres(): GenreListDto

    @GET("3/movie/{id}")
    suspend fun getMovieDetails(@Path("id") movieId: Int): MovieDetailsDto

    @GET("3/movie/{id}/credits")
    suspend fun getMovieCredits(@Path("id") movieId: Int): CreditsDto

    @GET("3/movie/{id}/similar")
    suspend fun getSimilarMovies(@Path("id") movieId: Int, @Query("page") page: Int = 1): PagedResponseDto<MovieDto>

    @GET("3/search/movie")
    suspend fun searchMovies(@Query("query") query: String, @Query("page") page: Int = 1): PagedResponseDto<MovieDto>

    @GET("3/search/person")
    suspend fun searchPeople(@Query("query") query: String, @Query("page") page: Int = 1): PagedResponseDto<PersonDto>

    @GET("3/person/{id}/movie_credits")
    suspend fun getPersonMovieCredits(@Path("id") personId: Int): PersonMovieCreditsDto
}
