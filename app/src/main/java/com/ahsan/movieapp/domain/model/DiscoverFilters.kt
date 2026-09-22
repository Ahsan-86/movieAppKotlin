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

/**
 * Session 7 — the deterministic Room cache key for a filter combination. Discover results are now
 * cached per applied combo (see `DiscoverComboMovieCrossRef`/`DiscoverComboRemoteKeys`), so the key
 * must stringify the exact combo: re-applying an identical panel selection resolves to the same
 * key and reuses the cached pages. Only non-null fields participate, each labeled so different
 * combos can never collide (e.g. year 2024 vs genre 2024).
 */
fun DiscoverFilters.comboKey(): String = buildList {
    genreId?.let { add("genre:$it") }
    year?.let { add("year:$it") }
    language?.let { add("lang:$it") }
    minRating?.let { add("rating:$it") }
}.joinToString("|").ifEmpty { "all" }
