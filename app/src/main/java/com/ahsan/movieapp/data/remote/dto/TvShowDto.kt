package com.ahsan.movieapp.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * `/discover/tv` row — the TV equivalent of [MovieDto]. Kept as its own small DTO rather than
 * reusing [MultiSearchResultDto] since this is a plain single-media-type endpoint, not the
 * heterogeneous multi-search shape.
 */
data class TvShowDto(
    val id: Int,
    val name: String,
    val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("vote_average") val voteAverage: Double?,
    @SerializedName("vote_count") val voteCount: Int?,
    @SerializedName("genre_ids") val genreIds: List<Int>?
)
