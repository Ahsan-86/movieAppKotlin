package com.ahsan.movieapp.domain.model

data class MovieDetails(
    val id: Int,
    val title: String,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val releaseDate: String,
    val runtimeMinutes: Int?,
    val voteAverage: Double,
    val voteCount: Int,
    val genres: List<String>,
    val tagline: String?,
    val isFavorite: Boolean = false
) {
    val releaseYear: String get() = releaseDate.take(4).ifBlank { "—" }
    val ratingOutOfTen: String get() = String.format("%.1f", voteAverage)
    val runtimeFormatted: String? get() = runtimeMinutes?.let { "${it / 60}h ${it % 60}m" }
}

data class CastMember(
    val id: Int,
    val name: String,
    val character: String,
    val profileUrl: String?,
    val order: Int
)

/** A person surfaced from search or a cast credit, used to drill into their filmography. */
data class Person(
    val id: Int,
    val name: String,
    val profileUrl: String?,
    val knownFor: String? = null
)
