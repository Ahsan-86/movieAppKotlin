package com.ahsan.movieapp.ui.photos

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ahsan.movieapp.R
import com.ahsan.movieapp.domain.model.Photo
import com.ahsan.movieapp.ui.components.ShimmerAsyncImage
import com.ahsan.movieapp.ui.components.ShimmerBox

/**
 * Phase 5 — the fullscreen photo viewer: swipe left/right between stills, pinch to zoom in and out,
 * double-tap to toggle zoom, with an "n / total" counter. It is its own nav destination (not a
 * Dialog), like the trailer screen. No share or save, by design.
 *
 * Gestures: a single finger on an un-zoomed photo is left alone so the pager swipes; a second finger
 * (pinch) or a zoomed-in photo is handled here instead — panning inside the zoomed image — and while
 * the current photo is zoomed the pager's own swiping is switched off so the two never fight.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoViewerScreen(
    onBack: () -> Unit,
    viewModel: PhotosViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when {
            state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ShimmerBox(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f), shape = RoundedCornerShape(0.dp))
            }
            state.photos.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(stringResource(R.string.photos_couldnt_load), color = Color.White)
                if (state.errorMessage != null) {
                    TextButton(onClick = viewModel::retry) { Text(stringResource(R.string.retry)) }
                }
            }
            else -> {
                val photos = state.photos
                val pagerState = rememberPagerState(
                    initialPage = viewModel.startIndex.coerceIn(0, photos.lastIndex),
                    pageCount = { photos.size }
                )
                var isZoomed by remember { mutableStateOf(false) }

                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = !isZoomed,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    ZoomablePhoto(
                        photo = photos[page],
                        contentDescription = stringResource(R.string.photos_photo_description, page + 1, photos.size),
                        onZoomedChange = { zoomed -> if (page == pagerState.currentPage) isZoomed = zoomed }
                    )
                }

                // Counter + back, floating over the photo on a soft scrim so they stay legible.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)))
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                    }
                    Text(
                        text = stringResource(R.string.photos_counter, pagerState.currentPage + 1, photos.size),
                        color = Color.White,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }

        // While loading / on error there is no pager row above, so keep a back button reachable.
        if (state.isLoading || state.photos.isEmpty()) {
            IconButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
            }
        }
    }
}

/**
 * One zoomable photo page. scale runs 1-MAX_SCALE; offset is the pan in pixels, clamped so the
 * zoomed image can never be dragged past its own edges. Pinch zooms about the gesture, double-tap
 * toggles between fit and [DOUBLE_TAP_SCALE] centred on the tapped point.
 */
@Composable
private fun ZoomablePhoto(
    photo: Photo,
    contentDescription: String,
    onZoomedChange: (Boolean) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    fun clampOffset(candidate: Offset, forScale: Float): Offset {
        val maxX = (forScale - 1f) * size.width / 2f
        val maxY = (forScale - 1f) * size.height / 2f
        return Offset(candidate.x.coerceIn(-maxX, maxX), candidate.y.coerceIn(-maxY, maxY))
    }

    LaunchedEffect(scale > 1f) { onZoomedChange(scale > 1f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { tap ->
                    if (scale > 1.01f) {
                        scale = 1f
                        offset = Offset.Zero
                    } else {
                        val target = DOUBLE_TAP_SCALE
                        val fromCenter = tap - Offset(size.width / 2f, size.height / 2f)
                        scale = target
                        // Keep the tapped point under the finger: translate by (1 - scale) * its offset from centre.
                        offset = clampOffset(fromCenter * (1f - target), target)
                    }
                })
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val fingers = event.changes.count { it.pressed }
                        // One finger on an un-zoomed photo belongs to the pager; anything else is ours.
                        if (fingers >= 2 || scale > 1f) {
                            val newScale = (scale * event.calculateZoom()).coerceIn(1f, MAX_SCALE)
                            val newOffset = if (newScale <= 1f) Offset.Zero else offset + event.calculatePan()
                            scale = newScale
                            offset = clampOffset(newOffset, newScale)
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        ShimmerAsyncImage(
            model = photo.fullUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )
    }
}

private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 2.5f
