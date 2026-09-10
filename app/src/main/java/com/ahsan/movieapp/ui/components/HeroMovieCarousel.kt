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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/**
 * The Explore screen's top banner: a swipeable, full-width pager of [movies] (the caller passes
 * the top 8 popular titles), each card showing a backdrop image with title/rating scrimmed over
 * the bottom, plus a sliding-line progress indicator below (a thin track with a shorter
 * highlighted segment that slides as you swipe, rather than a row of per-page dots). Sits above
 * the genre chips row and the rest of Explore's carousel sections — the "hero" in front of the
 * regular shelves, not a replacement for any of them.
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

        SlidingPageIndicator(
            pagerState = pagerState,
            pageCount = movies.size,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 10.dp)
        )
    }
}

/**
 * A thin horizontal track with a shorter highlighted segment that slides left-to-right as the
 * pager scrolls — used instead of a row of per-page dots. Reads [PagerState.currentPageOffsetFraction]
 * (not just [PagerState.currentPage]) so the segment moves continuously mid-swipe, not just when a
 * page snap completes.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SlidingPageIndicator(
    pagerState: PagerState,
    pageCount: Int,
    modifier: Modifier = Modifier
) {
    if (pageCount <= 1) return
    val progress = ((pagerState.currentPage + pagerState.currentPageOffsetFraction) / (pageCount - 1))
        .coerceIn(0f, 1f)
    val thumbWidth = INDICATOR_TRACK_WIDTH / pageCount

    Box(
        modifier = modifier
            .width(INDICATOR_TRACK_WIDTH)
            .height(INDICATOR_HEIGHT)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(
            modifier = Modifier
                .offset(x = (INDICATOR_TRACK_WIDTH - thumbWidth) * progress)
                .width(thumbWidth)
                .height(INDICATOR_HEIGHT)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

private val INDICATOR_TRACK_WIDTH = 56.dp
private val INDICATOR_HEIGHT = 3.dp
