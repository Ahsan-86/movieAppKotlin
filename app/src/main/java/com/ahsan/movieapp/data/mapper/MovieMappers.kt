package com.ahsan.movieapp.data.mapper

import com.ahsan.movieapp.data.local.dao.MovieCategoryRow
import com.ahsan.movieapp.data.local.entity.CastMemberEntity
import com.ahsan.movieapp.data.local.entity.MovieDetailsEntity
import com.ahsan.movieapp.data.local.entity.MovieEntity
import com.ahsan.movieapp.data.remote.dto.CastMemberDto
import com.ahsan.movieapp.data.remote.dto.CollectionDetailsDto
import com.ahsan.movieapp.data.remote.dto.CombinedCreditDto
import com.ahsan.movieapp.data.remote.dto.CombinedCreditsDto
import com.ahsan.movieapp.data.remote.dto.CrewMemberDto
import com.ahsan.movieapp.data.remote.dto.MovieDetailsDto
import com.ahsan.movieapp.data.remote.dto.MovieDto
import com.ahsan.movieapp.data.remote.dto.MultiSearchResultDto
import com.ahsan.movieapp.data.remote.dto.PersonDetailsDto
import com.ahsan.movieapp.data.remote.dto.PersonDto
import com.ahsan.movieapp.data.remote.dto.ProductionCompanyDto
import com.ahsan.movieapp.data.remote.dto.TvShowDto
import com.ahsan.movieapp.data.remote.dto.WatchProviderDto
import com.ahsan.movieapp.data.remote.dto.WatchProviderRegionDto
import com.ahsan.movieapp.data.remote.dto.WatchProvidersResponseDto
import com.ahsan.movieapp.domain.model.CastMember
import com.ahsan.movieapp.domain.model.CollectionSummary
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.domain.model.MovieCollection
import com.ahsan.movieapp.domain.model.MovieDetails
import com.ahsan.movieapp.domain.model.Person
import com.ahsan.movieapp.domain.model.PersonCredits
import com.ahsan.movieapp.domain.model.PersonDetails
import com.ahsan.movieapp.domain.model.ProductionCompany
import com.ahsan.movieapp.domain.model.WatchProvider
import com.ahsan.movieapp.domain.model.WatchProviderRegion
import com.ahsan.movieapp.domain.model.WatchProviders
import com.ahsan.movieapp.util.Constants

fun MovieDto.toEntity(cachedAt: Long): MovieEntity = MovieEntity(
    id = id,
    title = displayTitle,
    overview = overview.orEmpty(),
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = releaseDate.orEmpty(),
    voteAverage = voteAverage ?: 0.0,
    voteCount = voteCount ?: 0,
    genreIds = genreIds.orEmpty(),
    cachedAt = cachedAt
)

fun MovieEntity.toDomain(isFavorite: Boolean = false): Movie = Movie(
    id = id,
    title = title,
    overview = overview,
    posterUrl = Constants.posterUrl(posterPath),
    backdropUrl = Constants.backdropUrl(backdropPath),
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    voteCount = voteCount,
    genreIds = genreIds,
    isFavorite = isFavorite
)

/** Phase 4 (pagination) — [MovieCategoryRow] already carries its own favorite status (computed in
 * SQL via a `LEFT JOIN`), so this just forwards it instead of a caller having to cross-reference a
 * separate favorites list the way the non-paged category flows do. */
fun MovieCategoryRow.toDomain(): Movie = movie.toDomain(isFavorite)

fun MovieDetailsDto.toEntity(cachedAt: Long): MovieDetailsEntity = MovieDetailsEntity(
    movieId = id,
    tagline = tagline,
    runtimeMinutes = runtime,
    genreNames = genres.orEmpty().map { it.name },
    originalTitle = originalTitle.orEmpty(),
    status = status,
    homepage = homepage,
    budget = budget ?: 0L,
    revenue = revenue ?: 0L,
    productionCountries = productionCountries.orEmpty().map { it.name },
    productionCompaniesRaw = encodeProductionCompanies(productionCompanies.orEmpty()),
    collectionId = belongsToCollection?.id,
    collectionName = belongsToCollection?.name,
    collectionPosterPath = belongsToCollection?.posterPath,
    cachedAt = cachedAt
)

