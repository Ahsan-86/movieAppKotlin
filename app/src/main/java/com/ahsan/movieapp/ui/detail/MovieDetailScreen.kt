package com.ahsan.movieapp.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ahsan.movieapp.ui.components.FullScreenError
import com.ahsan.movieapp.ui.components.FullScreenLoading

/**
 * Basic detail screen for Phase 1 (backdrop, poster, key facts, favorite toggle, overview).
 * Phase 3 overhauls this into the full cascaded design: cast list with clickable filmography
 * drill-down and a "More Like This" row.
 */
@Composable
fun MovieDetailScreen(
    onBack: () -> Unit,
    viewModel: MovieDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading -> FullScreenLoading()
            state.details == null -> FullScreenError(message = state.errorMessage ?: "Couldn't load this movie")
            else -> {
                val details = state.details!!
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Box {
                        AsyncImage(
                            model = details.backdropUrl ?: details.posterUrl,
                            contentDescription = details.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 10f)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .align(Alignment.BottomStart)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background)
                                    )
                                )
                        )
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.4f), androidx.compose.foundation.shape.CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    }

                    Row(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().offsetUp()) {
                        AsyncImage(
                            model = details.posterUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(110.dp)
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(12.dp))
                        )

                        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                            Text(text = details.title, style = MaterialTheme.typography.headlineMedium)
                            if (!details.tagline.isNullOrBlank()) {
                                Text(
                                    text = "“${details.tagline}”",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                modifier = Modifier.padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MetaChip(icon = Icons.Filled.Star, text = details.ratingOutOfTen)
                                Text(text = details.releaseYear, style = MaterialTheme.typography.labelLarge)
                                details.runtimeFormatted?.let {
                                    Text(text = it, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                            if (details.genres.isNotEmpty()) {
                                Text(
                                    text = details.genres.joinToString(" • "),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Overview", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = details.overview.ifBlank { "No description available." },
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                FloatingActionButton(
                    onClick = { viewModel.toggleFavorite() },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .padding(20.dp)
                        .align(Alignment.BottomEnd)
                ) {
                    Icon(
                        imageVector = if (details.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.height(16.dp))
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

private fun Modifier.offsetUp(): Modifier = this.offset(y = (-40).dp)
