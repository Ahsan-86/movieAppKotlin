package com.ahsan.movieapp.ui.favorites

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.ahsan.movieapp.ui.components.MovieListRow
import kotlinx.coroutines.flow.Flow

/**
 * The Favorites tab. Session 6 — now a Movies / TV Shows segmented-toggle screen (same control as
 * Genre/Person): each tab shows that media type's saved favorites. On Ahsan's request (Session 7
 * working-tree round) the two tabs render as full-width list rows via [MovieListRow] — the same
 * list row the search screen's list-view mode and the person filmography use — instead of a poster
 * grid. A TV row routes to the real TV detail screen via [onTvClick]; a movie row goes through
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
    val listState = rememberLazyListState()

    val shownMovies = if (selectedTab == FavoritesTab.MOVIES) favoriteMovies else favoriteTvShows
    val isTvTab = selectedTab == FavoritesTab.TV

    LaunchedEffect(scrollToTopEvents) {
        scrollToTopEvents?.collect { listState.animateScrollToItem(0) }
    }
    // Switching tab shows a different list — snap back to the top so the user never lands in the
    // middle of the other tab's rows.
    LaunchedEffect(selectedTab) {
        listState.scrollToItem(0)
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
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(shownMovies, key = { it.id to it.mediaType }) { movie ->
                    MovieListRow(
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