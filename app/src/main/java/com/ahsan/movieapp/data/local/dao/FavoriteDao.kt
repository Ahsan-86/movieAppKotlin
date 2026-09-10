package com.ahsan.movieapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ahsan.movieapp.data.local.entity.FavoriteEntity
import com.ahsan.movieapp.data.local.entity.MovieEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE movieId = :movieId")
    suspend fun removeFavorite(movieId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE movieId = :movieId)")
    fun isFavorite(movieId: Int): Flow<Boolean>

    @Query(
        """
        SELECT movies.* FROM movies
        INNER JOIN favorites ON movies.id = favorites.movieId
        ORDER BY favorites.addedAt DESC
        """
    )
    fun observeFavoriteMovies(): Flow<List<MovieEntity>>

    // Returns the raw, comma-joined genreIds column (not List<Int>) on purpose: Room's KSP
    // processor can't resolve a TypeConverter chain when the per-row converted type is itself a
    // generic collection wrapped in an outer List (i.e. List<List<Int>>). Parsing happens in the
    // repository instead.
    @Query("SELECT genreIds FROM movies INNER JOIN favorites ON movies.id = favorites.movieId")
    suspend fun getFavoriteGenreIdsRaw(): List<String>
}
