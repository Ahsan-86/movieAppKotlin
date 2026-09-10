package com.ahsan.movieapp.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.ahsan.movieapp.data.local.dao.FavoriteDao
import com.ahsan.movieapp.data.local.dao.MovieDao
import com.ahsan.movieapp.data.mapper.toDomain
import com.ahsan.movieapp.data.mapper.toEntity
import com.ahsan.movieapp.data.mapper.toMovieDto
import com.ahsan.movieapp.data.remote.TmdbApi
import com.ahsan.movieapp.domain.model.Movie
import kotlinx.coroutines.flow.first

/**
 * Phase 4 (pagination) Round 4 — the search screen's movie results. Network-only, straight over
 * [TmdbApi.searchMulti] filtered to `media_type == "movie"`; people are a separate one-shot fetch
 * (see [com.ahsan.movieapp.data.repository.MovieRepository.searchPeople]) rather than part of this
 * pager — `/search/multi`'s pages mix movies and people together, but only the first handful of
 * people a query returns are ever shown, so there's nothing worth paginating there.
 *
 * Every returned movie still gets upserted into the shared `movies` table, same convention as
 * [DiscoverPagingSource] and every other network fetch in this app. Favorite status is resolved
 * with a one-time snapshot per `load()` call (`favoriteDao.observeFavoriteMovies().first()`), the
 * same pattern already used by `getCollectionDetails()`/`getPersonCredits()` in
 * `MovieRepositoryImpl` — NOT a live `combine()` against this pager's output. An earlier version of
 * this class always returned `isFavorite = false` and left
 * [com.ahsan.movieapp.ui.search.SearchViewModel] to `combine()` a live favorites flow over the raw
 * [androidx.paging.PagingData] before `cachedIn()`; that crashed with `IllegalStateException:
 * Attempt to collect twice from pageEventFlow`, because `combine()` re-invokes `.map{}` on the same
 * underlying `PagingData` every time the side flow emits (not just when the paging flow itself
 * emits), producing two overlapping generations that both try to collect the same page-event flow
 * at once — Paging 3 explicitly disallows that. The trade-off of the snapshot approach: toggling a
 * favorite from Search/Discover results doesn't flip the heart icon live the way Trending/Genre's
 * Room-`LEFT JOIN`-based reactivity does — it's correct again next time this screen re-queries.
 *
 * Falls back to a local `LIKE` match, but ONLY on page 1 and ONLY when the network call fails
 * outright — the same "search still half-works offline" behavior this app has always had — and
 * returns it as a single, non-paginated page (`endOfPaginationReached` implied by a null
 * [LoadResult.Page.nextKey]) since a bounded local match has no real "next page." A page-2+ network
 * failure is a normal Paging 3 append error instead (retry button), not a silent switch to local
 * data mid-scroll.
 */
class SearchMoviesPagingSource(
    private val api: TmdbApi,
    private val movieDao: MovieDao,
    private val favoriteDao: FavoriteDao,
    private val query: String
) : PagingSource<Int, Movie>() {

    // TMDB's `/search/multi` pages aren't guaranteed disjoint for a given query — the same movie can
    // come back on more than one page (seen 2026-09-11: scrolling "spider" past page 4-5 crashed with
    // `IllegalArgumentException: Key "640258" was already used`, since LazyGrid's itemKey{it.id}
    // requires every key in the whole list to be unique). One `seenMovieIds` set per PagingSource
    // instance (a fresh instance per query/refresh, so this always starts empty) drops any repeat the
    // API hands back on a later page. Paging 3's `load()` calls are always sequential for one
    // PagingSource, never concurrent, so this plain mutable set needs no synchronization.
    private val seenMovieIds = mutableSetOf<Int>()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> {
        val page = params.key ?: 1
        return try {
            val response = api.searchMulti(query, page = page)
            val movieDtos = response.results
                .filter { it.mediaType == "movie" }
                .map { it.toMovieDto() }
                .filter { seenMovieIds.add(it.id) }
            val now = System.currentTimeMillis()
            if (movieDtos.isNotEmpty()) {
                movieDao.upsertMovies(movieDtos.map { it.toEntity(now) })
            }
            val favoriteIds = favoriteDao.observeFavoriteMovies().first().map { it.id }.toSet()
            LoadResult.Page(
                data = movieDtos.map { it.toEntity(now).toDomain(isFavorite = it.id in favoriteIds) },
                prevKey = null,
                nextKey = (page + 1).takeIf { page < response.totalPages }
            )
        } catch (throwable: Throwable) {
            if (page == 1) {
                val localMatches = runCatching {
                    movieDao.searchLocalMovies("%$query%", LOCAL_SEARCH_LIMIT)
                }.getOrDefault(emptyList())
                if (localMatches.isNotEmpty()) {
                    val favoriteIds = runCatching {
                        favoriteDao.observeFavoriteMovies().first().map { it.id }.toSet()
                    }.getOrDefault(emptySet())
                    return LoadResult.Page(
                        data = localMatches.map { it.toDomain(isFavorite = it.id in favoriteIds) },
                        prevKey = null,
                        nextKey = null
                    )
                }
            }
            LoadResult.Error(throwable)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Movie>): Int? =
        state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }

    companion object {
        private const val LOCAL_SEARCH_LIMIT = 25
    }
}
