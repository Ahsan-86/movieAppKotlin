package com.ahsan.movieapp.ui.components

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Phase 5 — the app-wide shimmer / skeleton-loading toolkit, replacing the old centered spinners.
 *
 * [Modifier.shimmer] paints a moving highlight band over a theme-derived base colour (so it reads in
 * both light and dark themes), and the animation reads its progress inside the draw phase — it never
 * recomposes the skeleton. If the system's animations are turned off (Developer options "Animator
 * duration scale" = off, which is also what the accessibility "remove animations" switch sets) the
 * band is drawn static: a flat placeholder, no motion.
 *
 * The building blocks below ([ShimmerBox], [ShimmerLine], [PosterCardSkeleton], …) mirror the real
 * layouts' dimensions so content swaps in without the screen jumping around.
 */
@Composable
fun Modifier.shimmer(base: Color = MaterialTheme.colorScheme.surfaceVariant): Modifier {
    val highlight = lerp(base, MaterialTheme.colorScheme.onSurface, 0.14f)
    val context = LocalContext.current
    val animationsEnabled = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
    if (!animationsEnabled) return this.drawWithCache { onDrawBehind { drawRect(base) } }

    val progress = rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1300, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerProgress"
    )
    return this.drawWithCache {
        val width = size.width
        onDrawBehind {
            drawRect(base)
            // The band sweeps from fully off the left edge to fully off the right edge.
            val startX = -width + progress.value * 2f * width
            drawRect(
                Brush.linearGradient(
                    colors = listOf(Color.Transparent, highlight, Color.Transparent),
                    start = Offset(startX, 0f),
                    end = Offset(startX + width, size.height * 0.4f)
                )
            )
        }
    }
}

/** A shimmering rounded block — the primitive every skeleton below is made of. */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(8.dp)) {
    Box(modifier = modifier.clip(shape).shimmer())
}

/** A shimmering "line of text" — [widthFraction] of the available width, [height] tall. */
@Composable
fun ShimmerLine(
    modifier: Modifier = Modifier,
    widthFraction: Float = 1f,
    height: Dp = 12.dp
) {
    ShimmerBox(
        modifier = modifier.fillMaxWidth(widthFraction).height(height),
        shape = RoundedCornerShape(6.dp)
    )
}

/** Skeleton twin of [MoviePosterCard]: 2:3 poster, a two-line title, a short year line. */
@Composable
fun PosterCardSkeleton(modifier: Modifier = Modifier, width: Dp? = 128.dp) {
    Column(modifier = modifier.let { if (width != null) it.width(width) else it.fillMaxWidth() }) {
        ShimmerBox(
            modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
            shape = RoundedCornerShape(14.dp)
        )
        ShimmerLine(modifier = Modifier.padding(top = 8.dp), widthFraction = 0.85f)
        ShimmerLine(modifier = Modifier.padding(top = 6.dp), widthFraction = 0.4f, height = 10.dp)
    }
}

/** A non-scrolling horizontal row of [PosterCardSkeleton]s — a carousel's loading state. */
@Composable
fun PosterRowSkeleton(
    modifier: Modifier = Modifier,
    count: Int = 6,
    cardWidth: Dp = 128.dp,
    horizontalPadding: Dp = 16.dp
) {
    LazyRow(
        modifier = modifier,
        userScrollEnabled = false,
        contentPadding = PaddingValues(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items((0 until count).toList()) { PosterCardSkeleton(width = cardWidth) }
    }
}

/** A non-scrolling grid of [PosterCardSkeleton]s — Trending / Genre / Collection / Search grids. */
@Composable
fun PosterGridSkeleton(
    modifier: Modifier = Modifier,
    columns: GridCells = GridCells.Adaptive(minSize = 128.dp),
    count: Int = 12
) {
    LazyVerticalGrid(
        columns = columns,
        modifier = modifier.fillMaxSize(),
        userScrollEnabled = false,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(count) { PosterCardSkeleton(width = null) }
    }
}

/** Skeleton twin of [MovieListRow]: poster thumbnail + title / year / two overview lines. */
@Composable
fun ListRowSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        ShimmerBox(
            modifier = Modifier.width(64.dp).aspectRatio(2f / 3f),
            shape = RoundedCornerShape(10.dp)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ShimmerLine(widthFraction = 0.7f, height = 16.dp)
            ShimmerLine(widthFraction = 0.25f, height = 10.dp)
            ShimmerLine(widthFraction = 0.95f, height = 10.dp)
            ShimmerLine(widthFraction = 0.6f, height = 10.dp)
        }
    }
}

