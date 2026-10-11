package com.ahsan.movieapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.annotation.ExperimentalCoilApi
import coil.compose.AsyncImage
import coil.imageLoader
import coil.memory.MemoryCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Phase 5 — [AsyncImage] that shimmers while a *network* download is in flight, and never otherwise.
 *
 * Before showing anything it asks Coil's caches whether this URL is already on the device: the memory
 * cache synchronously, then the disk cache off the main thread. Cached images appear on their own
 * within a frame or two, so a shimmer there would only flash; only an image that is in neither cache
 * (a real download) gets the shimmer, and it is removed from composition the moment the image
 * succeeds or fails — no animation keeps running behind a loaded poster. While the disk check is
 * still pending nothing is shown beyond the parent's plain placeholder block.
 *
 * The cache keys are the plain URL: Coil's default memory/disk keys for an un-transformed string
 * request (this app configures no custom ImageLoader, keyers or transformations).
 */
@OptIn(ExperimentalCoilApi::class)
@Composable
fun ShimmerAsyncImage(
    model: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    // Override when the image sits on a surfaceVariant-coloured card, where the default shimmer base would blend in.
    shimmerBase: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val context = LocalContext.current
    val imageLoader = context.imageLoader

    var finished by remember(model) { mutableStateOf(false) }
    // null = not known yet, true = already on the device, false = needs a real download.
    var isCached by remember(model) {
        mutableStateOf(
            when {
                model == null -> true
                imageLoader.memoryCache?.get(MemoryCache.Key(model)) != null -> true
                else -> null
            }
        )
    }
    if (isCached == null && model != null) {
        LaunchedEffect(model) {
            isCached = withContext(Dispatchers.IO) {
                imageLoader.diskCache?.openSnapshot(model)?.use { true } ?: false
            }
        }
    }

    Box(modifier = modifier) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = contentScale,
            onSuccess = { finished = true },
            onError = { finished = true },
            modifier = Modifier.fillMaxSize()
        )
        if (!finished && isCached == false) {
            Box(modifier = Modifier.fillMaxSize().shimmer(shimmerBase))
        }
    }
}
