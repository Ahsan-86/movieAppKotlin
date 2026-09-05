package com.ahsan.movieapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ahsan.movieapp.domain.model.Movie

/**
 * A titled, horizontally-scrolling row of posters — the building block of the new multi-section
 * Explore screen (Trending / Popular / Top Rated / For You / …), replacing the old single list.
 */
@Composable
fun MovieCarouselSection(
    title: String,
    movies: List<Movie>,
    onMovieClick: (Movie) -> Unit,
    onToggleFavorite: ((Movie) -> Unit)? = null,
    onSeeAll: (() -> Unit)? = null,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SectionHeader(title = title, onSeeAll = onSeeAll, modifier = Modifier.padding(horizontal = 16.dp))

        Spacer4()

        if (isLoading && movies.isEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(6) { PosterSkeleton() }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(movies, key = { it.id }) { movie ->
                    MoviePosterCard(
                        movie = movie,
                        onClick = { onMovieClick(movie) },
                        onToggleFavorite = onToggleFavorite?.let { { it(movie) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun Spacer4() {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(10.dp))
}

@Composable
private fun PosterSkeleton() {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .width(128.dp)
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

@Composable
fun SectionHeader(
    title: String,
    onSeeAll: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        if (onSeeAll != null) {
            TextButton(onClick = onSeeAll) {
                Text(text = "See all", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
