package com.ahsan.movieapp.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.ahsan.movieapp.data.local.AppDatabase
import com.ahsan.movieapp.data.local.dao.MovieCategoryRow
import com.ahsan.movieapp.data.local.dao.MovieDao
import com.ahsan.movieapp.data.local.entity.DiscoverComboRemoteKeys
import com.ahsan.movieapp.data.mapper.toEntity
import com.ahsan.movieapp.data.remote.dto.MovieDto
import com.ahsan.movieapp.data.remote.dto.PagedResponseDto

/**
 * Session 7 — the network half of one cached Discover filter combination (the search screen's
 * filter panel + the genre screen's filtered Movies tab). [MovieDao.pagingSourceForDiscover] feeds
 * the UI from Room; this only decides *when* to hit TMDB and *what* to save — the exact same shape
 * as [CategoryRemoteMediator] for the curated carousels, but keyed by a `comboKey` string (a
 * normalized `DiscoverFilters.comboKey()`) instead of a category name.
 *
 * Same offline-first staleness philosophy: [initialize] skips the network for combos fetched inside
 * [staleThresholdMs] (2h, same window as the carousels), so re-applying an identical filter combo
 * renders instantly from Room. `discover_combo_remote_keys` (see [DiscoverComboRemoteKeys]) lets
 * APPEND resume from the right TMDB page across process restarts — and also carries the combo's
 * `totalResults`, which replaces the old `DiscoverPagingSource`'s per-load "N results found" write
 * (the count now survives cached/offline reads instead of only appearing after a network page).
 *
 * The cache is bounded: every write evicts combos beyond the [keepCount] most recently fetched
 * (LRU by `fetchedAt`), so the naturally small combo space never grows into an unbounded table.
 */
@OptIn(ExperimentalPagingApi::class)
class DiscoverRemoteMediator(
    private val comboKey: String,
    private val staleThresholdMs: Long,
    private val keepCount: Int,
    private val fetchPage: suspend (page: Int) -> PagedResponseDto<MovieDto>,
    private val database: AppDatabase,
    private val movieDao: MovieDao
) : RemoteMediator<Int, MovieCategoryRow>() {

    override suspend fun initialize(): InitializeAction {
        val fetchedAt = movieDao.discoverRemoteKeys(comboKey)?.fetchedAt
        return if (fetchedAt != null && System.currentTimeMillis() - fetchedAt <= staleThresholdMs) {
            InitializeAction.SKIP_INITIAL_REFRESH
        } else {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        }
    }

    override suspend fun load(loadType: LoadType, state: PagingState<Int, MovieCategoryRow>): MediatorResult {
        return try {
            when (loadType) {
                LoadType.PREPEND ->
                    // Discover combos are only ever fetched forward, page 1 onward.
                    MediatorResult.Success(endOfPaginationReached = true)

                LoadType.REFRESH -> {
                    val response = fetchPage(1)
                    val now = System.currentTimeMillis()
                    database.withTransaction {
                        movieDao.replaceDiscoverCombo(comboKey, response.results.map { it.toEntity(now) }, now)
                        movieDao.upsertDiscoverRemoteKeys(
                            DiscoverComboRemoteKeys(
                                comboKey = comboKey,
                                nextPage = 2.takeIf { response.page < response.totalPages },
                                totalResults = response.totalResults,
                                fetchedAt = now
                            )
                        )
                        movieDao.evictDiscoverComboMovies(keepCount)
                        movieDao.evictDiscoverComboKeys(keepCount)
                    }
                    MediatorResult.Success(endOfPaginationReached = response.page >= response.totalPages)
                }

                LoadType.APPEND -> {
                    val keys = movieDao.discoverRemoteKeys(comboKey)
                    val nextPage = keys?.nextPage
                        ?: return MediatorResult.Success(endOfPaginationReached = true)
                    val response = fetchPage(nextPage)
                    val now = System.currentTimeMillis()
                    val startPosition = (movieDao.maxDiscoverPosition(comboKey) ?: -1) + 1
                    database.withTransaction {
                        movieDao.appendDiscoverCombo(comboKey, response.results.map { it.toEntity(now) }, startPosition, now)
                        movieDao.upsertDiscoverRemoteKeys(
                            keys.copy(nextPage = (nextPage + 1).takeIf { response.page < response.totalPages })
                        )
                        movieDao.evictDiscoverComboMovies(keepCount)
                        movieDao.evictDiscoverComboKeys(keepCount)
                    }
                    MediatorResult.Success(endOfPaginationReached = response.page >= response.totalPages)
                }
            }
        } catch (throwable: Throwable) {
            MediatorResult.Error(throwable)
        }
    }
}