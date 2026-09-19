package com.ahsan.movieapp.ui.genre

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.ui.components.EmptyState
import com.ahsan.movieapp.ui.components.FullScreenError
import com.ahsan.movieapp.ui.components.FullScreenLoading
import com.ahsan.movieapp.ui.components.MoviePosterCard
import com.ahsan.movieapp.ui.components.PagingAppendFooter
import com.ahsan.movieapp.ui.person.MediaTab

/**
 * Full-screen "browse a genre" view opened from the search screen's genre chips. Shows a
 * Movies/TV segmented toggle only when the genre actually has both (e.g. "Horror" is
 * movie-only) — same pattern as the person screen's Movies/TV toggle. Both tabs are paginated,
 * infinite-scroll (Phase 4 Round 2 for Movies, Round 3 for TV) — see [GenreViewModel]'s class doc
 * for why they're backed by two different mechanisms under the hood. Tapping a TV item routes to
 * the real TV detail screen via [onTvClick] (the toast that used to live here was retired on
 * 2026-09-22 now that the TV detail screen exists).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreScreen(
    onBack: () -> Unit,
    onMovieClick: (Movie) -> Unit,
    onTvClick: (Movie) -> Unit,
    viewModel: GenreViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    // Each is only non-null (and only ever collected) when this chip actually has that media
    // genre — a TV-only chip's pagedMovies is null and a movie-only chip's pagedTvShows is null,
    // so there's simply nothing to page for the tab that doesn't apply.
    val pagedMovies = viewModel.pagedMovies?.collectAsLazyPagingItems()
    val pagedTvShows = viewModel.pagedTvShows?.collectAsLazyPagingItems()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.genreName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (state.showMoviesTab && state.showTvTab) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    SegmentedButton(
                        selected = state.selectedMediaType == MediaTab.MOVIES,
                        onClick = { viewModel.onMediaTabSelected(MediaTab.MOVIES) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("Movies") }
                    SegmentedButton(
                        selected = state.selectedMediaType == MediaTab.TV,
                        onClick = { viewModel.onMediaTabSelected(MediaTab.TV) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("TV Shows") }
                }
            }

            val isTv = state.selectedMediaType == MediaTab.TV

            // Boxed in its own weighted slot rather than left as a bare Column sibling — each of
            // these states fills its container, and without a bounded slot to fill that would
            // fight the segmented row above it for space instead of taking just what's left.
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (isTv && pagedTvShows != null) {
                    GenrePagedGrid(
                        pagingItems = pagedTvShows,
                        emptyBody = "No TV shows found for ${state.genreName}.",
                        onItemClick = onTvClick,
                        // TV shows can't be favorited yet — no schema support for TV favorites.
                        onToggleFavorite = null
                    )
                } else if (!isTv && pagedMovies != null) {
                    GenrePagedGrid(
                        pagingItems = pagedMovies,
                        emptyBody = "No movies found for ${state.genreName}.",
                        onItemClick = onMovieClick,
                        onToggleFavorite = { movie -> viewModel.toggleFavorite(movie) }
                    )
                }
            }
        }
    }
}

/** Shared grid for both of [GenreScreen]'s tabs — same loading/error/empty/append handling,
 *  paging library, and layout either way, just a different [LazyPagingItems] source, empty-state
 *  message, click handler, and favoriting behavior. */
@Composable
private fun GenrePagedGrid(
    pagingItems: LazyPagingItems<Movie>,
    emptyBody: String,
    onItemClick: (Movie) -> Unit,
    onToggleFavorite: ((Movie) -> Unit)?
) {
    val refreshState = pagingItems.loadState.refresh
    when {
        refreshState is LoadState.Loading && pagingItems.itemCount == 0 -> FullScreenLoading()
        refreshState is LoadState.Error && pagingItems.itemCount == 0 -> FullScreenError(
            message = refreshState.error.message ?: "Something went wrong",
            onRetry = { pagingItems.retry() }
        )
        pagingItems.itemCount == 0 -> EmptyState(title = "Nothing here", body = emptyBody)
        else -> LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 128.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(count = pagingItems.itemCount, key = pagingItems.itemKey { it.id }) { index ->
                val movie = pagingItems[index] ?: return@items
                MoviePosterCard(
                    movie = movie,
                    onClick = { onItemClick(movie) },
                    onToggleFavorite = onToggleFavorite?.let { toggle -> { toggle(movie) } },
                    width = null
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                PagingAppendFooter(pagingItems = pagingItems)
            }
        }
    }
}
