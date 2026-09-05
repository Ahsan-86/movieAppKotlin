package com.ahsan.movieapp.data.repository

import com.ahsan.movieapp.domain.model.CastMember
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.domain.model.MovieCategory
import com.ahsan.movieapp.domain.model.MovieDetails
import com.ahsan.movieapp.domain.model.Person
import com.ahsan.movieapp.util.Resource
import kotlinx.coroutines.flow.Flow

interface MovieRepository {

    /** Offline-first: emits cached movies immediately, refreshes from TMDB, re-emits on change. */
    fun getCategory(category: MovieCategory): Flow<Resource<List<Movie>>>

    /** "For You" — discovers movies from the genres of the person's current favorites. */
    fun getForYou(): Flow<Resource<List<Movie>>>

    fun getMovieDetails(movieId: Int): Flow<Resource<MovieDetails>>

    fun getCast(movieId: Int): Flow<Resource<List<CastMember>>>

    fun getSimilarMovies(movieId: Int): Flow<Resource<List<Movie>>>

    suspend fun searchMovies(query: String): Result<List<Movie>>

    suspend fun searchPeople(query: String): Result<List<Person>>

    /** Movies a given person has appeared in — used for "tap a cast member" drill-down. */
    fun getPersonFilmography(personId: Int, personName: String): Flow<Resource<List<Movie>>>

    fun observeFavorites(): Flow<List<Movie>>

    fun isFavorite(movieId: Int): Flow<Boolean>

    suspend fun toggleFavorite(movie: Movie)

    /** Refreshes every list category from the network; used by the background sync worker. */
    suspend fun refreshAllCategories(): Result<Unit>
}
