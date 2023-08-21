package com.ahsan.kotlinpractice.models

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.ahsan.kotlinpractice.LastViewedMovie
import com.ahsan.kotlinpractice.interfaces.MovieService
import com.ahsan.kotlinpractice.interfaces.UsersService
import com.ahsan.kotlinpractice.repositories.MovieRepository
import com.ahsan.kotlinpractice.repositories.UserRepository
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

class MovieViewModel constructor(
//    private val movieRepository: MovieRepository
) : ViewModel() {
    private var movieLiveData = MutableLiveData<List<Movie>>()
//    val lastViewedMovie: LiveData<LastViewedMovie?> = movieRepository.lastViewedMovie

//    // Function to get user data by ID
//    fun getUserById(userId: Int): LiveData<Movie> {
//        return movieRepository.getUserById(userId)
//    }

//    suspend fun insertOrUpdateLastViewedMovie(movie: LastViewedMovie) {
//        movieRepository.insertOrUpdateLastViewedMovie(movie)
//    }

    fun getPopularMovies() {
        MovieService.getInstance().getPopularMovies("455dce7f5c445c65a65195667221b78e").enqueue(object  :
            Callback<MovieMetadata> {
            override fun onResponse(call: Call<MovieMetadata>, response: Response<MovieMetadata>) {
                if (response.body()!=null){
                    movieLiveData.value = response.body()!!.results
                }
                else{
                    return
                }
            }
            override fun onFailure(call: Call<MovieMetadata>, t: Throwable) {
                Log.d("TAG",t.message.toString())
            }
        })
    }
    fun observeMovieLiveData() : LiveData<List<Movie>> {
        return movieLiveData
    }

//    val errorMessage = MutableLiveData<String>()
//    val usersList = MutableLiveData<List<User>>()
//    var job: Job? = null
//
//    val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
//        onError("Exception handled: ${throwable.localizedMessage}")
//    }
//
//    val loading = MutableLiveData<Boolean>()
//
////    fun getUsers() = liveResponse {
////        userRepository.getAllUsers()
////    }
////
////    inline fun <T> liveResponse(crossinline body: suspend () -> UserRepository.ResponseResult<T>) =
////    liveData(Dispatchers.IO) {
////        emit(UserRepository.ResponseResult.Pending)
////        val result = body()
////        emit(result)
////        emit(UserRepository.ResponseResult.Complete)
////    }
//
//    fun getAllUsers() {
//        job = CoroutineScope(Dispatchers.IO + exceptionHandler).launch {
//            val response = movieRepository.getPopularMovies()
//            withContext(Dispatchers.Main) {
//                if (response.isSuccessful) {
//                    usersList.postValue(response.body())
//                    loading.value = false
//                } else {
//                    onError("Error : ${response.message()} ")
//                }
//            }
//        }
//    }
//    private fun onError(message: String) {
//        errorMessage.value = message
//        loading.value = false
//    }
//    override fun onCleared() {
//        super.onCleared()
//        job?.cancel()
//    }
}