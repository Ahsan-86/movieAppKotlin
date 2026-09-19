package com.ahsan.movieapp.domain.model

import java.util.Locale

/**
 * TV counterpart of [MovieDetails] — Phase 2.6's TV detail screen (info section, Cast & Crew, and
 * an Information section added on Ahsan's post-build feedback, Session 1; a Seasons section,
 * Session 2). Deliberately smaller than [MovieDetails] in one respect: no budget/revenue/
 * collection-teaser fields, since those are movie-specific TMDB fields with no TV equivalent.
 *
 * Cast + director reuse [MovieCredits] rather than a new `TvCredits` type — see
 * [com.ahsan.movieapp.data.repository.MovieRepository.getTvCredits] — since TMDB's
 * `/tv/{id}/credits` response has the exact same cast/crew shape as the movie side.
 */
data class TvShowDetails(
    val id: Int,
    val name: String,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val firstAirDate: String,
    val voteAverage: Double,
    val voteCount: Int,
    val genres: List<String>,
    val tagline: String?,
    val numberOfSeasons: Int?,
    val numberOfEpisodes: Int?,
    /** TMDB returns `episode_run_time` as a list (one entry per differing runtime the show has
     *  used); the first entry is shown as a representative typical episode length, same
     *  "good enough single value" approach as [MovieDetails.runtimeFormatted]. */
    val episodeRuntimeMinutes: Int?,
    // Everything below backs the TV detail screen's Information section — same convention as
    // [MovieDetails]'s Information fields, names-only for networks/production companies (no
    // logos/images), per Ahsan's original request for the movie side.
    val originalName: String = "",
    val status: String? = null,
    val homepage: String? = null,
    val countries: List<String> = emptyList(),
    val networks: List<String> = emptyList(),
    val productionCompanies: List<String> = emptyList(),
    // Phase 2.6 Session 2 — backs the Seasons section. Season 0 ("Specials") is filtered out at
    // the mapper, not here — see [com.ahsan.movieapp.data.mapper.toDomain].
    val seasons: List<Season> = emptyList()
) {
    val releaseYear: String get() = firstAirDate.take(4).ifBlank { "—" }
    val ratingOutOfTen: String get() = String.format(Locale.US, "%.1f", voteAverage)

    val seasonsFormatted: String?
        get() = numberOfSeasons?.takeIf { it > 0 }?.let { "$it Season${if (it != 1) "s" else ""}" }

    val episodeRuntimeFormatted: String?
        get() = episodeRuntimeMinutes?.takeIf { it > 0 }?.let { "${it}m" }

    val originalNameIfPresent: String? get() = originalName.takeIf { it.isNotBlank() && it != name }
    val statusIfPresent: String? get() = status?.takeIf { it.isNotBlank() }
    val homepageIfPresent: String? get() = homepage?.takeIf { it.isNotBlank() }
    val countriesFormatted: String? get() = countries.takeIf { it.isNotEmpty() }?.joinToString(", ")
    val networksFormatted: String? get() = networks.takeIf { it.isNotEmpty() }?.joinToString(", ")

    /** Names only, comma-separated — same "no logos/images" convention as [MovieDetails.productionCompaniesFormatted]. */
    val productionCompaniesFormatted: String?
        get() = productionCompanies.takeIf { it.isNotEmpty() }?.joinToString(", ")
}
