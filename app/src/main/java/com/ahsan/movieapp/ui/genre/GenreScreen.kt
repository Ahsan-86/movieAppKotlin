package com.ahsan.movieapp.ui.genre

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.ui.components.EmptyState
import com.ahsan.movieapp.ui.components.FullScreenError
import com.ahsan.movieapp.ui.components.FullScreenLoading
import com.ahsan.movieapp.ui.components.MoviePosterCard
import com.ahsan.movieapp.ui.person.MediaTab

/**
 * Full-screen "browse a genre" view opened from the search screen's genre chips. Shows a
 * Movies/TV segmented toggle only when the genre actually has both (e.g. "Horror" is
 * movie-only) — same pattern as the person screen's Movies/TV toggle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreScreen(
    onBack: () -> Unit,
    onMovieClick: (Movie) -> Unit,
    viewModel: GenreViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

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

            val items = state.displayedItems
            val isTv = state.selectedMediaType == MediaTab.TV

            // Boxed in its own weighted slot rather than left as a bare Column sibling — each of
            // these states fills its container, and without a bounded slot to fill that would
            // fight the segmented row above it for space instead of taking just what's left.
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.isLoading -> FullScreenLoading()
                    items.isEmpty() && state.errorMessage != null -> FullScreenError(message = state.errorMessage ?: "Something went wrong")
                    items.isEmpty() -> EmptyState(
                        title = "Nothing here",
                        body = "No ${if (isTv) "TV shows" else "movies"} found for ${state.genreName}."
                    )
                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 128.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(items, key = { it.id }) { movie ->
                            MoviePosterCard(
                                movie = movie,
                                onClick = { onMovieClick(movie) },
                                // TV shows can't be favorited yet — see MovieRepositoryImpl.browseGenreTv.
                                onToggleFavorite = if (isTv) null else { { viewModel.toggleFavorite(movie) } },
                                width = null
                            )
                        }
                    }
                }
            }
        }
    }
}
