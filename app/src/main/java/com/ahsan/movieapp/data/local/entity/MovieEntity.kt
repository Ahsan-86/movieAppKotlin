package com.ahsan.movieapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per movie TMDB has ever returned to us, regardless of which list(s) it appeared on.
 * This is the single offline source of truth every screen reads from.
 */
@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String,
    val voteAverage: Double,
    val voteCount: Int,
    val genreIds: List<Int>,
    val cachedAt: Long
)

/** Maps a [MovieCategory][com.ahsan.movieapp.domain.model.MovieCategory] to the ordered movie ids it contains. */
@Entity(tableName = "category_movies", primaryKeys = ["category", "movieId"])
data class CategoryMovieCrossRef(
    val category: String,
    val movieId: Int,
    val position: Int,
    val fetchedAt: Long
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val movieId: Int,
    val addedAt: Long
)
