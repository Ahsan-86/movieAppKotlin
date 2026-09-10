package com.ahsan.movieapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ahsan.movieapp.data.local.entity.CastMemberEntity
import com.ahsan.movieapp.data.local.entity.CategoryMovieCrossRef
import com.ahsan.movieapp.data.local.entity.MovieDetailsEntity
import com.ahsan.movieapp.data.local.entity.MovieEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMovies(movies: List<MovieEntity>)

    @Query("DELETE FROM category_movies WHERE category = :category")
    suspend fun clearCategory(category: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryRefs(refs: List<CategoryMovieCrossRef>)

    @Transaction
    suspend fun replaceCategory(category: String, movies: List<MovieEntity>, fetchedAt: Long) {
        upsertMovies(movies)
        clearCategory(category)
        insertCategoryRefs(
            movies.mapIndexed { index, movie ->
                CategoryMovieCrossRef(category = category, movieId = movie.id, position = index, fetchedAt = fetchedAt)
            }
        )
    }

    @Query(
        """
        SELECT movies.* FROM movies
        INNER JOIN category_movies ON movies.id = category_movies.movieId
        WHERE category_movies.category = :category
        ORDER BY category_movies.position ASC
        """
    )
    fun observeCategory(category: String): Flow<List<MovieEntity>>

    @Query("SELECT MIN(fetchedAt) FROM category_movies WHERE category = :category")
    suspend fun categoryFetchedAt(category: String): Long?

    @Query("SELECT * FROM movies WHERE id = :movieId")
    fun observeMovie(movieId: Int): Flow<MovieEntity?>

    @Query("SELECT * FROM movies WHERE id = :movieId")
    suspend fun getMovie(movieId: Int): MovieEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMovieDetails(details: MovieDetailsEntity)

    @Query("SELECT * FROM movie_details WHERE movieId = :movieId")
    fun observeMovieDetails(movieId: Int): Flow<MovieDetailsEntity?>

    @Query("DELETE FROM cast_members WHERE movieId = :movieId")
    suspend fun clearCast(movieId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCast(cast: List<CastMemberEntity>)

    @Transaction
    suspend fun replaceCast(movieId: Int, cast: List<CastMemberEntity>) {
        clearCast(movieId)
        insertCast(cast)
    }

    @Query("SELECT * FROM cast_members WHERE movieId = :movieId ORDER BY `order` ASC")
    fun observeCast(movieId: Int): Flow<List<CastMemberEntity>>

    @Query(
        """
        SELECT movies.* FROM movies
        INNER JOIN favorites ON movies.id = favorites.movieId
        WHERE favorites.movieId = :movieId
        LIMIT 1
        """
    )
    fun observeIsFavoriteMovie(movieId: Int): Flow<MovieEntity?>

    /**
     * Offline fallback for search: a capped match against whatever's already cached. [limit]
     * exists specifically so one query can never return an unbounded row count regardless of how
     * many movies happen to be cached — this is what caused the Phase 2 slowdown last time,
     * combined with a non-lazy results grid (now fixed on the UI side).
     */
    @Query(
        """
        SELECT * FROM movies
        WHERE title LIKE :likeQuery OR overview LIKE :likeQuery
        ORDER BY voteAverage DESC
        LIMIT :limit
        """
    )
    suspend fun searchLocalMovies(likeQuery: String, limit: Int): List<MovieEntity>
}
