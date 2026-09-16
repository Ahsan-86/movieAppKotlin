package com.ahsan.movieapp.domain.model

/**
 * One season of a TV show — Phase 2.6 Session 2's Seasons section, listed on the TV detail screen
 * below Cast & Crew. Backed by TMDB's `/tv/{id}` `seasons` array (comes free on the same call as
 * the rest of [TvShowDetails], no extra fetch needed for this list). Season 0 ("Specials") is
 * filtered out at the mapper — see [com.ahsan.movieapp.data.mapper.toDomain] for [TvShowDetails].
 */
data class Season(
    val seasonNumber: Int,
    val name: String,
    val overview: String,
    val posterUrl: String?,
    val episodeCount: Int,
    val airDate: String
) {
    val yearFormatted: String? get() = airDate.take(4).ifBlank { null }
    val episodeCountFormatted: String?
        get() = episodeCount.takeIf { it > 0 }?.let { "$it Episode${if (it != 1) "s" else ""}" }
}

/**
 * A season's full episode list — opened by tapping a [Season] on the TV detail screen. One-shot,
 * network-only fetch (`/tv/{id}/season/{season_number}`, see
 * [com.ahsan.movieapp.data.repository.MovieRepository.getSeasonDetails]), same no-Room-cache
 * convention as [com.ahsan.movieapp.data.repository.MovieRepository.getTvDetails]. Purely
 * informational for now — tapping an episode does nothing (parked for Phase 6 discussion, per the
 * project doc's Open items).
 */
data class SeasonDetails(
    val seasonNumber: Int,
    val name: String,
    val overview: String,
    val episodes: List<Episode>
)

data class Episode(
    val episodeNumber: Int,
    val name: String,
    val overview: String,
    val imageUrl: String?,
    val airDate: String
) {
    val airDateFormatted: String? get() = airDate.takeIf { it.isNotBlank() }
}
