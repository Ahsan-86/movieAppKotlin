package com.ahsan.movieapp.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * `/tv/{id}/aggregate_credits` — the whole-show cast & crew view rolled up across every episode,
 * which TMDB populates with per-episode directors that the plain `/tv/{id}/credits` `crew` list
 * often omits for series (see MovieRepositoryImpl.getTvCredits). Only the crew half is modeled
 * here: `getTvCredits` keeps the regular credits' cast and falls back to this response just to
 * find the show's director.
 */
data class TvAggregateCreditsDto(
    val id: Int,
    val crew: List<TvAggregateCrewMemberDto> = emptyList()
)

/** One crew member in the aggregate view — carries a `jobs` list (per-episode roles), not a single `job`. */
data class TvAggregateCrewMemberDto(
    val id: Int,
    val name: String,
    @SerializedName("profile_path") val profilePath: String?,
    val jobs: List<TvAggregateCrewJobDto> = emptyList()
)

data class TvAggregateCrewJobDto(
    val job: String?
)