/**
 * [MovieDetailsEntity.productionCompaniesRaw]'s encoding: one entry per company as
 * "name␟logoPath" (logoPath empty when absent), entries joined by a doubled "␟␟" — reusing the
 * same unit-separator character Converters.fromStringList/toStringList already use for
 * List<String> columns, rather than adding a dedicated Room TypeConverter for this one field.
 * Safe as long as neither a company name nor a TMDB logo path ever contains "␟" (they don't —
 * logo paths are plain URL segments).
 */
private const val COMPANY_FIELD_SEPARATOR = "␟"
private const val COMPANY_ENTRY_SEPARATOR = "␟␟"

private fun encodeProductionCompanies(companies: List<ProductionCompanyDto>): String =
    companies.joinToString(COMPANY_ENTRY_SEPARATOR) { "${it.name}$COMPANY_FIELD_SEPARATOR${it.logoPath.orEmpty()}" }

private fun decodeProductionCompanies(raw: String): List<ProductionCompany> =
    if (raw.isBlank()) emptyList() else raw.split(COMPANY_ENTRY_SEPARATOR).mapNotNull { entry ->
        val parts = entry.split(COMPANY_FIELD_SEPARATOR)
        val name = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val logoPath = parts.getOrNull(1)?.takeIf { it.isNotBlank() }
        ProductionCompany(name = name, logoUrl = Constants.logoUrl(logoPath))
    }

fun MovieDetailsDto.toMovieEntity(cachedAt: Long): MovieEntity = MovieEntity(
    id = id,
    title = title,
    overview = overview.orEmpty(),
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = releaseDate.orEmpty(),
    voteAverage = voteAverage ?: 0.0,
    voteCount = voteCount ?: 0,
    genreIds = genres.orEmpty().map { it.id },
    cachedAt = cachedAt
)

fun combineToMovieDetails(base: MovieEntity, extra: MovieDetailsEntity?, isFavorite: Boolean): MovieDetails = MovieDetails(
    id = base.id,
    title = base.title,
    overview = base.overview,
    posterUrl = Constants.posterUrl(base.posterPath),
    backdropUrl = Constants.backdropUrl(base.backdropPath, Constants.BACKDROP_WIDTH),
    releaseDate = base.releaseDate,
    runtimeMinutes = extra?.runtimeMinutes,
    voteAverage = base.voteAverage,
    voteCount = base.voteCount,
    genres = extra?.genreNames.orEmpty(),
    tagline = extra?.tagline,
    isFavorite = isFavorite,
    originalTitle = extra?.originalTitle.orEmpty(),
    status = extra?.status,
    homepage = extra?.homepage,
    budget = extra?.budget ?: 0L,
    revenue = extra?.revenue ?: 0L,
    productionCountries = extra?.productionCountries.orEmpty(),
    productionCompanies = extra?.productionCompaniesRaw?.let { decodeProductionCompanies(it) }.orEmpty(),
    collection = extra?.collectionId?.let { id ->
        CollectionSummary(
            id = id,
            name = extra.collectionName.orEmpty(),
            posterUrl = Constants.posterUrl(extra.collectionPosterPath, Constants.POSTER_WIDTH_SMALL)
        )
    }
)

fun CastMemberDto.toEntity(movieId: Int): CastMemberEntity = CastMemberEntity(
    movieId = movieId,
    personId = id,
    name = name,
    character = character.orEmpty(),
    profilePath = profilePath,
    order = order
)

fun CastMemberEntity.toDomain(): CastMember = CastMember(
    id = personId,
    name = name,
    character = character,
    profileUrl = Constants.profileUrl(profilePath),
    order = order
)

fun PersonDto.toDomain(): Person = Person(
    id = id,
    name = name,
    profileUrl = Constants.profileUrl(profilePath),
    knownFor = knownForDepartment
)

