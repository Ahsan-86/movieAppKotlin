package com.ahsan.movieapp.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.ui.components.MovieCarouselSection
import com.ahsan.movieapp.ui.components.OfflineBanner

/**
 * The new "Explore" home: a stack of themed, horizontally-scrolling carousels (Trending,
 * Popular, For You, Now Playing, Top Rated, Upcoming) — replacing the old app's single
 * vertical list of popular movies.
 */
@Composable
fun HomeScreen(
    onMovieClick: (Movie) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        OfflineBanner(visible = state.isOffline)

        Text(
            text = "Explore",
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(state.sections, key = { it.title }) { section ->
                MovieCarouselSection(
                    title = section.title,
                    movies = section.movies,
                    isLoading = section.isLoading,
                    onMovieClick = onMovieClick,
                    onToggleFavorite = { viewModel.toggleFavorite(it) }
                )
            }
        }
    }
}
