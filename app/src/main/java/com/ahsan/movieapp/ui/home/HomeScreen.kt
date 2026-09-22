package com.ahsan.movieapp.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ahsan.movieapp.R
import com.ahsan.movieapp.domain.model.GenreChip
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.ui.components.HeroMovieCarousel
import com.ahsan.movieapp.ui.components.MovieCarouselSection
import com.ahsan.movieapp.ui.components.OfflineBanner
import kotlinx.coroutines.flow.Flow

/**
 * The "Explore" home: a hero banner of the top popular movies, a text-only genre chips row, then
 * a stack of themed, horizontally-scrolling carousels (Popular, For You, Upcoming, Sci-Fi Movies,
 * then Popular/On The Air/Sci-Fi TV Shows). No heading text of its own — the persistent "Movie App"
 * top bar covers that, so this screen's whole content area is the hero-first layout described above.
 */
@Composable
fun HomeScreen(
    onMovieClick: (Movie) -> Unit,
    // Phase 2.6 Session 1 — Popular TV Shows row's tap target: the real TV detail screen. Still
    // modeled as [Movie] like every other TV item in this app (its `id` is a TV id, not a movie id).
    onTvClick: (Movie) -> Unit,
    onGenreClick: (GenreChip) -> Unit,
    // Fires when the user re-taps the already-selected Explore tab — scrolls back to top instead
    // of doing nothing, matching Instagram/YouTube-style tab-reselect behavior.
    scrollToTopEvents: Flow<Unit>? = null,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(scrollToTopEvents) {
        scrollToTopEvents?.collect { listState.animateScrollToItem(0) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OfflineBanner(visible = state.isOffline)

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item(key = "hero") {
                // Genre id → display name from the already-fetched chips list, so the hero card's
                // genre line (diamond-joined, 2026-09-26) has real names to show.
                val genreNameById = remember(state.genreChips) {
                    state.genreChips.mapNotNull { chip ->
                        chip.movieGenreId?.let { it to chip.name }
                    }.toMap()
                }
                HeroMovieCarousel(
                    movies = state.heroMovies,
                    onMovieClick = onMovieClick,
                    genreNameById = genreNameById,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            item(key = "genre_chips") {
                ExploreGenreChipsRow(genres = state.genreChips, onGenreClick = onGenreClick)
            }

            items(state.sections, key = { it.labelRes }) { section ->
                MovieCarouselSection(
                    title = stringResource(section.labelRes),
                    movies = section.movies,
                    isLoading = section.isLoading,
                    errorMessage = section.errorMessage.takeIf { section.movies.isEmpty() },
                    onRetry = viewModel::retry,
                    onMovieClick = if (section.isTv) {
                        { onTvClick(it) }
                    } else onMovieClick,
                    onToggleFavorite = if (section.allowFavoriting) { { viewModel.toggleFavorite(it) } } else null
                )
            }
        }
    }
}

/** A "Categories" heading plus text-only genre chips (no icons, unlike the search screen's
 *  image-backed genre cards) — a quicker, lower-commitment way to jump into a genre straight
 *  from Explore. */
@Composable
private fun ExploreGenreChipsRow(genres: List<GenreChip>, onGenreClick: (GenreChip) -> Unit) {
    if (genres.isEmpty()) return
    Column {
        androidx.compose.material3.Text(
            text = stringResource(R.string.categories),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(genres, key = { it.navId }) { genre ->
                AssistChip(
                    onClick = { onGenreClick(genre) },
                    label = { androidx.compose.material3.Text(genre.name) }
                )
            }
        }
    }
}