/**
 * Direct DTO -> domain mapping for the Detail screen's cast/crew section (Phase 3 Round A) — this
 * path is network-only (see MovieRepositoryImpl.getMovieCredits) and deliberately doesn't go
 * through Room, unlike [CastMemberEntity.toDomain] above, which backs the separate (currently
 * unused) offline-cached cast path left in place from Phase 1.
 */
fun CastMemberDto.toDomain(): CastMember = CastMember(
    id = id,
    name = name,
    character = character.orEmpty(),
    profileUrl = Constants.profileUrl(profilePath),
    order = order
)

/** A director credit surfaced as a [Person] so it can reuse the same PersonScreen drill-down as cast. */
fun CrewMemberDto.toPerson(): Person = Person(
    id = id,
    name = name,
    profileUrl = Constants.profileUrl(profilePath),
    knownFor = job
)

/**
 * `/collection/{id}` -> the Detail screen's full "franchise" list (Phase 3 Round B). `parts`
 * decodes straight into the existing [MovieDto], mapped here directly to [Movie] (bypassing Room,
 * same convention as [TvShowDto.toMovie] and [CombinedCreditDto.toMovie]) since this path is
 * network-only — see MovieRepositoryImpl.getCollectionDetails for why. Every part still gets
 * upserted into the shared `movies` table by the repository afterward, so each movie is just as
 * available offline afterward as one found any other way; this mapper just produces the in-memory
 * domain objects for the screen itself.
 */
fun CollectionDetailsDto.toDomain(favoriteIds: Set<Int>): MovieCollection = MovieCollection(
    id = id,
    name = name,
    overview = overview?.takeIf { it.isNotBlank() },
    posterUrl = Constants.posterUrl(posterPath),
    backdropUrl = Constants.backdropUrl(backdropPath),
    movies = parts.orEmpty().map { it.toDomainDirect(favoriteIds) }
)

private fun MovieDto.toDomainDirect(favoriteIds: Set<Int>): Movie = Movie(
    id = id,
    title = displayTitle,
    overview = overview.orEmpty(),
    posterUrl = Constants.posterUrl(posterPath),
    backdropUrl = Constants.backdropUrl(backdropPath),
    releaseDate = releaseDate.orEmpty(),
    voteAverage = voteAverage ?: 0.0,
    voteCount = voteCount ?: 0,
    genreIds = genreIds.orEmpty(),
    isFavorite = id in favoriteIds
)

/**
 * `/movie/{id}/watch/providers` -> the Detail screen's streaming-availability section (Phase 3
 * Round C). TMDB's response already covers every region in one payload — this just reshapes each
 * region's DTO into the domain [WatchProviderRegion] used for display, sorted by TMDB's own
 * `display_priority` (falls back to arrival order for providers where TMDB omits it).
 */
fun WatchProvidersResponseDto.toDomain(): WatchProviders {
    val regions = results.orEmpty()
    return WatchProviders(
        regionsAvailable = regions.keys.sorted(),
        byRegion = regions.mapValues { (_, region) -> region.toDomain() }
    )
}

private fun WatchProviderRegionDto.toDomain(): WatchProviderRegion = WatchProviderRegion(
    link = link,
    flatrate = flatrate.orEmpty().sortedProviders(),
    rent = rent.orEmpty().sortedProviders(),
    buy = buy.orEmpty().sortedProviders()
)

private fun List<WatchProviderDto>.sortedProviders(): List<WatchProvider> =
    sortedBy { it.displayPriority ?: Int.MAX_VALUE }.map {
        WatchProvider(id = it.providerId, name = it.providerName, logoUrl = Constants.logoUrl(it.logoPath))
    }

/** Bridges a `/search/multi` row (media_type == "movie") back into the existing MovieDto pipeline. */
fun MultiSearchResultDto.toMovieDto(): MovieDto = MovieDto(
    id = id,
    title = title,
    name = name,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    voteCount = voteCount,
    genreIds = genreIds,
    mediaType = mediaType
)

