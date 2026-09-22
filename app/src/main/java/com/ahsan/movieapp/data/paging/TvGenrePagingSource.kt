package com.ahsan.movieapp.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.ahsan.movieapp.data.mapper.toMovie
import com.ahsan.movieapp.data.remote.TmdbApi
import com.ahsan.movieapp.domain.model.DiscoverFilters
import com.ahsan.movieapp.domain.model.Movie
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Phase 4 (pagination) Round 3 — the genre screen's TV tab. Network-only, straight over
 * [TmdbApi.discoverTvByGenres] with no Room table and no [androidx.paging.RemoteMediator] behind
 * it, unlike [CategoryRemoteMediator]'s offline-first pattern: a genre+filter TV search has no
 * cache table of its own (only the curated TV carousels are cached — see
 * [com.ahsan.movieapp.data.repository.MovieRepository.getCategoryTv]), so there's nothing for a
 * mediator to page into — a plain [PagingSource] reading TMDB pages directly is the whole story.
 * [filters] (year/language/minimum rating — the genre is already [genreId]) fold into the discover
 * call: a [DiscoverFilters] with nothing set pages the plain genre list, so the genre screen's
 * filter section can switch between the two without swapping sources.
 */
class TvGenrePagingSource(
    private val api: TmdbApi,
    private val genreId: Int,
    private val filters: DiscoverFilters = DiscoverFilters(),
    private val totalResults: MutableStateFlow<Int?>? = null
) : PagingSource<Int, Movie>() {

    // Same defensive guard as SearchMoviesPagingSource — TMDB's
    // `/discover/tv` page boundaries aren't guaranteed disjoint when results tie on the sort key
    // (popularity), so the same show can come back on more than one page. The genre grid keys
    // every cell with itemKey{ it.id }, which requires every key in the whole list to be unique —
    // a returning id crashes with `IllegalArgumentException: Key "<id>" was already used` once the
    // user scrolls far enough for the repeat to load. One `seenTvIds` set per PagingSource instance
    // (fresh instance per genre/refresh) drops any repeat on a later page. Paging 3's `load()`
    // calls are always sequential for one PagingSource, so this plain mutable set needs no
    // synchronization.
    private val seenTvIds = mutableSetOf<Int>()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> {
        val page = params.key ?: 1
        return try {
            val response = api.discoverTvByGenres(
                genreId.toString(),
                year = filters.year,
                language = filters.language,
                minRating = filters.minRating,
                page = page
            )
            totalResults?.value = response.totalResults
            LoadResult.Page(
                data = response.results
                    .map { it.toMovie() }
                    .filter { seenTvIds.add(it.id) },
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
