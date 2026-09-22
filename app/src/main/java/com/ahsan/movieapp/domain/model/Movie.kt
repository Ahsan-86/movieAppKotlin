package com.ahsan.movieapp.domain.model

import java.util.Locale

/** UI-facing movie model — screens only ever see this, never the network DTO or the Room entity.
 *  TV shows reuse this same shape everywhere (a TV id in the same numeric range as movies), so the
 *  model carries its own [MediaType] — the key that keeps movie and TV favorites apart in Session
 *  6's composite-key Favorites table. */
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
    val isFavorite: Boolean = false,
    val mediaType: MediaType = MediaType.MOVIE
) {
    val releaseYear: String get() = releaseDate.take(4).ifBlank { "—" }
    val ratingOutOfTen: String get() = String.format(Locale.US, "%.1f", voteAverage)
}

/**
 * Session 6 — whether a [Movie] (or a `favorites` row) is a real movie or a TV show. TMDB reuses
 * one numeric id range across movies and TV, so the composite favorite key is `(id, mediaType)`.
 * [stored] is the `"movie"`/`"tv"` string the Room `favorites.mediaType` column keeps, the same
 * strings the network DTOs already emit (`MultiSearchResultDto.mediaType`, genre tables).
 */
enum class MediaType(val stored: String) {
    MOVIE("movie"),
    TV("tv")
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
 *  in their own `tv_shows`/`category_tv_shows` tables (never `movies`): even though Session 6 added
 *  TV favorites via a composite `(id, mediaType)` key, the *cache* tables stay split the way they
 *  were built — a TV id must never share the movie cache row. */
enum class TvCategory(val storageKey: String) {
    TRENDING_TV("trending_tv"),
    POPULAR_TV("popular_tv"),
    ON_THE_AIR("on_the_air_tv")
}
