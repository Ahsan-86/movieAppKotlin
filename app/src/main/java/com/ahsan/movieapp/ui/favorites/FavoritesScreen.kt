package com.ahsan.movieapp.ui.favorites

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
import com.ahsan.movieapp.ui.components.EmptyState
import com.ahsan.movieapp.ui.components.MoviePosterCard
import kotlinx.coroutines.flow.Flow

@Composable
fun FavoritesScreen(
    onMovieClick: (Movie) -> Unit,
    // Fires when the user re-taps the already-selected Favorites tab — scrolls back to top
    // instead of doing nothing, matching Instagram/YouTube-style tab-reselect behavior.
    scrollToTopEvents: Flow<Unit>? = null,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val favorites by viewModel.favorites.collectAsState()
    val gridState = rememberLazyGridState()

    LaunchedEffect(scrollToTopEvents) {
        scrollToTopEvents?.collect { gridState.animateScrollToItem(0) }
    }

    // The top bar now shows "Favorites" as its title (see MovieNavGraph.MovieTopBar), so this
    // screen no longer needs its own redundant heading below it.
    Column(modifier = Modifier.fillMaxSize()) {
        if (favorites.isEmpty()) {
            EmptyState(
                title = "No favorites yet",
                body = "Tap the heart on any movie to save it here — even offline."
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 128.dp),
                state = gridState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(favorites, key = { it.id }) { movie ->
                    MoviePosterCard(
                        movie = movie,
                        onClick = { onMovieClick(movie) },
                        onToggleFavorite = { viewModel.toggleFavorite(movie) }
                    )
                }
            }
        }
    }
}
