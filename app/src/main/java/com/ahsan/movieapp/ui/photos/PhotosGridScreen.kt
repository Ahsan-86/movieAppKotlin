package com.ahsan.movieapp.ui.photos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ahsan.movieapp.R
import com.ahsan.movieapp.ui.components.EmptyState
import com.ahsan.movieapp.ui.components.FullScreenError
import com.ahsan.movieapp.ui.components.ShimmerAsyncImage
import com.ahsan.movieapp.ui.components.ShimmerBox

/**
 * Phase 5 — the "See all" photos grid opened from a detail screen's Photos row: every still
 * (up to the repository's cap) as a grid of 16:9 thumbnails; tapping one opens the fullscreen
 * viewer at that photo. Own Scaffold + TopAppBar + back button, not a root tab (same pattern as
 * CollectionScreen / CastCrewListScreen).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotosGridScreen(
    onBack: () -> Unit,
    onPhotoClick: (index: Int) -> Unit,
    viewModel: PhotosViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (viewModel.isTv) R.string.photos_tv_gallery else R.string.photos_movie_gallery)) },
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
                state.isLoading -> PhotosGridSkeleton()
                state.photos.isEmpty() && state.errorMessage != null -> FullScreenError(
                    message = state.errorMessage ?: stringResource(R.string.photos_couldnt_load),
                    onRetry = viewModel::retry
                )
                state.photos.isEmpty() -> EmptyState(
                    title = stringResource(R.string.photos_empty_title),
                    body = stringResource(R.string.photos_empty_body)
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(state.photos) { index, photo ->
                        Box(
                            modifier = Modifier
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
            }
        }
    }
}

/** Shimmer twin of the grid — a screenful of 16:9 blocks. */
@Composable
private fun PhotosGridSkeleton() {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        userScrollEnabled = false,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(10) {
            ShimmerBox(modifier = Modifier.aspectRatio(16f / 9f), shape = RoundedCornerShape(12.dp))
        }
    }
}
