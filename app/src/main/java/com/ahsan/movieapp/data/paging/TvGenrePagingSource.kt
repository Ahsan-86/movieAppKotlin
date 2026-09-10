package com.ahsan.movieapp.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.ahsan.movieapp.data.mapper.toMovie
import com.ahsan.movieapp.data.remote.TmdbApi
import com.ahsan.movieapp.domain.model.Movie

/**
 * Phase 4 (pagination) Round 3 — the genre screen's TV tab. Network-only, straight over
 * [TmdbApi.discoverTvByGenres] with no Room table and no [androidx.paging.RemoteMediator] behind
 * it, unlike [CategoryRemoteMediator]'s offline-first pattern: this app doesn't persist TV data
 * yet (same one-screen exception as
 * [com.ahsan.movieapp.data.repository.MovieRepository.getPopularTv]), so there's nothing for a
 * mediator to page into — a plain [PagingSource] reading TMDB pages directly is the whole story.
 */
class TvGenrePagingSource(
    private val api: TmdbApi,
    private val genreId: Int
) : PagingSource<Int, Movie>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> {
        val page = params.key ?: 1
        return try {
            val response = api.discoverTvByGenres(genreId.toString(), page = page)
            LoadResult.Page(
                data = response.results.map { it.toMovie() },
                // TMDB genre browsing is only ever fetched forward, page 1 onward.
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
