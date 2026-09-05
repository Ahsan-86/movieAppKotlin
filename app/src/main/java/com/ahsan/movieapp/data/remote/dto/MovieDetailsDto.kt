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
    val genres: List<GenreDto>?
)

data class CreditsDto(
    val id: Int,
    val cast: List<CastMemberDto>
)

data class CastMemberDto(
    val id: Int,
    val name: String,
    val character: String?,
    @SerializedName("profile_path") val profilePath: String?,
    val order: Int
)

data class PersonDto(
    val id: Int,
    val name: String,
    @SerializedName("profile_path") val profilePath: String?,
    @SerializedName("known_for_department") val knownForDepartment: String?
)

data class PersonMovieCreditsDto(
    val id: Int,
    val cast: List<MovieDto>
)
