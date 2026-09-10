package com.ahsan.movieapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MovieDetailsDto(
    val id: Int,
    val title: String,
    val overview: String?,
    val tagline: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("release_date") val releaseDate: String?,
    val runtime: Int?,
    @SerializedName("vote_average") val voteAverage: Double?,
    @SerializedName("vote_count") val voteCount: Int?,
    val genres: List<GenreDto>?,
    // Everything below backs the Detail screen's Information section (Phase 3). All come free on
    // the same /movie/{id} call already being made for the fields above — nothing extra to fetch.
    @SerializedName("original_title") val originalTitle: String?,
    val status: String?,
    val homepage: String?,
    val budget: Long?,
    val revenue: Long?,
    @SerializedName("production_countries") val productionCountries: List<ProductionCountryDto>?,
    @SerializedName("production_companies") val productionCompanies: List<ProductionCompanyDto>?,
    // Free on this same call when the movie belongs to a franchise (e.g. a trilogy) — null
    // otherwise. Backs the Detail screen's collection teaser (Phase 3 Round B).
    @SerializedName("belongs_to_collection") val belongsToCollection: CollectionSummaryDto?
)

/** The nested summary TMDB embeds in `/movie/{id}`'s `belongs_to_collection` field. */
data class CollectionSummaryDto(
    val id: Int,
    val name: String,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?
)

data class ProductionCountryDto(
    @SerializedName("iso_3166_1") val isoCode: String,
    val name: String
)

data class ProductionCompanyDto(
    val id: Int,
    val name: String,
    @SerializedName("logo_path") val logoPath: String?
)

data class CreditsDto(
    val id: Int,
    val cast: List<CastMemberDto>,
    val crew: List<CrewMemberDto> = emptyList()
)

data class CastMemberDto(
    val id: Int,
    val name: String,
    val character: String?,
    @SerializedName("profile_path") val profilePath: String?,
    val order: Int
)

/**
 * One crew entry from `/movie/{id}/credits` (director, writer, producer, etc. — filter by [job]).
 * Phase 3 Round A only surfaces the director; other crew roles are read here but not exposed
 * further up the stack yet (see MovieRepositoryImpl.getMovieCredits).
 */
data class CrewMemberDto(
    val id: Int,
    val name: String,
    val job: String?,
    @SerializedName("profile_path") val profilePath: String?
)

data class PersonDto(
    val id: Int,
    val name: String,
    @SerializedName("profile_path") val profilePath: String?,
    @SerializedName("known_for_department") val knownForDepartment: String?
)
