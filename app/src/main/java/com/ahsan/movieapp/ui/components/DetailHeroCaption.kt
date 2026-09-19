package com.ahsan.movieapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The title/meta caption overlaid on the bottom of a detail screen's hero image — the redesign
 * Ahsan asked for on 2026-09-20 and refined on 2026-09-22: Movie and TV details no longer put
 * title/tagline/rating/year/genres in a column beside an overlapping poster; instead each screen's
 * hero is now a big portrait poster (PersonHero-style) with the details sunk into its bottom edge
 * behind the same stacked scrims PersonHero uses (radial vignette around the edges + a taller
 * bottom gradient behind the caption text). The 2:3 poster *is* the hero image the caller passes
 * in, and the Overview then sits directly below it. [metaLabels] plugs in the media-specific labels
 * (movie runtime vs TV seasons + episode runtime) while everything else stays shared between
 * [com.ahsan.movieapp.ui.detail.MovieDetailScreen] and
 * [com.ahsan.movieapp.ui.tv.TvDetailScreen].
 */
@Composable
fun DetailHeroCaption(
    title: String,
    tagline: String?,
    rating: String,
    metaLabels: List<String>,
    genres: List<String>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Vignette: same soft radial falloff PersonHero uses, darkening corners/edges all around
        // so the caption and the transparent bar's white icons stay legible without a hard border.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.32f))
                    )
                )
        )
        // Caption scrim: the same taller, stronger bottom gradient PersonHero uses, so the caption
        // stays legible over any poster.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .align(Alignment.BottomStart)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (!tagline.isNullOrBlank()) {
                Text(
                    text = "“$tagline”",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFC857),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = rating,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
                metaLabels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
            if (genres.isNotEmpty()) {
                Text(
                    text = genres.joinToString(" • "),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}