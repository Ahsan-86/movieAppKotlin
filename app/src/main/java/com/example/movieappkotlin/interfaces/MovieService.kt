package com.ahsan.kotlinpractice.interfaces

import com.ahsan.kotlinpractice.models.MovieMetadata
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*


interface MovieService {
    @GET("3/movie/popular?")
    fun getPopularMovies(@Query("api_key") api_key : String) : Call<MovieMetadata>

    @GET("3/genre/movie/list?")
    fun getMovieGenres(@Query("api_key") api_key : String) : Call<MovieMetadata>

    companion object {
        var retrofitService: MovieService? = null
        private const val BASE_URL = "https://api.themoviedb.org/" // Replace with your actual API URL
        fun getInstance() : MovieService {
            if (retrofitService == null) {
                val retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                retrofitService = retrofit.create(MovieService::class.java)
            }
            return retrofitService!!
        }
    }
}