/** Bridges a `/search/multi` row (media_type == "person") back into the existing PersonDto pipeline. */
fun MultiSearchResultDto.toPersonDto(): PersonDto = PersonDto(
    id = id,
    name = name ?: title.orEmpty(),
    profilePath = profilePath,
    knownForDepartment = knownForDepartment
)

fun PersonDetailsDto.toDomain(): PersonDetails = PersonDetails(
    id = id,
    name = name,
    profileUrl = Constants.profileUrl(profilePath, Constants.PROFILE_WIDTH_LARGE),
    biography = biography?.takeIf { it.isNotBlank() },
    role = knownForDepartment?.let(::friendlyRoleLabel),
    birthday = birthday?.takeIf { it.isNotBlank() },
    deathday = deathday?.takeIf { it.isNotBlank() },
    gender = gender
)

private fun friendlyRoleLabel(department: String): String = when (department) {
    "Acting" -> "Actor"
    "Directing" -> "Director"
    else -> department
}

/**
 * Splits combined_credits into the four buckets the person screen's Movies/TV and
 * Actor/Director toggles read from directly — no re-fetch, just filtering already-fetched data.
 * These credits never touch Room (deliberately — see MovieRepositoryImpl.getPersonCredits):
 * TV entries especially shouldn't get upserted into the movie-only `movies` cache table before
 * this app has real TV support.
 *
 * Each bucket is sorted newest-first by release/first-air date (TMDB's own ordering isn't
 * date-based). A blank date — an announced/upcoming title TMDB hasn't assigned one yet — sorts
 * to the very end for free, since "" is lexically smaller than any real ISO date string; upcoming
 * titles that DO have a (future) date sort correctly above everything already released, no
 * separate handling needed for those.
 */
fun CombinedCreditsDto.toDomain(favoriteIds: Set<Int>): PersonCredits {
    val distinctCast = cast.distinctBy { it.id to it.mediaType }
    val distinctCrew = crew.distinctBy { it.id to it.mediaType to it.job }

    return PersonCredits(
        actingMovies = distinctCast.filter { it.mediaType == "movie" }.map { it.toMovie(favoriteIds) }.sortedByDescending { it.releaseDate },
        directingMovies = distinctCrew.filter { it.mediaType == "movie" && it.job == "Director" }.map { it.toMovie(favoriteIds) }.sortedByDescending { it.releaseDate },
        actingTvShows = distinctCast.filter { it.mediaType == "tv" }.map { it.toMovie(favoriteIds) }.sortedByDescending { it.releaseDate },
        directingTvShows = distinctCrew.filter { it.mediaType == "tv" && it.job == "Director" }.map { it.toMovie(favoriteIds) }.sortedByDescending { it.releaseDate }
    )
}

private fun CombinedCreditDto.toMovie(favoriteIds: Set<Int>): Movie = Movie(
    id = id,
    title = title ?: name ?: "Untitled",
    overview = overview.orEmpty(),
    posterUrl = Constants.posterUrl(posterPath),
    backdropUrl = Constants.backdropUrl(backdropPath),
    releaseDate = (releaseDate ?: firstAirDate).orEmpty(),
    voteAverage = voteAverage ?: 0.0,
    voteCount = voteCount ?: 0,
    genreIds = genreIds.orEmpty(),
    isFavorite = id in favoriteIds
)

/**
 * Bridges a `/discover/tv` row into the same [Movie] shape everything else renders — TV shows
 * can't be favorited yet (no schema support), so this always comes back with isFavorite = false.
 */
fun TvShowDto.toMovie(): Movie = Movie(
    id = id,
    title = name,
    overview = overview.orEmpty(),
    posterUrl = Constants.posterUrl(posterPath),
    backdropUrl = Constants.backdropUrl(backdropPath),
    releaseDate = firstAirDate.orEmpty(),
    voteAverage = voteAverage ?: 0.0,
    voteCount = voteCount ?: 0,
    genreIds = genreIds.orEmpty(),
    isFavorite = false
)
