package com.ahsan.movieapp.domain.model

import java.util.Locale

/** UI-facing movie model — screens only ever see this, never the network DTO or the Room entity. */
data class Movie(
    val id: Int,
    val title: String,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val releaseDate: String,
    val voteAverage: Double,
    val voteCount: Int,
    val genreIds: List<Int> = emptyList(),
    val isFavorite: Boolean = false
) {
    val releaseYear: String get() = releaseDate.take(4).ifBlank { "—" }
    val ratingOutOfTen: String get() = String.format(Locale.US, "%.1f", voteAverage)
}

enum class MovieCategory(val storageKey: String) {
    TRENDING_TODAY("trending_today"),
    POPULAR("popular"),
    TOP_RATED("top_rated"),
    NOW_PLAYING("now_playing"),
    UPCOMING("upcoming"),
    FOR_YOU("for_you")
}

/** Phase 2.6 Session 3 — the TV lists Room-caches, the [MovieCategory] equivalent for TV. Stored
 *  in their own `tv_shows`/`category_tv_shows` tables (never `movies`/`favorites`): TMDB reuses
 *  one numeric ID range across movies and TV, so a TV id must never collide with the movie-only
 *  `favorites` table until Session 6's composite-key migration. */
enum class TvCategory(val storageKey: String) {
    TRENDING_TV("trending_tv"),
    POPULAR_TV("popular_tv"),
    ON_THE_AIR("on_the_air_tv")
}
