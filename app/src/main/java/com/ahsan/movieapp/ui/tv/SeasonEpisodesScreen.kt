package com.ahsan.movieapp.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ahsan.movieapp.domain.model.Episode
import com.ahsan.movieapp.ui.components.EmptyState
import com.ahsan.movieapp.ui.components.FullScreenError
import com.ahsan.movieapp.ui.components.FullScreenLoading

/**
 * Phase 2.6 Session 2 — one season's full episode list (image + description per episode), opened
 * by tapping a season on [TvDetailScreen]'s Seasons section. Purely informational: episode rows
 * aren't clickable — see [SeasonEpisodesViewModel]'s doc for why. Same full-screen
 * Scaffold+dynamic-TopAppBar pattern as every other detail-type screen in this app. Its
 * [LazyColumn] is this screen's ONLY scrolling container (unlike TvDetailScreen's outer
 * `.verticalScroll()` Column) — per the project's lazy-inside-scrolling-parent rule, a episode
 * list belongs in a real `LazyColumn`, not nested inside another scrollable Column.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonEpisodesScreen(
    onBack: () -> Unit,
    viewModel: SeasonEpisodesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.seasonName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isLoading -> FullScreenLoading()
                state.episodes.isEmpty() && state.errorMessage != null ->
                    FullScreenError(message = state.errorMessage ?: "Couldn't load episodes")
                state.episodes.isEmpty() ->
                    EmptyState(title = "No episodes yet", body = "TMDB doesn't have episode data for this season yet.")
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.episodes, key = { it.episodeNumber }) { episode ->
                        EpisodeRow(episode = episode)
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeRow(episode: Episode) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = episode.imageUrl,
                contentDescription = episode.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            text = "${episode.episodeNumber}. ${episode.name}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        episode.airDateFormatted?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        if (episode.overview.isNotBlank()) {
            Text(
                text = episode.overview,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
