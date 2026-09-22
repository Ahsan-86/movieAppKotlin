package com.ahsan.movieapp.domain.model

import java.util.Locale

data class MovieDetails(
    val id: Int,
    val title: String,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val releaseDate: String,
    val runtimeMinutes: Int?,
    val voteAverage: Double,
    val voteCount: Int,
    val genres: List<String>,
    val tagline: String?,
    val isFavorite: Boolean = false,
    // Everything below backs the Detail screen's Information section (Phase 3) — see
    // MovieDetailsEntity for why these live in a separate Room migration bump.
    val originalTitle: String = "",
    val status: String? = null,
    val homepage: String? = null,
    val budget: Long = 0L,
    val revenue: Long = 0L,
    val productionCountries: List<String> = emptyList(),
    val productionCompanies: List<ProductionCompany> = emptyList(),
    /**
     * Set only when this movie belongs to a TMDB "collection" (a franchise, e.g. a trilogy) — comes
     * free on the same `/movie/{id}` call as everything else above (`belongs_to_collection`), no
     * extra fetch. Backs the Detail screen's compact collection teaser (Phase 3 Round B); tapping it
     * opens a new full-screen [MovieCollection] listing every movie in the collection, fetched
     * separately via [MovieCollection] once the teaser is tapped.
     */
    val collection: CollectionSummary? = null
) {
    val releaseYear: String get() = releaseDate.take(4).ifBlank { "—" }
    val ratingOutOfTen: String get() = String.format(Locale.US, "%.1f", voteAverage)
    val runtimeFormatted: String? get() = runtimeMinutes?.let { "${it / 60}h ${it % 60}m" }

    val originalTitleIfPresent: String? get() = originalTitle.takeIf { it.isNotBlank() }

    val budgetFormatted: String? get() = budget.takeIf { it > 0 }?.let { "$${"%,d".format(it)}" }
    val revenueFormatted: String? get() = revenue.takeIf { it > 0 }?.let { "$${"%,d".format(it)}" }
    val homepageIfPresent: String? get() = homepage?.takeIf { it.isNotBlank() }
    val statusIfPresent: String? get() = status?.takeIf { it.isNotBlank() }
    val countriesFormatted: String? get() = productionCountries.takeIf { it.isNotEmpty() }?.joinToString(", ")

    /** Names only, comma-separated — the Information section shows no company logos/images. */
    val productionCompaniesFormatted: String?
        get() = productionCompanies.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.name }
}

/**
 * One entry from [MovieDetails.productionCompanies] — name plus an optional logo. [logoUrl] is
 * still captured from TMDB and stored (see MovieDetailsEntity), but the Information section only
 * ever displays [name] (see [MovieDetails.productionCompaniesFormatted]) — Ahsan asked for
 * names-only, comma-separated, no logos/images.
 */
data class ProductionCompany(
    val name: String,
    val logoUrl: String?
)

/**
 * The compact "part of a collection" teaser shown on the Detail screen (Phase 3 Round B) — just
 * enough to render a tappable poster + name row. The full collection (overview + every movie in
 * it) is a separate one-shot fetch, [MovieCollection], only made once the teaser is tapped.
 */
data class CollectionSummary(
    val id: Int,
    val name: String,
    val posterUrl: String?
)

/**
 * The full "franchise" list opened from the Detail screen's collection teaser (Phase 3 Round B) —
 * every movie belonging to a TMDB collection, each clickable through to its own Detail screen.
 * One-shot, network-only fetch (see MovieRepositoryImpl.getCollectionDetails); no Room cache, same
 * convention as [MovieCredits] and the other newer Phase-3 data that doesn't have its own offline
 * table yet.
 */
data class MovieCollection(
    val id: Int,
    val name: String,
    val overview: String?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val movies: List<Movie>
)

/**
 * Streaming availability for the Detail screen's Round C section — every TMDB region this movie
 * has JustWatch data for, plus each region's own [WatchProviderRegion]. One-shot, network-only
 * fetch (see MovieRepositoryImpl.getWatchProviders) that covers every region in a single TMDB
 * call; [regionsAvailable] backs the region dropdown, and switching regions is pure client-side
 * lookup via [forRegion] — never a re-fetch.
 */
data class WatchProviders(
    val regionsAvailable: List<String>,
    private val byRegion: Map<String, WatchProviderRegion>
) {
    fun forRegion(regionCode: String): WatchProviderRegion? = byRegion[regionCode]
}

/**
 * One region's Stream/Rent/Buy provider lists, per Round C's confirmed scope (TMDB's `free`/`ads`
 * ad-supported categories aren't surfaced). [link] is TMDB's own watch-providers page for this
 * movie+region, included for a future "more info" affordance but not currently used by the UI.
 */
