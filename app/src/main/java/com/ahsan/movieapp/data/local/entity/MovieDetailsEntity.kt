package com.ahsan.movieapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The extra fields TMDB's /movie/{id} endpoint returns beyond what's already in [MovieEntity]
 * (tagline, runtime, genre names). Kept separate so list screens don't pay for detail data.
 */
@Entity(tableName = "movie_details")
data class MovieDetailsEntity(
    @PrimaryKey val movieId: Int,
    val tagline: String?,
    val runtimeMinutes: Int?,
    val genreNames: List<String>,
    val cachedAt: Long
)

@Entity(tableName = "cast_members", primaryKeys = ["movieId", "personId"])
data class CastMemberEntity(
    val movieId: Int,
    val personId: Int,
    val name: String,
    val character: String,
    val profilePath: String?,
    val order: Int
)
