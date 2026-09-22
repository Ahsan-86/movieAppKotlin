package com.ahsan.movieapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Phase 2.6 Session 3 — one row per TV show TMDB has ever returned to us across the cached TV
 * carousels (Trending TV, Popular TV). The TV mirror of [MovieEntity], deliberately in its own
 * `tv_shows` table rather than the shared `movies` table: TMDB reuses one numeric ID range across
 * movies and TV, so a TV id must never share the movie cache (or, more importantly, the
 * movie-only `favorites` table) until Session 6's composite-key migration. Fields track the TV
 * API's names (`name`, `first_air_date`) rather than the movie API's (`title`, `release_date`).
 */
@Entity(tableName = "tv_shows")
data class TvShowEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val firstAirDate: String,
    val voteAverage: Double,
    val voteCount: Int,
    val genreIds: List<Int>,
    val cachedAt: Long
)

/** Maps a [TvCategory][com.ahsan.movieapp.domain.model.TvCategory] to the ordered TV ids it contains. */
@Entity(tableName = "category_tv_shows", primaryKeys = ["category", "tvId"])
data class CategoryTvShowCrossRef(
    val category: String,
    val tvId: Int,
    val position: Int,
    val fetchedAt: Long
)

/** The `tv_remote_keys` counterpart of
 * [CategoryRemoteKeys][com.ahsan.movieapp.data.local.entity.CategoryRemoteKeys] — per TV category,
 * which TMDB page to fetch next and when, so
 * [com.ahsan.movieapp.data.paging.TvCategoryRemoteMediator] can resume "load more" across app
 * restarts. `nextPage == null` means TMDB reached this category's `total_pages`. */
@Entity(tableName = "tv_remote_keys")
data class TvRemoteKeys(
    @PrimaryKey val category: String,
    val nextPage: Int?,
    val fetchedAt: Long
)