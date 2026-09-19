package com.ahsan.movieapp.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ahsan.movieapp.domain.model.Movie
import kotlin.math.abs
import kotlinx.coroutines.delay

/**
 * The Explore screen's top banner: a swipeable, full-width pager of [movies] (the caller passes
 * the top 8 popular titles), each card showing a backdrop image with title/rating scrimmed over
 * the bottom, plus a row of M3-style dots below (an active pill that widens 6→14dp and slides
 * smoothly between pages as you swipe, driven by [PagerState.currentPageOffsetFraction]). Sits
 * above the genre chips row and the rest of Explore's carousel sections — the "hero" in front of
 * the regular shelves, not a replacement for any of them.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroMovieCarousel(
    movies: List<Movie>,
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    if (movies.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { movies.size })

    // Auto-advances through the hero pages every AUTO_ADVANCE_MS, pausing while the user is
    // dragging. The leading delay is important: isScrollInProgress is also true while our own
    // animateScrollToPage() runs, so checking it right when this effect starts would read that
    // animation and leave the carousel advancing itself forever. Animate normally between
    // pages, but jump instantly back to the first page when wrapping past the last.
    LaunchedEffect(movies.size) {
        if (movies.size <= 1) return@LaunchedEffect
        while (true) {
            delay(AUTO_ADVANCE_MS)
            if (pagerState.isScrollInProgress) continue
            val next = pagerState.currentPage + 1
            if (next >= pagerState.pageCount) {
                pagerState.scrollToPage(0)
            } else {
                pagerState.animateScrollToPage(next)
            }
        }
    }

    Column(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 24.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val movie = movies[page]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onMovieClick(movie) }
            ) {
                AsyncImage(
                    model = movie.backdropUrl ?: movie.posterUrl,
                    contentDescription = movie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Full-height gradient (not just a bottom sliver) so the title/rating stay legible
                // over a bright backdrop, not just a dark one.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = movie.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "★ ${movie.ratingOutOfTen}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFFFC857)
                        )
                        Text(
                            text = movie.releaseYear,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        DotsPageIndicator(
            pagerState = pagerState,
            pageCount = movies.size,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 10.dp)
        )
    }
}

/**
 * A row of M3-style page dots — one 6dp circle per page, with the current page's dot widening
 * into a 14dp pill. The active pill widens continuously mid-swipe and hands off to the next dot
 * because it reads [PagerState.currentPageOffsetFraction], not just [PagerState.currentPage].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DotsPageIndicator(
    pagerState: PagerState,
    pageCount: Int,
    modifier: Modifier = Modifier
) {
    if (pageCount <= 1) return

    Row(
        modifier = modifier.height(DOT_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(DOT_SPACING),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            // 1 for the settled page, falling to 0 once the next page has fully arrived; the
            // neighbor on the other side rises symmetrically as it slides away.
            val widthFraction = (1f - abs(pagerState.currentPage - index + pagerState.currentPageOffsetFraction))
                .coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .width(DOT_WIDTH + (ACTIVE_DOT_WIDTH - DOT_WIDTH) * widthFraction)
                    .height(DOT_HEIGHT)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (widthFraction > 0.5f) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
                    )
            )
        }
    }
}

private val DOT_WIDTH = 6.dp
private val ACTIVE_DOT_WIDTH = 14.dp
private val DOT_HEIGHT = 6.dp
private val DOT_SPACING = 5.dp
private const val AUTO_ADVANCE_MS = 3_000L