data class WatchProviderRegion(
    val link: String?,
    val flatrate: List<WatchProvider>,
    val rent: List<WatchProvider>,
    val buy: List<WatchProvider>
) {
    val isEmpty: Boolean get() = flatrate.isEmpty() && rent.isEmpty() && buy.isEmpty()
}

/** One provider logo in a Stream/Rent/Buy row (e.g. Netflix, Amazon Video). */
data class WatchProvider(
    val id: Int,
    val name: String,
    val logoUrl: String?
)

data class CastMember(
    val id: Int,
    val name: String,
    val character: String,
    val profileUrl: String?,
    val order: Int
)

/**
 * Cast + director for the Detail screen's cast/crew section (Phase 3 Round A) and its "view all"
 * expansion. One-shot, network-only fetch (see MovieRepositoryImpl.getMovieCredits) — cast +
 * director only, no other crew roles, per the round's confirmed scope.
 */
data class MovieCredits(
    val cast: List<CastMember>,
    val director: Person?
)

/** A person surfaced from search or a cast credit, used to drill into their filmography. */
data class Person(
    val id: Int,
    val name: String,
    val profileUrl: String?,
    val knownFor: String? = null
)

/**
 * Bio/photo shown at the top of the person screen. [birthday]/[deathday] are TMDB's ISO
 * "YYYY-MM-DD" strings, both nullable — [age] and the header rely on them. [gender] is TMDB's
 * numeric code; the caller maps it to a localized label (Female/Male/Non-binary/Unknown).
 */
data class PersonDetails(
    val id: Int,
    val name: String,
    val profileUrl: String?,
    val biography: String?,
    /** Friendly primary-role label, e.g. "Actor" or "Director" — derived from TMDB's known_for_department. */
    val role: String?,
    val birthday: String? = null,
    val deathday: String? = null,
    /** TMDB's numeric gender code: 0 = unspecified, 1 = female, 2 = male, 3 = non-binary. */
    val gender: Int? = null
) {
    /**
     * Age in years as of today, or age at death when [deathday] is present. Null when [birthday]
     * is missing or unparseable, so the header can drop the segment entirely instead of showing a
     * bogus "0 years old". Uses plain integer date-part math rather than java.time, since this
     * module doesn't assume a desugared minSdk. The caller formats it for display ("N years old").
     */
    val age: Int?
        get() {
            val birth = birthday?.takeIf { it.isNotBlank() }?.let(::parseIsoDate) ?: return null
            val reference = deathday?.takeIf { it.isNotBlank() }?.let(::parseIsoDate) ?: todayDateParts()
            var age = reference.year - birth.year
            if (reference.month < birth.month || (reference.month == birth.month && reference.day < birth.day)) {
                age--
            }
            return age.takeIf { it >= 0 }
        }
}

private data class DateParts(val year: Int, val month: Int, val day: Int)

private fun parseIsoDate(value: String): DateParts? {
    val parts = value.split("-")
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    return DateParts(year, month, day)
}

private fun todayDateParts(): DateParts {
    val calendar = java.util.Calendar.getInstance()
    return DateParts(
        year = calendar.get(java.util.Calendar.YEAR),
        month = calendar.get(java.util.Calendar.MONTH) + 1,
        day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
    )
}

/**
 * A person's combined movie/TV credits, pre-split by media type and by role (acting vs
 * directing) so the person screen's toggles are pure client-side filtering over data already
 * fetched once — no re-fetch per toggle switch.
 */
data class PersonCredits(
    val actingMovies: List<Movie>,
    val directingMovies: List<Movie>,
    val actingTvShows: List<Movie>,
    val directingTvShows: List<Movie>
) {
    val hasTvCredits: Boolean get() = actingTvShows.isNotEmpty() || directingTvShows.isNotEmpty()
    fun hasDirectingCredits(isTv: Boolean): Boolean = if (isTv) directingTvShows.isNotEmpty() else directingMovies.isNotEmpty()
}

/**
 * One "browse by genre" chip on the search screen's first-open state. TMDB uses separate genre-id
 * spaces for movies and TV (e.g. movie "Action" = 28 vs TV "Action & Adventure" = 10759), and a
 * handful of genres only exist for one media type (currently, in this app's curated list:
 * movie-only Horror, Romance, Thriller) — so a chip carries both ids and either one may be null,
 * though the app doesn't currently include any TV-only genre in its curated list.
 */
data class GenreChip(
    val movieGenreId: Int?,
    val tvGenreId: Int?,
    val name: String,
    /**
     * A real poster from an already-cached movie in this genre — null if nothing's cached yet for
     * it (would always be null for a TV-only genre, since this app doesn't cache TV posters).
     */
    val imageUrl: String?
) {
    /** Stable identity for nav args / list keys — whichever id this chip actually has. */
    val navId: Int get() = movieGenreId ?: tvGenreId ?: 0
}
