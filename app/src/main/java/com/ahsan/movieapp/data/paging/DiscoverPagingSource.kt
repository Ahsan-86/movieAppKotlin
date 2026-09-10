package com.ahsan.movieapp.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.ahsan.movieapp.data.local.dao.FavoriteDao
import com.ahsan.movieapp.data.local.dao.MovieDao
import com.ahsan.movieapp.data.mapper.toDomain
import com.ahsan.movieapp.data.mapper.toEntity
import com.ahsan.movieapp.data.remote.TmdbApi
import com.ahsan.movieapp.domain.model.DiscoverFilters
import com.ahsan.movieapp.domain.model.Movie
import kotlinx.coroutines.flow.first

/**
 * Phase 4 (pagination) Round 4 — the search screen's filter-panel Discover results (Phase 2.5).
 * Network-only, straight over [TmdbApi.discoverMovies] with whichever filter combination was last
 * Applied. No Room cache by filter combination (there are too many combinations to usefully cache
 * each one) and so no [androidx.paging.RemoteMediator] — same reasoning as [SearchMoviesPagingSource]
 * and [com.ahsan.movieapp.data.paging.TvGenrePagingSource].
 *
 * Every returned movie still gets upserted into the shared `movies` table. Favorite status is
 * resolved the same way as [SearchMoviesPagingSource] — a one-time snapshot per `load()` call via
 * `favoriteDao.observeFavoriteMovies().first()`, not a live `combine()` — see that class's doc for
 * why (a `combine()`-based live flow over this pager's [androidx.paging.PagingData] crashed with
 * `IllegalStateException: Attempt to collect twice from pageEventFlow`).
 */
class DiscoverPagingSource(
    private val api: TmdbApi,
    private val movieDao: MovieDao,
    private val favoriteDao: FavoriteDao,
    private val filters: DiscoverFilters
) : PagingSource<Int, Movie>() {

    // Same defensive guard as SearchMoviesPagingSource — TMDB's page boundaries aren't guaranteed
    // stable when results tie on the sort key, so drop any movie id this instance has already
    // emitted on an earlier page rather than risk the LazyGrid itemKey{} duplicate-key crash.
    private val seenMovieIds = mutableSetOf<Int>()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> {
        val page = params.key ?: 1
        return try {
            val response = api.discoverMovies(
                genreIds = filters.genreId?.toString(),
                year = filters.year,
                language = filters.language,
                minRating = filters.minRating,
                page = page
            )
            val results = response.results.filter { seenMovieIds.add(it.id) }
            val now = System.currentTimeMillis()
            if (results.isNotEmpty()) {
                movieDao.upsertMovies(results.map { it.toEntity(now) })
            }
            val favoriteIds = favoriteDao.observeFavoriteMovies().first().map { it.id }.toSet()
            LoadResult.Page(
                data = results.map { it.toEntity(now).toDomain(isFavorite = it.id in favoriteIds) },
                prevKey = null,
                nextKey = (page + 1).takeIf { page < response.totalPages }
            )
        } catch (throwable: Throwable) {
            LoadResult.Error(throwable)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Movie>): Int? =
        state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
}
