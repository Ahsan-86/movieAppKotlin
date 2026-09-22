package com.ahsan.movieapp.ui.detail

import com.ahsan.movieapp.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.ui.components.EmptyState
import com.ahsan.movieapp.ui.components.FullScreenError
import com.ahsan.movieapp.ui.components.FullScreenLoading
import com.ahsan.movieapp.ui.components.MoviePosterCard

/**
 * Phase 3 Round B's collection teaser destination — every movie in a franchise (e.g. a trilogy),
 * opened by tapping the Detail screen's compact "part of a collection" row. Same full-screen
 * pattern as GenreScreen/CastCrewListScreen: own Scaffold + TopAppBar + real back button, not a
 * root-level tab (not in MovieNavGraph's TOP_LEVEL_ROUTES). Each poster is clickable through to
 * its own Detail screen, same as every other "tap a poster" spot in the app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    onBack: () -> Unit,
    onMovieClick: (Movie) -> Unit,
    viewModel: CollectionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isLoading -> FullScreenLoading()
                state.movies.isEmpty() && state.errorMessage != null ->
                    FullScreenError(message = state.errorMessage ?: stringResource(R.string.collection_couldnt_load))
                state.movies.isEmpty() -> EmptyState(title = stringResource(R.string.nothing_here), body = stringResource(R.string.collection_empty_body))
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 128.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (!state.overview.isNullOrBlank()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = state.overview.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            )
                        }
                    }
                    items(state.movies, key = { it.id }) { movie ->
                        MoviePosterCard(
                            movie = movie,
                            onClick = { onMovieClick(movie) },
                            onToggleFavorite = { viewModel.toggleFavorite(movie) },
                            width = null
                        )
                    }
                }
            }
        }
    }
}
