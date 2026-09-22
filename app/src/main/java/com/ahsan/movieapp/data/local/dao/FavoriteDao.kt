package com.ahsan.movieapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ahsan.movieapp.data.local.entity.FavoriteEntity
import com.ahsan.movieapp.data.local.entity.MovieEntity
import com.ahsan.movieapp.data.local.entity.TvShowEntity
import kotlinx.coroutines.flow.Flow

/**
 * Session 6 — every query is now media-type aware: the Favorites table's composite `(id, mediaType)`
 * primary key means a TV show (id in the same numeric range as movies) can never be confused with a
 * same-numbered movie. Movie rows join `movies`, TV rows join `tv_shows`, each against the matching
 * `favorites.mediaType`.
 */
@Dao
interface FavoriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE id = :id AND mediaType = :mediaType")
    suspend fun removeFavorite(id: Int, mediaType: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :id AND mediaType = :mediaType)")
    fun isFavorite(id: Int, mediaType: String): Flow<Boolean>

    @Query(
        """
        SELECT movies.* FROM movies
        INNER JOIN favorites ON movies.id = favorites.id AND favorites.mediaType = 'movie'
        ORDER BY favorites.addedAt DESC
        """
    )
    fun observeFavoriteMovies(): Flow<List<MovieEntity>>

    /** Session 6 — the TV half of the Favorites screen, over the `tv_shows` cache table. */
    @Query(
        """
        SELECT tv_shows.* FROM tv_shows
        INNER JOIN favorites ON tv_shows.id = favorites.id AND favorites.mediaType = 'tv'
        ORDER BY favorites.addedAt DESC
        """
    )
    fun observeFavoriteTvShows(): Flow<List<TvShowEntity>>

    // Returns the raw, comma-joined genreIds column (not List<Int>) on purpose: Room's KSP
    // processor can't resolve a TypeConverter chain when the per-row converted type is itself a
    // generic collection wrapped in an outer List (i.e. List<List<Int>>). Parsing happens in the
    // repository instead. Movie favorites only — "For You" personalization is a movie concept.
    @Query("SELECT genreIds FROM movies INNER JOIN favorites ON movies.id = favorites.id AND favorites.mediaType = 'movie'")
    suspend fun getFavoriteGenreIdsRaw(): List<String>
}