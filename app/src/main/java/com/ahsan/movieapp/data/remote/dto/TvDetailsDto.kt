package com.ahsan.movieapp.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * `/tv/{id}` response — the TV counterpart of [MovieDetailsDto], scoped to what Phase 2.6's TV
 * detail screen needs (info section + Cast & Crew + Information, Session 1; Seasons, Session 2).
 * Deliberately excludes fields with no TV equivalent (budget/revenue/belongs_to_collection).
 * [productionCountries]/[productionCompanies]/[networks] back the Information section added on
 * Ahsan's post-build feedback, same fields as [MovieDetailsDto]'s Information section plus TV's
 * own `networks` field (reuses [ProductionCompanyDto]'s shape — TMDB's networks have the identical
 * id/name/logo_path fields). [seasons] backs Session 2's Seasons section — each season's full
 * episode list is a separate `/tv/{id}/season/{season_number}` call ([SeasonDetailsDto]), not part
 * of this response.
 */
data class TvDetailsDto(
    val id: Int,
    val name: String,
    @SerializedName("original_name") val originalName: String?,
    val overview: String?,
    val tagline: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("vote_average") val voteAverage: Double?,
    @SerializedName("vote_count") val voteCount: Int?,
    val genres: List<GenreDto>?,
    @SerializedName("number_of_seasons") val numberOfSeasons: Int?,
    @SerializedName("number_of_episodes") val numberOfEpisodes: Int?,
    @SerializedName("episode_run_time") val episodeRunTime: List<Int>?,
    val status: String?,
    val homepage: String?,
    @SerializedName("production_countries") val productionCountries: List<ProductionCountryDto>?,
    @SerializedName("production_companies") val productionCompanies: List<ProductionCompanyDto>?,
    val networks: List<ProductionCompanyDto>?,
    val seasons: List<SeasonDto>?
)
