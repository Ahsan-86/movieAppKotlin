package com.ahsan.movieapp.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.ahsan.movieapp.data.local.AppDatabase
import com.ahsan.movieapp.data.local.dao.TvShowDao
import com.ahsan.movieapp.data.local.entity.TvRemoteKeys
import com.ahsan.movieapp.data.local.entity.TvShowEntity
import com.ahsan.movieapp.data.mapper.toTvEntity
import com.ahsan.movieapp.data.remote.dto.PagedResponseDto
import com.ahsan.movieapp.data.remote.dto.TvShowDto

/**
 * Phase 2.6 Session 3 — the network half of one cached TV category's offline-first listing, the
 * [CategoryRemoteMediator] counterpart for the `tv_shows`/`category_tv_shows`/`tv_remote_keys`
 * tables. Same shape in every respect: [TvShowDao.pagingSourceForCategoryTv] feeds the UI from
 * Room, this only decides *when* to hit the network and *what* to save, [initialize] skips the
 * fetch entirely while the category is within its staleness window, and [TvRemoteKeys] is what lets
 * APPEND resume from the right TMDB page after a process restart.
 */
@OptIn(ExperimentalPagingApi::class)
class TvCategoryRemoteMediator(
    private val category: String,
    private val staleThresholdMs: Long,
    private val fetchPage: suspend (page: Int) -> PagedResponseDto<TvShowDto>,
    private val database: AppDatabase,
    private val tvShowDao: TvShowDao
) : RemoteMediator<Int, TvShowEntity>() {

    override suspend fun initialize(): InitializeAction {
        val fetchedAt = tvShowDao.categoryTvFetchedAt(category)
        return if (fetchedAt != null && System.currentTimeMillis() - fetchedAt <= staleThresholdMs) {
            InitializeAction.SKIP_INITIAL_REFRESH
        } else {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        }
    }

    override suspend fun load(loadType: LoadType, state: PagingState<Int, TvShowEntity>): MediatorResult {
        return try {
            when (loadType) {
                LoadType.PREPEND ->
                    // TV categories are only ever fetched forward, page 1 onward.
                    MediatorResult.Success(endOfPaginationReached = true)

                LoadType.REFRESH -> {
                    val response = fetchPage(1)
                    val now = System.currentTimeMillis()
                    database.withTransaction {
                        tvShowDao.replaceCategoryTv(category, response.results.map { it.toTvEntity(now) }, now)
                        tvShowDao.upsertTvRemoteKeys(
                            TvRemoteKeys(
                                category = category,
                                nextPage = 2.takeIf { response.page < response.totalPages },
                                fetchedAt = now
                            )
                        )
                    }
                    MediatorResult.Success(endOfPaginationReached = response.page >= response.totalPages)
                }

                LoadType.APPEND -> {
                    val keys = tvShowDao.tvRemoteKeys(category)
                    val nextPage = keys?.nextPage
                        ?: return MediatorResult.Success(endOfPaginationReached = true)
                    val response = fetchPage(nextPage)
                    val now = System.currentTimeMillis()
                    val startPosition = (tvShowDao.maxCategoryTvPosition(category) ?: -1) + 1
                    database.withTransaction {
                        tvShowDao.appendCategoryTv(category, response.results.map { it.toTvEntity(now) }, startPosition, now)
                        tvShowDao.upsertTvRemoteKeys(
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