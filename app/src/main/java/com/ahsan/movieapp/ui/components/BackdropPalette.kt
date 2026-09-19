package com.ahsan.movieapp.ui.components

import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Downloads [backdropUrl] at a small size (512px — served from Coil's cache when the full-res
 * backdrop was already fetched for the hero, and decodes fast enough to run once per screen) and
 * extracts a [Palette] off the main thread. Memoized by URL: the sample runs once per image and a
 * theme/rotation change reuses it. Returns null on any failure or a blank URL so callers fall back
 * to their theme background silently.
 */
@Composable
fun rememberBackdropPalette(backdropUrl: String?): Palette? {
    var palette by remember { mutableStateOf<Palette?>(null) }
    val context = LocalContext.current

    LaunchedEffect(backdropUrl) {
        palette = null
        if (backdropUrl.isNullOrBlank()) return@LaunchedEffect
        palette = withContext(Dispatchers.Default) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(backdropUrl)
                    .size(512)
                    // Palette can't read the hardware bitmaps Coil allocates by default — Palette.from
                    // throws on them and the whole sample silently fails. Force a software decode.
                    .allowHardware(false)
                    .build()
                val result = context.imageLoader.execute(request)
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    ?: return@runCatching null
                if (bitmap.isRecycled) return@runCatching null
                Palette.from(bitmap).maximumColorCount(16).generate()
            }.getOrNull()
        }
    }

    return palette
}

/**
 * The swatch best suited to tint the content background with the hero's color. Dominant is
 * usually near-black or grey on movie backdrops (useless as a tint), so muted first, then vibrant,
 * then dominant only as a last resort.
 */
val Palette.backgroundSwatch: Palette.Swatch?
    get() = mutedSwatch ?: vibrantSwatch ?: dominantSwatch