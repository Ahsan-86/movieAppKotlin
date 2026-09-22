package com.ahsan.movieapp.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ahsan.movieapp.R
import com.ahsan.movieapp.domain.model.MediaType
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.ui.components.EmptyState
import com.ahsan.movieapp.ui.components.MoviePosterCard
import kotlinx.coroutines.flow.Flow

/**
 * The Favorites tab. Session 6 — now a Movies / TV Shows segmented-toggle screen (same control as
 * Genre/Person): each tab shows that media type's saved favorites as a poster grid, newest-added
 * first. A TV card routes to the real TV detail screen via [onTvClick]; a movie card goes through
 * [onMovieClick]. Empty states are per-tab since the two are independent lists now.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onMovieClick: (Movie) -> Unit,
    // Session 6 — TV favorites route to the real TV detail screen (the item's [MediaType] decides
    // which callback a card fires).
    onTvClick: (Movie) -> Unit,
    // Fires when the user re-taps the already-selected Favorites tab — scrolls back to top
    // instead of doing nothing, matching Instagram/YouTube-style tab-reselect behavior.
    scrollToTopEvents: Flow<Unit>? = null,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val favoriteMovies by viewModel.favoriteMovies.collectAsState()
    val favoriteTvShows by viewModel.favoriteTvShows.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val gridState = rememberLazyGridState()

    val shownMovies = if (selectedTab == FavoritesTab.MOVIES) favoriteMovies else favoriteTvShows
    val isTvTab = selectedTab == FavoritesTab.TV

    LaunchedEffect(scrollToTopEvents) {
        scrollToTopEvents?.collect { gridState.animateScrollToItem(0) }
    }
    // Switching tab shows a different list — snap back to the top so the user never lands in the
    // middle of the other tab's grid.
    LaunchedEffect(selectedTab) {
        gridState.scrollToItem(0)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            SegmentedButton(
                selected = !isTvTab,
                onClick = { viewModel.onTabSelected(FavoritesTab.MOVIES) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text(stringResource(R.string.movies)) }
            SegmentedButton(
                selected = isTvTab,
                onClick = { viewModel.onTabSelected(FavoritesTab.TV) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text(stringResource(R.string.tv_shows)) }
        }

        if (shownMovies.isEmpty()) {
            EmptyState(
                title = stringResource(
                    if (isTvTab) R.string.favorites_empty_tv_title else R.string.favorites_empty_title
                ),
                body = stringResource(
                    if (isTvTab) R.string.favorites_empty_tv_body else R.string.favorites_empty_body
                )
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 128.dp),
                state = gridState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(shownMovies, key = { it.id }) { movie ->
                    MoviePosterCard(
                        movie = movie,
                        onClick = {
                            if (movie.mediaType == MediaType.TV) {
                                onTvClick(movie)
                            } else {
                                onMovieClick(movie)
                            }
                        },
                        onToggleFavorite = { viewModel.toggleFavorite(movie) }
                    )
                }
            }
        }
    }
}