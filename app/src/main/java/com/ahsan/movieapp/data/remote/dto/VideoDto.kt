package com.ahsan.movieapp.data.remote.dto

/**
 * `/movie/{id}/videos` and `/tv/{id}/videos` — Phase 2.6 Session 2's Watch Trailer feature on both
 * detail screens. TMDB returns every kind of attached video (trailers, teasers, clips,
 * featurettes, behind-the-scenes) across multiple sites (YouTube, Vimeo); see
 * [com.ahsan.movieapp.data.mapper.bestYoutubeTrailerKey] for how the one actually shown is picked
 * out of [results].
 */
data class VideosResponseDto(
    val id: Int,
    val results: List<VideoDto>?
)

data class VideoDto(
    val id: String,
    val key: String,
    val name: String,
    val site: String?,
    val type: String?,
    val official: Boolean?
)
