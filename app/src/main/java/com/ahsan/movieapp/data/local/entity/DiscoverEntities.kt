package com.ahsan.movieapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Session 7 — the per-combo ordered movie list for the cached Discover/filter-panel results.
 * Mirrors [CategoryMovieCrossRef] (which keys curated carousels) but keyed by a Discover filter
 * combination's normalized string (a `DiscoverFilters.comboKey()`): one ordered list of movie ids
 * per applied genre/year/language/rating combo, page after page. The movies themselves live in the
 * shared `movies` table (upserted on fetch, exactly like the category cross-refs write there);
 * [DiscoverComboRemoteKeys] carries the resume bookkeeping.
 *
 * The combo space is naturally small (a few curated genres × years × a couple of languages × rating
 * steps), so this table stays bounded: every cache write evicts combos beyond the most-recent N
 * (LRU by last fetch) — see `MovieDao.evictDiscoverComboKeys` /
 * `MovieDao.evictDiscoverComboMovies` and [com.ahsan.movieapp.data.paging.DiscoverRemoteMediator].
 */
@Entity(tableName = "discover_combo_movies", primaryKeys = ["comboKey", "movieId"])
data class DiscoverComboMovieCrossRef(
    val comboKey: String,
    val movieId: Int,
    val position: Int,
    val fetchedAt: Long
)

/**
 * Session 7 — per cached Discover combo, which TMDB page to fetch next and when that bookkeeping was
 * last updated. The `category_remote_keys` counterpart for the filter-panel combos. `nextPage ==
 * null` means TMDB's `total_pages` was reached; `totalResults` mirrors the old
 * `DiscoverPagingSource`'s "N results found" write so the count survives cached/offline reads.
 */
@Entity(tableName = "discover_combo_remote_keys")
data class DiscoverComboRemoteKeys(
    @PrimaryKey val comboKey: String,
    val nextPage: Int?,
    val totalResults: Int?,
    val fetchedAt: Long
)