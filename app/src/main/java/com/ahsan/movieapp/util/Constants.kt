package com.ahsan.movieapp.util

object Constants {
    const val TMDB_BASE_URL = "https://api.themoviedb.org/"
    // TMDB's image CDN — widths documented at https://developer.themoviedb.org/docs/image-basics
    const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"

    const val POSTER_WIDTH = "w500"
    const val POSTER_WIDTH_SMALL = "w185"
    const val BACKDROP_WIDTH = "w780"
    const val PROFILE_WIDTH = "w185"
    // Fixed-height, larger profile image for the person screen's header photo.
    const val PROFILE_WIDTH_LARGE = "h632"
    // Production company logos are small, mostly-transparent PNGs — a small fixed width is plenty.
    const val LOGO_WIDTH = "w92"

    fun posterUrl(path: String?, width: String = POSTER_WIDTH): String? =
        path?.let { "$IMAGE_BASE_URL$width$it" }

    fun backdropUrl(path: String?, width: String = BACKDROP_WIDTH): String? =
        path?.let { "$IMAGE_BASE_URL$width$it" }

    fun profileUrl(path: String?, width: String = PROFILE_WIDTH): String? =
        path?.let { "$IMAGE_BASE_URL$width$it" }

    fun logoUrl(path: String?, width: String = LOGO_WIDTH): String? =
        path?.let { "$IMAGE_BASE_URL$width$it" }

    const val SYNC_WORK_NAME = "movie_offline_sync_work"

    const val DATASTORE_NAME = "movie_app_prefs"
}
