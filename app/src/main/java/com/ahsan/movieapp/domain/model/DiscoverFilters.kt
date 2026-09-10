package com.ahsan.movieapp.domain.model

/**
 * Criteria for the search screen's collapsible filter panel (Phase 2.5) — backs a plain
 * `/discover/movie` call rather than `/search/multi`, since TMDB's text-search endpoints don't
 * accept any of these params. Only meaningful when there's no text query typed; each field is a
 * single-select choice for this first version of the panel (no combining, say, two genres).
 */
data class DiscoverFilters(
    val genreId: Int? = null,
    val year: Int? = null,
    val language: String? = null,
    val minRating: Float? = null
) {
    val isEmpty: Boolean get() = genreId == null && year == null && language == null && minRating == null

    val activeCount: Int get() = listOfNotNull(genreId, year, language, minRating).size
}
