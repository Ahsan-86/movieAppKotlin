package com.ahsan.movieapp.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * `/collection/{id}` response — the full "franchise" list opened from the Detail screen's
 * collection teaser (Phase 3 Round B). `parts` comes back in the same per-item shape as any other
 * TMDB movie list, so it's decoded straight into the existing [MovieDto] rather than a new type.
 */
data class CollectionDetailsDto(
    val id: Int,
    val name: String,
    val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    val parts: List<MovieDto>?
)
