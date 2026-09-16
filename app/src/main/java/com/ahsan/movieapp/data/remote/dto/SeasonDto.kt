package com.ahsan.movieapp.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * One entry from `/tv/{id}`'s `seasons` array (Phase 2.6 Session 2's Seasons section) — comes free
 * on the same call as the rest of [TvDetailsDto], enough to render the season list without a
 * separate fetch per season. Tapping a season opens [SeasonDetailsDto] — a separate
 * `/tv/{id}/season/{season_number}` call — for that season's full episode list.
 */
data class SeasonDto(
    @SerializedName("season_number") val seasonNumber: Int,
    val name: String,
    val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("episode_count") val episodeCount: Int?,
    @SerializedName("air_date") val airDate: String?
)

/** `/tv/{id}/season/{season_number}` response — one season's full episode list. */
data class SeasonDetailsDto(
    val id: Int,
    @SerializedName("season_number") val seasonNumber: Int,
    val name: String,
    val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    val episodes: List<EpisodeDto>?
)

data class EpisodeDto(
    @SerializedName("episode_number") val episodeNumber: Int,
    val name: String,
    val overview: String?,
    @SerializedName("still_path") val stillPath: String?,
    @SerializedName("air_date") val airDate: String?,
    @SerializedName("vote_average") val voteAverage: Double?
)
