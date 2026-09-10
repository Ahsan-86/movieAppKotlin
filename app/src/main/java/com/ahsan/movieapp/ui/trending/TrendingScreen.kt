package com.ahsan.movieapp.ui.trending

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.ui.components.FullScreenError
import com.ahsan.movieapp.ui.components.FullScreenLoading
import com.ahsan.movieapp.ui.components.MoviePosterCard
import com.ahsan.movieapp.ui.components.PagingAppendFooter
import kotlinx.coroutines.flow.Flow

/**
 * Phase 4 (pagination) Round 1 — was a single-page [LazyVerticalGrid] over a plain
 * `List<Movie>`; now backed by [androidx.paging.compose.LazyPagingItems] so scrolling near the
 * bottom transparently loads the next TMDB page (see [TrendingViewModel.pagedMovies]/
 * [com.ahsan.movieapp.data.paging.CategoryRemoteMediator]). The full-screen loading/error states
 * below are driven by `loadState.refresh` (first page); [PagingAppendFooter] handles "loading/
 * failed the *next* page" separately, at the bottom of an already-populated grid.
 */
@Composable
fun TrendingScreen(
    onMovieClick: (Movie) -> Unit,
    // Fires when the user re-taps the already-selected Trending tab — scrolls back to top
    // instead of doing nothing, matching Instagram/YouTube-style tab-reselect behavior.
    scrollToTopEvents: Flow<Unit>? = null,
    viewModel: TrendingViewModel = hiltViewModel()
) {
    val movies = viewModel.pagedMovies.collectAsLazyPagingItems()
    val gridState = rememberLazyGridState()

    LaunchedEffect(scrollToTopEvents) {
        scrollToTopEvents?.collect { gridState.animateScrollToItem(0) }
    }

    // The top bar now shows "Trending" as its title (see MovieNavGraph.MovieTopBar), so this
    // screen no longer needs its own redundant heading below it.
    Column(modifier = Modifier.fillMaxSize()) {
        val refreshState = movies.loadState.refresh
        when {
            refreshState is LoadState.Loading && movies.itemCount == 0 -> FullScreenLoading()
            // Nothing cached and the refresh failed — used to fall through to an empty grid with
            // no explanation and no way to try again.
            refreshState is LoadState.Error && movies.itemCount == 0 -> FullScreenError(
                message = refreshState.error.message ?: "Couldn't load trending movies",
                onRetry = { movies.retry() }
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 128.dp),
                state = gridState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(count = movies.itemCount, key = movies.itemKey { it.id }) { index ->
                    val movie = movies[index] ?: return@items
                    MoviePosterCard(
                        movie = movie,
                        onClick = { onMovieClick(movie) },
                        onToggleFavorite = { viewModel.toggleFavorite(movie) },
                        width = 128.dp
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    PagingAppendFooter(pagingItems = movies)
                }
            }
        }
    }
}
