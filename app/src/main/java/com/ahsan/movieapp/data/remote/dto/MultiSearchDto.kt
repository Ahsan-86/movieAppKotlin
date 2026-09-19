package com.ahsan.movieapp.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * TMDB `/search/multi` returns movies, TV shows, and people in one heterogeneous list,
 * distinguished by [mediaType]. One flat DTO covers every field any of the three shapes uses
 * (Gson just leaves the fields that don't apply to a given item as null). The repository splits
 * the three media types apart — movies (paginated search grid), TV shows (see
 * [com.ahsan.movieapp.data.mapper.toTvShowDto]), and people (one-shot row) — so TV results are
 * no longer dropped since this app now browses TV.
 */
data class MultiSearchResultDto(
    val id: Int,
    @SerializedName("media_type") val mediaType: String?,
    val title: String?,          // movie
    val name: String?,           // tv / person
    val overview: String?,       // movie / tv
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("profile_path") val profilePath: String?,        // person
    @SerializedName("release_date") val releaseDate: String?,        // movie
    @SerializedName("first_air_date") val firstAirDate: String?,     // tv
    @SerializedName("vote_average") val voteAverage: Double?,
    @SerializedName("vote_count") val voteCount: Int?,
    @SerializedName("genre_ids") val genreIds: List<Int>?,
    @SerializedName("known_for_department") val knownForDepartment: String? // person
)
