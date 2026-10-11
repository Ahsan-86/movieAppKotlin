package com.ahsan.movieapp.domain.model

/**
 * One still/screenshot in a movie's or show's Photos gallery (Phase 5). [thumbUrl] is the mid-size
 * image for the row and grid; [fullUrl] is the large one the fullscreen viewer loads.
 * [aspectRatio] is width/height as TMDB reports it (backdrops are almost always ~16:9).
 */
data class Photo(
    val thumbUrl: String,
    val fullUrl: String,
    val aspectRatio: Float
)
