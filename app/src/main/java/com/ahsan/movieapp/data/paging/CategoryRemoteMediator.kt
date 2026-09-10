package com.ahsan.movieapp.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.ahsan.movieapp.data.local.AppDatabase
import com.ahsan.movieapp.data.local.dao.MovieCategoryRow
import com.ahsan.movieapp.data.local.dao.MovieDao
import com.ahsan.movieapp.data.local.entity.CategoryRemoteKeys
import com.ahsan.movieapp.data.mapper.toEntity
import com.ahsan.movieapp.data.remote.dto.MovieDto
import com.ahsan.movieapp.data.remote.dto.PagedResponseDto

/**
 * Phase 4 (pagination) — the network half of one paginated category's offline-first "load more"
 * list. [MovieDao.pagingSourceForCategory] (via [androidx.paging.Pager]) is what actually feeds
 * the UI from Room; this only decides *when* to hit the network and *what* to save.
 *
 * Deliberately keeps this project's existing offline-first/staleness philosophy rather than the
 * "always refresh on screen open" default most Paging 3 samples use: [initialize] skips the
 * network entirely if the category was fetched recently (same [staleThresholdMs] window
 * [com.ahsan.movieapp.data.repository.MovieRepositoryImpl] already uses for its non-paged
 * categories), so reopening a screen with fresh cached data never re-fires TMDB calls just to
 * satisfy Paging 3's usual "always REFRESH on first collect" behavior. `category_remote_keys`
 * (see [CategoryRemoteKeys]) is what lets APPEND resume from the right TMDB page even after the
 * process restarts, since Room — not this class — is the actual source of truth in between loads.
 */
@OptIn(ExperimentalPagingApi::class)
class CategoryRemoteMediator(
    private val category: String,
    private val staleThresholdMs: Long,
    private val fetchPage: suspend (page: Int) -> PagedResponseDto<MovieDto>,
    private val database: AppDatabase,
    private val movieDao: MovieDao
) : RemoteMediator<Int, MovieCategoryRow>() {

    override suspend fun initialize(): InitializeAction {
        val fetchedAt = movieDao.categoryFetchedAt(category)
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
                    // TMDB categories are only ever fetched forward, page 1 onward — there is
                    // nothing "before" what's already loaded.
                    MediatorResult.Success(endOfPaginationReached = true)

                LoadType.REFRESH -> {
                    val response = fetchPage(1)
                    val now = System.currentTimeMillis()
                    database.withTransaction {
                        movieDao.replaceCategory(category, response.results.map { it.toEntity(now) }, now)
                        movieDao.upsertRemoteKeys(
                            CategoryRemoteKeys(
                                category = category,
                                nextPage = 2.takeIf { response.page < response.totalPages },
                                fetchedAt = now
                            )
                        )
                    }
                    MediatorResult.Success(endOfPaginationReached = response.page >= response.totalPages)
                }

                LoadType.APPEND -> {
                    val keys = movieDao.remoteKeys(category)
                    val nextPage = keys?.nextPage
                        ?: return MediatorResult.Success(endOfPaginationReached = true)
                    val response = fetchPage(nextPage)
                    val now = System.currentTimeMillis()
                    val startPosition = (movieDao.maxCategoryPosition(category) ?: -1) + 1
                    database.withTransaction {
                        movieDao.appendCategory(category, response.results.map { it.toEntity(now) }, startPosition, now)
                        movieDao.upsertRemoteKeys(
                            keys.copy(nextPage = (nextPage + 1).takeIf { response.page < response.totalPages })
                        )
                    }
                    MediatorResult.Success(endOfPaginationReached = response.page >= response.totalPages)
                }
            }
        } catch (throwable: Throwable) {
            MediatorResult.Error(throwable)
        }
    }
}
