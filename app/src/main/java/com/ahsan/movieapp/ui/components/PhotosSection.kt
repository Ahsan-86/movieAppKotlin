package com.ahsan.movieapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ahsan.movieapp.R
import com.ahsan.movieapp.domain.model.Photo

/**
 * What a detail screen's Photos row is showing. [None] covers both "TMDB has no photos" and "the
 * fetch failed" — like cast and trailer, the gallery is supplementary, so a failure just means the
 * row doesn't render rather than blocking or erroring the screen.
 */
sealed interface PhotosState {
    data object Loading : PhotosState
    data class Loaded(val photos: List<Photo>) : PhotosState
    data object None : PhotosState
}

/**
 * Phase 5's IMDb-style Photos row for the Movie and TV detail screens: a heading with a "see all"
 * arrow (same pattern as Cast & Crew), then a horizontal strip of the first [ROW_LIMIT] stills.
 * Tapping a still opens the fullscreen viewer at that photo ([onPhotoClick] gets its index); the arrow
 * opens the full grid ([onSeeAll]). While the separate, lazy fetch is in flight the row holds its
 * place with shimmer thumbnails, so the main detail content is never delayed by it.
 */
@Composable
fun PhotosSection(
    state: PhotosState,
    title: String,
    onPhotoClick: (index: Int) -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state is PhotosState.None) return

    Column(modifier = modifier.padding(top = 8.dp, bottom = 16.dp)) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            if (state is PhotosState.Loaded) {
                IconButton(onClick = onSeeAll) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.photos_see_all))
                }
            }
        }

        if (state is PhotosState.Loaded) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(state.photos.take(ROW_LIMIT)) { index, photo ->
                    Box(
                        modifier = Modifier
                            .width(THUMB_WIDTH)
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPhotoClick(index) }
                    ) {
                        ShimmerAsyncImage(
                            model = photo.thumbUrl,
                            contentDescription = stringResource(R.string.photos_photo_description, index + 1, state.photos.size),
                            modifier = Modifier.matchParentSize()
                        )
                    }
                }
            }
        } else {
            PhotosRowSkeleton(modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** Shimmer twin of the Photos row — three 16:9 blocks where the thumbnails will appear. */
@Composable
private fun PhotosRowSkeleton(modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier,
        userScrollEnabled = false,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(3) {
            ShimmerBox(
                modifier = Modifier.width(THUMB_WIDTH).aspectRatio(16f / 9f),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

private val THUMB_WIDTH = 240.dp
private const val ROW_LIMIT = 10