/** A non-scrolling stack of [ListRowSkeleton]s — list-mode search results, Favorites, filmography. */
@Composable
fun ListSkeleton(modifier: Modifier = Modifier, count: Int = 8) {
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        repeat(count) { ListRowSkeleton() }
    }
}

/** Skeleton for the Explore hero pager: one big rounded card with the real pager's insets + dots. */
@Composable
fun HeroSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        ShimmerBox(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 42.dp).aspectRatio(16f / 12f),
            shape = RoundedCornerShape(20.dp)
        )
        Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(5) { ShimmerBox(modifier = Modifier.size(6.dp), shape = CircleShape) }
        }
    }
}

/** A heading-sized shimmer line sitting where a section title will appear. */
@Composable
fun SectionTitleSkeleton(modifier: Modifier = Modifier) {
    ShimmerLine(modifier = modifier.padding(horizontal = 16.dp), widthFraction = 0.4f, height = 20.dp)
}

/**
 * Skeleton for the Movie / TV detail screens: the 3:4 portrait hero (with caption lines where the
 * title and meta sit), an Overview block, and a cast-style row. Non-scrolling by design — it is only
 * shown while the first load is in flight, and the real screen replaces it wholesale.
 */
@Composable
fun DetailScreenSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f)) {
            ShimmerBox(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(0.dp))
            Column(
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomStart)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ShimmerLine(widthFraction = 0.6f, height = 24.dp)
                ShimmerLine(widthFraction = 0.4f, height = 12.dp)
            }
        }
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ShimmerLine(widthFraction = 0.3f, height = 20.dp)
            ShimmerLine(height = 12.dp)
            ShimmerLine(height = 12.dp)
            ShimmerLine(widthFraction = 0.7f, height = 12.dp)
        }
        SectionTitleSkeleton()
        CastRowSkeleton(modifier = Modifier.padding(top = 12.dp))
    }
}

/** A row of circular-avatar + two-line-name placeholders, like the detail screens' cast strip. */
@Composable
fun CastRowSkeleton(modifier: Modifier = Modifier, count: Int = 6) {
    LazyRow(
        modifier = modifier,
        userScrollEnabled = false,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items((0 until count).toList()) {
            Column(modifier = Modifier.width(88.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                ShimmerBox(modifier = Modifier.size(72.dp), shape = CircleShape)
                ShimmerLine(modifier = Modifier.padding(top = 8.dp), widthFraction = 0.9f, height = 10.dp)
                ShimmerLine(modifier = Modifier.padding(top = 6.dp), widthFraction = 0.6f, height = 10.dp)
            }
        }
    }
}

/** Skeleton for a cast & crew list row: 48dp avatar + name / character lines. */
@Composable
fun PersonRowSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShimmerBox(modifier = Modifier.size(48.dp), shape = CircleShape)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ShimmerLine(widthFraction = 0.5f, height = 14.dp)
            ShimmerLine(widthFraction = 0.3f, height = 10.dp)
        }
    }
}

@Composable
fun PersonListSkeleton(modifier: Modifier = Modifier, count: Int = 10) {
    Column(modifier = modifier.fillMaxSize().padding(top = 12.dp)) {
        repeat(count) { PersonRowSkeleton() }
    }
}

/** Skeleton for an episode card: 16:9 still, title line, date line, two overview lines. */
@Composable
fun EpisodeRowSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ShimmerBox(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f), shape = RoundedCornerShape(10.dp))
        ShimmerLine(widthFraction = 0.6f, height = 16.dp)
        ShimmerLine(widthFraction = 0.25f, height = 10.dp)
        ShimmerLine(height = 10.dp)
        ShimmerLine(widthFraction = 0.8f, height = 10.dp)
    }
}

@Composable
fun EpisodeListSkeleton(modifier: Modifier = Modifier, count: Int = 3) {
    Column(modifier = modifier.fillMaxSize()) {
        repeat(count) { EpisodeRowSkeleton() }
    }
}

/**
 * Skeleton for the Person screen: the 3:4 profile hero, a filter-row of chips, then filmography rows.
 */
@Composable
fun PersonScreenSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        ShimmerBox(modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f), shape = RoundedCornerShape(0.dp))
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(3) { ShimmerBox(modifier = Modifier.width(88.dp).height(32.dp), shape = RoundedCornerShape(16.dp)) }
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            repeat(3) { ListRowSkeleton() }
        }
    }
}
