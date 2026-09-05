package com.ahsan.movieapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MovieDto(
    val id: Int,
    val title: String?,
    val name: String?, // some endpoints (multi-search / tv) use "name" instead of "title"
    val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("vote_average") val voteAverage: Double?,
    @SerializedName("vote_count") val voteCount: Int?,
    @SerializedName("genre_ids") val genreIds: List<Int>?,
    @SerializedName("media_type") val mediaType: String?
) {
    val displayTitle: String get() = title ?: name ?: "Untitled"
}

data class PagedResponseDto<T>(
    val page: Int,
    val results: List<T>,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("total_results") val totalResults: Int
)

data class GenreDto(
    val id: Int,
    val name: String
)

data class GenreListDto(
    val genres: List<GenreDto>
)
