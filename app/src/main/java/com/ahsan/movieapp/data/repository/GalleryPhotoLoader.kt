package com.ahsan.movieapp.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.get
import androidx.core.graphics.scale
import com.ahsan.movieapp.data.remote.TmdbApi
import com.ahsan.movieapp.domain.model.Photo
import com.ahsan.movieapp.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Phase 5 — builds the detail screens' Photos gallery out of *real frames*, not artwork.
 *
 * TMDB's own `/images` `backdrops` turned out to be a mixed pool of key art, fan edits and composites
 * (even the language-neutral ones), with no field that says "this is a frame from the film", so they
 * are not used. Instead:
 *
 *  - **TV shows** use episode stills (`still_path`): a real frame from each episode. A show can have
 *    dozens of seasons, so a few seasons spread across its run are sampled (one call each, in
 *    parallel) and their stills interleaved so the gallery mixes early and late seasons.
 *  - **Movies** use the auto-generated frames YouTube makes for every HD video (`maxres1..3`) of the
 *    movie's own Trailer / Teaser / Clip videos listed by TMDB (Featurettes and behind-the-scenes are
 *    skipped). Each candidate frame is checked on a small 320px copy: vertical/pillarboxed shorts and
 *    near-black fades are dropped, and near-duplicate shots (the same moment showing up in both the
 *    trailer and the teaser) are removed with a perceptual hash.
 *
 * Network-only and best-effort: any failure yields an empty list and the detail screen simply hides
 * the Photos row. Callers are cached by [MovieRepositoryImpl].
 */
@Singleton
class GalleryPhotoLoader @Inject constructor(private val api: TmdbApi) {

    suspend fun loadTvStills(tvId: Int): List<Photo> {
        val seasonNumbers = api.getTvDetails(tvId).seasons.orEmpty()
            .filter { it.seasonNumber > 0 && (it.episodeCount ?: 0) > 0 }
            .map { it.seasonNumber }
            .sorted()
        if (seasonNumbers.isEmpty()) return emptyList()

        val sampledSeasons = spreadPick(seasonNumbers, MAX_STILL_SEASONS)
        val stillsPerSeason = coroutineScope {
            sampledSeasons.map { season ->
                async { runCatching { api.getSeasonDetails(tvId, season) }.getOrNull()?.episodes.orEmpty().mapNotNull { it.stillPath } }
            }.awaitAll()
        }

        val perSeasonCap = (PHOTO_LIMIT + sampledSeasons.size - 1) / sampledSeasons.size
        val picked = stillsPerSeason.map { spreadPick(it, perSeasonCap) }

        // Round-robin across the sampled seasons so the first photos aren't all from one season.
        val ordered = buildList {
            for (index in 0 until (picked.maxOfOrNull { it.size } ?: 0)) {
                picked.forEach { season -> season.getOrNull(index)?.let { add(it) } }
            }
        }
        return ordered.distinct().take(PHOTO_LIMIT).map { path ->
            Photo(
                thumbUrl = Constants.backdropUrl(path, Constants.BACKDROP_WIDTH).orEmpty(),
                fullUrl = Constants.backdropUrl(path, Constants.BACKDROP_WIDTH_LARGE).orEmpty(),
                aspectRatio = 16f / 9f
            )
        }
    }

    suspend fun loadMovieFrames(movieId: Int): List<Photo> {
        val videos = api.getMovieVideos(movieId).results.orEmpty()
            .filter { it.site == "YouTube" && it.type in FRAME_VIDEO_TYPES }
            .distinctBy { it.key }
            // Trailers first, then Teasers, then Clips; official uploads ahead of the rest.
            .sortedWith(compareBy({ FRAME_VIDEO_TYPES.indexOf(it.type) }, { it.official != true }))
            .take(MAX_FRAME_VIDEOS)
        if (videos.isEmpty()) return emptyList()

        val candidates = videos.flatMap { video ->
            (1..FRAMES_PER_VIDEO).map { n ->
                FrameCandidate(
                    fullUrl = "https://img.youtube.com/vi/${video.key}/maxres$n.jpg",
                    probeUrl = "https://img.youtube.com/vi/${video.key}/mq$n.jpg"
                )
            }
        }
        val hashes = coroutineScope { candidates.map { async { analyze(it.probeUrl) } }.awaitAll() }

        val keptHashes = mutableListOf<Long>()
        val kept = mutableListOf<Photo>()
        candidates.zip(hashes).forEach { (candidate, hash) ->
            if (hash == null) return@forEach
            if (keptHashes.any { java.lang.Long.bitCount(it xor hash) <= DUPLICATE_HASH_DISTANCE }) return@forEach
            keptHashes += hash
            kept += Photo(thumbUrl = candidate.fullUrl, fullUrl = candidate.fullUrl, aspectRatio = 16f / 9f)
        }
        return kept.take(PHOTO_LIMIT)
    }

