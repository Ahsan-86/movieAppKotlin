package com.ahsan.movieapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The extra fields TMDB's /movie/{id} endpoint returns beyond what's already in [MovieEntity]
 * (tagline, runtime, genre names). Kept separate so list screens don't pay for detail data.
 *
 * Phase 3 added the Information-section fields (originalTitle through productionCompaniesRaw) —
 * this bumped the Room schema version (see AppDatabase), which resets local Favorites once on
 * first run after the update, same as the Phase 2 search_history addition did.
 * [productionCompaniesRaw] is a manually delimited string (see MovieMappers' encode/decode
 * helpers) rather than a List<ProductionCompany> Room TypeConverter, since name+logo pairs don't
 * fit the existing List<String>/List<Int> converters and a one-off converter for a single field
 * wasn't worth adding to the shared Converters class.
 *
 * Phase 3 Round B added [collectionId]/[collectionName]/[collectionPosterPath] — the compact
 * teaser TMDB's `belongs_to_collection` field carries on this same call, null when the movie
 * isn't part of a franchise. This bumped the Room schema version again (see AppDatabase), same
 * destructive-migration consequence as the earlier bumps.
 */
@Entity(tableName = "movie_details")
data class MovieDetailsEntity(
    @PrimaryKey val movieId: Int,
    val tagline: String?,
    val runtimeMinutes: Int?,
    val genreNames: List<String>,
    val originalTitle: String,
    val status: String?,
    val homepage: String?,
    val budget: Long,
    val revenue: Long,
    val productionCountries: List<String>,
    val productionCompaniesRaw: String,
    val collectionId: Int?,
    val collectionName: String?,
    val collectionPosterPath: String?,
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
