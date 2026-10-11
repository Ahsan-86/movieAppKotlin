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

/**
 * Whether a text-search result — a movie or a TV show — passes the search screen's client-side
 * [filters]. Year compares the release / first-air year, rating is a minimum on the vote average, and
 * language compares TMDB's original-language code (a result with no language on record, e.g. the
 * offline local-match fallback, doesn't pass a language filter). [DiscoverFilters.genreId] is a *movie*
 * genre id; a TV show is checked against [tvGenreId] instead (the matching TV id of that genre — TMDB
 * uses different ids for some, e.g. Action is 28 for movies but 10759 for TV — or null when the genre
 * has no TV counterpart, in which case no TV show passes a genre filter).
 */
fun Movie.matches(filters: DiscoverFilters, tvGenreId: Int? = null): Boolean =
    (filters.year == null || releaseDate.startsWith(filters.year.toString())) &&
        (filters.minRating == null || voteAverage >= filters.minRating) &&
        (filters.language == null || originalLanguage == filters.language) &&
        (filters.genreId == null || (if (mediaType == MediaType.TV) tvGenreId else filters.genreId)?.let { it in genreIds } == true)

/** The four criteria of [DiscoverFilters]; used to drop a single one (a filter chip's ✕). */
enum class FilterField { GENRE, YEAR, LANGUAGE, MIN_RATING }

/** These filters minus one criterion. */
fun DiscoverFilters.without(field: FilterField): DiscoverFilters = when (field) {
    FilterField.GENRE -> copy(genreId = null)
    FilterField.YEAR -> copy(year = null)
    FilterField.LANGUAGE -> copy(language = null)
    FilterField.MIN_RATING -> copy(minRating = null)
}
