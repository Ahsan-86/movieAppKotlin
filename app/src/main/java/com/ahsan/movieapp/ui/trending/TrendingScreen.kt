package com.ahsan.movieapp.ui.trending

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.ui.components.FullScreenError
import com.ahsan.movieapp.ui.components.FullScreenLoading
import com.ahsan.movieapp.ui.components.MoviePosterCard
import kotlinx.coroutines.flow.Flow

@Composable
fun TrendingScreen(
    onMovieClick: (Movie) -> Unit,
    // Fires when the user re-taps the already-selected Trending tab — scrolls back to top
    // instead of doing nothing, matching Instagram/YouTube-style tab-reselect behavior.
    scrollToTopEvents: Flow<Unit>? = null,
    viewModel: TrendingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()

    LaunchedEffect(scrollToTopEvents) {
        scrollToTopEvents?.collect { gridState.animateScrollToItem(0) }
    }

    // The top bar now shows "Trending" as its title (see MovieNavGraph.MovieTopBar), so this
    // screen no longer needs its own redundant heading below it.
    Column(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading -> FullScreenLoading()
            // Nothing cached and the refresh failed — used to fall through to an empty grid with
            // no explanation and no way to try again.
            state.movies.isEmpty() && state.errorMessage != null -> FullScreenError(
                message = state.errorMessage ?: "Couldn't load trending movies",
                onRetry = viewModel::retry
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 128.dp),
                state = gridState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(state.movies, key = { it.id }) { movie ->
                    MoviePosterCard(
                        movie = movie,
                        onClick = { onMovieClick(movie) },
                        onToggleFavorite = { viewModel.toggleFavorite(movie) },
                        width = 128.dp
                    )
                }
            }
        }
    }
}
