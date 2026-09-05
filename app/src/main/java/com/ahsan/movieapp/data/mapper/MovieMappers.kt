package com.ahsan.movieapp.data.mapper

import com.ahsan.movieapp.data.local.entity.CastMemberEntity
import com.ahsan.movieapp.data.local.entity.MovieDetailsEntity
import com.ahsan.movieapp.data.local.entity.MovieEntity
import com.ahsan.movieapp.data.remote.dto.CastMemberDto
import com.ahsan.movieapp.data.remote.dto.MovieDetailsDto
import com.ahsan.movieapp.data.remote.dto.MovieDto
import com.ahsan.movieapp.data.remote.dto.PersonDto
import com.ahsan.movieapp.domain.model.CastMember
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.domain.model.MovieDetails
import com.ahsan.movieapp.domain.model.Person
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

fun MovieDetailsDto.toEntity(cachedAt: Long): MovieDetailsEntity = MovieDetailsEntity(
    movieId = id,
    tagline = tagline,
    runtimeMinutes = runtime,
    genreNames = genres.orEmpty().map { it.name },
    cachedAt = cachedAt
)

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
    isFavorite = isFavorite
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