    private data class FrameCandidate(val fullUrl: String, val probeUrl: String)

    /**
     * Downloads the small copy of a frame and returns its 64-bit difference hash, or null if the frame
     * is unusable: missing (HTTP error / network failure), a near-black fade, or pillarboxed (black
     * columns down both sides — a vertical short embedded in a 16:9 frame). Letterbox bars (top and
     * bottom) are normal for cinematic trailers and are kept.
     */
    private suspend fun analyze(url: String): Long? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = PROBE_TIMEOUT_MS
                readTimeout = PROBE_TIMEOUT_MS
            }
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
                val bytes = connection.inputStream.use { it.readBytes() }
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@runCatching null
                hashIfUsable(bitmap).also { bitmap.recycle() }
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }

    private fun hashIfUsable(bitmap: Bitmap): Long? {
        // A 32x18 luma grid is enough to judge both the black columns and the overall brightness.
        val grid = bitmap.scale(GRID_W, GRID_H, filter = true)
        val luma = IntArray(GRID_W * GRID_H) { index ->
            val pixel = grid[index % GRID_W, index / GRID_W]
            (((pixel shr 16) and 0xFF) * 299 + ((pixel shr 8) and 0xFF) * 587 + (pixel and 0xFF) * 114) / 1000
        }
        grid.recycle()

        if (luma.average() < NEAR_BLACK_LUMA) return null
        val sideColumns = PILLAR_COLUMNS
        var sideSum = 0
        var sideCount = 0
        for (y in 0 until GRID_H) {
            for (x in 0 until sideColumns) {
                sideSum += luma[y * GRID_W + x] + luma[y * GRID_W + (GRID_W - 1 - x)]
                sideCount += 2
            }
        }
        if (sideSum.toFloat() / sideCount < NEAR_BLACK_LUMA) return null

        // Difference hash on a 9x8 grid: one bit per "is this pixel brighter than its right neighbour".
        val small = IntArray(9 * 8)
        for (y in 0 until 8) {
            for (x in 0 until 9) {
                val sx = (x * GRID_W / 9).coerceAtMost(GRID_W - 1)
                val sy = (y * GRID_H / 8).coerceAtMost(GRID_H - 1)
                small[y * 9 + x] = luma[sy * GRID_W + sx]
            }
        }
        var hash = 0L
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                hash = (hash shl 1) or (if (small[y * 9 + x] > small[y * 9 + x + 1]) 1L else 0L)
            }
        }
        return hash
    }

    /** [count] items spread evenly across [items] (always including the first and last when count > 1). */
    private fun <T> spreadPick(items: List<T>, count: Int): List<T> {
        if (items.size <= count) return items
        if (count <= 1) return items.take(count)
        return (0 until count).map { items[(it * (items.size - 1).toDouble() / (count - 1)).roundToInt()] }.distinct()
    }

    private companion object {
        const val PHOTO_LIMIT = 20
        const val MAX_STILL_SEASONS = 4
        const val MAX_FRAME_VIDEOS = 5
        const val FRAMES_PER_VIDEO = 3
        // Hamming distance (of 64) at or below which two frames count as the same shot.
        const val DUPLICATE_HASH_DISTANCE = 10
        const val PROBE_TIMEOUT_MS = 4_000
        const val GRID_W = 32
        const val GRID_H = 18
        const val PILLAR_COLUMNS = 4
        const val NEAR_BLACK_LUMA = 14
        val FRAME_VIDEO_TYPES = listOf("Trailer", "Teaser", "Clip")
    }
}
