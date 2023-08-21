package com.ahsan.kotlinpractice.interfaces

import androidx.lifecycle.LiveData
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.ahsan.kotlinpractice.LastViewedMovie
import com.ahsan.kotlinpractice.models.MovieMetadata
import com.ahsan.kotlinpractice.models.User
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*


interface MovieService {
    @GET("3/movie/popular?")
    fun getPopularMovies(@Query("api_key") api_key : String) : Call<MovieMetadata>

    @GET("3/genre/movie/list?")
    fun getMovieGenres(@Query("api_key") api_key : String) : Call<MovieMetadata>

    @androidx.room.Query("SELECT * FROM last_viewed_movie LIMIT 1")
    fun getLastViewedMovie(): LiveData<LastViewedMovie?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLastViewedMovie(movie: LastViewedMovie)

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


//    object RetrofitClient {
//        private const val BASE_URL = "https://reqres.in/api/" // Replace with your actual API URL
//        val retrofit: Retrofit by lazy {
//            Retrofit.Builder()
//                .baseUrl(BASE_URL)
//                .addConverterFactory(GsonConverterFactory.create())
//                .build()
//        }
//        val apiService: UsersService by lazy {
//            retrofit.create(UsersService::class.java)
//        }
//    }
}