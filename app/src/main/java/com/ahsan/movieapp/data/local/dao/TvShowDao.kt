package com.ahsan.movieapp.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ahsan.movieapp.data.local.entity.CategoryTvShowCrossRef
import com.ahsan.movieapp.data.local.entity.TvRemoteKeys
import com.ahsan.movieapp.data.local.entity.TvShowEntity
import kotlinx.coroutines.flow.Flow

/**
 * Phase 2.6 Session 3 — the `tv_shows`/`category_tv_shows`/`tv_remote_keys` mirror of [MovieDao],
 * backing the Explore screen's TV carousels the exact same way movies are backed. TV favorites
 * DON'T live here as a join — since Session 6 they're in the separate composite-key `favorites`
 * table (`mediaType = 'tv'`), folded in by MovieRepository.getCategoryTv via a favorites flow
 * (see MovieRepositoryImpl), never as a SQL join: TV ids share TMDB's numeric range with movie
 * ids and the two tables' keys mean nothing to each other.
 */
@Dao
interface TvShowDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTvShows(shows: List<TvShowEntity>)

    @Query("DELETE FROM category_tv_shows WHERE category = :category")
    suspend fun clearCategoryTv(category: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryTvRefs(refs: List<CategoryTvShowCrossRef>)

    @Transaction
    suspend fun replaceCategoryTv(category: String, shows: List<TvShowEntity>, fetchedAt: Long) {
        upsertTvShows(shows)
        clearCategoryTv(category)
        insertCategoryTvRefs(
            shows.mapIndexed { index, show ->
                CategoryTvShowCrossRef(category = category, tvId = show.id, position = index, fetchedAt = fetchedAt)
            }
        )
    }

    @Query(
        """
        SELECT tv_shows.* FROM tv_shows
        INNER JOIN category_tv_shows ON tv_shows.id = category_tv_shows.tvId
        WHERE category_tv_shows.category = :category
        ORDER BY category_tv_shows.position ASC
        """
    )
    fun observeCategoryTv(category: String): Flow<List<TvShowEntity>>

    @Query("SELECT MIN(fetchedAt) FROM category_tv_shows WHERE category = :category")
    suspend fun categoryTvFetchedAt(category: String): Long?

    /** The paged form of [observeCategoryTv], for [androidx.paging.Pager] + the TV mediator. */
    @Query(
        """
        SELECT tv_shows.* FROM tv_shows
        INNER JOIN category_tv_shows ON tv_shows.id = category_tv_shows.tvId
        WHERE category_tv_shows.category = :category
        ORDER BY category_tv_shows.position ASC
        """
    )
    fun pagingSourceForCategoryTv(category: String): PagingSource<Int, TvShowEntity>

    @Query("SELECT MAX(position) FROM category_tv_shows WHERE category = :category")
    suspend fun maxCategoryTvPosition(category: String): Int?

    /** Appends a freshly-fetched page to an existing TV category — the APPEND-half of the mediator. */
    @Transaction
    suspend fun appendCategoryTv(category: String, shows: List<TvShowEntity>, startPosition: Int, fetchedAt: Long) {
        upsertTvShows(shows)
        insertCategoryTvRefs(
            shows.mapIndexed { index, show ->
                CategoryTvShowCrossRef(category = category, tvId = show.id, position = startPosition + index, fetchedAt = fetchedAt)
            }
        )
    }

    @Query("SELECT * FROM tv_remote_keys WHERE category = :category")
    suspend fun tvRemoteKeys(category: String): TvRemoteKeys?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTvRemoteKeys(keys: TvRemoteKeys)
}