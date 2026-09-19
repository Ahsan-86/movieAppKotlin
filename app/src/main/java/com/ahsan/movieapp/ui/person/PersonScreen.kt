package com.ahsan.movieapp.ui.person

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.ui.components.EmptyState
import com.ahsan.movieapp.ui.components.FullScreenError
import com.ahsan.movieapp.ui.components.FullScreenLoading
import com.ahsan.movieapp.ui.components.MovieListRow
import com.ahsan.movieapp.ui.components.backgroundSwatch
import com.ahsan.movieapp.ui.components.rememberBackdropPalette

/**
 * Full screen with a real, pinned top bar — title is the person's name, a normal back arrow next
 * to it — matching the same pattern as GenreScreen and the search/genre destinations: the app bar
 * is dynamic per screen rather than the generic "Movie App" bar the root tabs share (that one is
 * hidden entirely on this screen — see MovieNavGraph's TOP_LEVEL_ROUTES check). Everything below
 * the bar — the hero photo, bio, toggles, filmography — is one scrollable `LazyColumn`; the
 * filmography renders as full-width list rows (the shared `MovieListRow`, same as the search
 * screen's list-view mode) rather than a poster grid, which is also why this is a plain
 * `LazyColumn` and not a `LazyVerticalGrid` like it used to be.
 *
 * The bar is transparent and floats over the content rather than pushing it down, so the hero
 * photo can start right at the very top of the screen (behind the status bar) instead of leaving a
 * plain-background gap above it — the common "photo behind the app bar" treatment. A short gradient
 * scrim sits behind the bar so the back button and title stay legible regardless of what's showing
 * underneath (a bright portrait, or the plain background during loading/error). The title itself is
 * hidden at rest (the hero caption shows the name then) and fades in only once the hero has scrolled
 * past — the same scroll-aware bar as the Movie/TV detail screens (2026-09-20 alignment pass), and
 * the same luminance-adaptive scrim + palette-tinted background those screens use.
 */
@OptIn(ExperimentalMaterial3Api::class)
// contentWindowInsets is deliberately (0,0,0,0) below (see the comment on that param) — the
// Scaffold's content-padding parameter really has nothing to apply, so it's discarded as `_`.
// The lint check can't tell "deliberately unused" from "forgot to apply it", hence the suppress.
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun PersonScreen(
    onBack: () -> Unit,
    onMovieClick: (Movie) -> Unit,
    // TV filmography rows route to the real TV detail screen (2026-09-22 — the "not available
    // yet" toast was retired now that TvDetailScreen exists).
    onTvClick: (Movie) -> Unit,
    viewModel: PersonViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    // One list state shared by the scroll-aware bar above and the LazyColumn in PersonContent, so
    // the bar can read how far the hero has scrolled.
    val listState = rememberLazyListState()

    val profileUrl = state.details?.profileUrl
    val palette = rememberBackdropPalette(profileUrl)
    val profileTone = palette?.backgroundSwatch?.rgb?.let { Color(it) }
    // Bright portraits get a stronger scrim behind the transparent bar so the white title/back stay
    // legible; dark ones keep the lighter scrim. Defaults to a dark tone (0.2 luminance) so the
    // scrim never disappears when there's no sampled color (loading/error states).
    val scrimAlpha = (0.5f + 0.25f * (profileTone?.luminance() ?: 0.2f)).coerceIn(0.5f, 0.9f)
    // The screen background is the theme background tinted toward the photo's palette, so the
    // sections below the hero continue the image's color instead of a flat theme color. The theme
    // tint still rules — palette nudges it about a third of the way.
    val background = profileTone?.let { lerp(MaterialTheme.colorScheme.background, it, 0.35f) }
        ?: MaterialTheme.colorScheme.background

    Scaffold(
        containerColor = background,
        topBar = {
            // PersonHero is the first (tall, 3:4) item in the LazyColumn below, so "scrolled past
            // 100dp" is: the first item's scroll offset past 100dp, or any later item on screen.
            // The LazyListState fields are snapshot-backed, so reading them here recomposes only
            // this lambda as the page scrolls. Loading/error have no hero behind the bar, so they
            // default to the opaque titled bar.
            val barOpaque = listState.firstVisibleItemIndex > 0 ||
                listState.firstVisibleItemScrollOffset > with(LocalDensity.current) { 100.dp.toPx() } ||
                state.isLoading || (state.errorMessage != null && state.credits == null)
            val barColor by animateColorAsState(if (barOpaque) MaterialTheme.colorScheme.surface else Color.Transparent)
            val barContentColor by animateColorAsState(if (barOpaque) MaterialTheme.colorScheme.onSurface else Color.White)
            Box {
                AnimatedVisibility(visible = !barOpaque) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = scrimAlpha), Color.Transparent)
                                )
                            )
                    )
                }
                TopAppBar(
                    title = {
                        AnimatedVisibility(visible = barOpaque) {
                            Text(state.personName, color = barContentColor)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = barContentColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = barColor)
                )
            }
        },
        // Zero content insets — paired with the transparent bar above, this lets the hero photo
        // (and, in the loading/error states, the plain background) run all the way to the top of
        // the screen instead of stopping below a reserved app-bar-height gap.
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> FullScreenLoading()
                state.errorMessage != null && state.credits == null ->
                    FullScreenError(message = state.errorMessage ?: "Couldn't load this person")
                else -> PersonContent(
                    state = state,
                    listState = listState,
                    onMovieClick = onMovieClick,
                    onTvClick = onTvClick,
                    onMediaTabSelected = viewModel::onMediaTabSelected,
                    onRoleTabSelected = viewModel::onRoleTabSelected,
                    onToggleFavorite = viewModel::toggleFavorite
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonContent(
    state: PersonUiState,
    listState: LazyListState,
    onMovieClick: (Movie) -> Unit,
    onTvClick: (Movie) -> Unit,
    onMediaTabSelected: (MediaTab) -> Unit,
    onRoleTabSelected: (RoleTab) -> Unit,
    onToggleFavorite: (Movie) -> Unit
) {
    val movies = state.displayedMovies

    // No horizontal contentPadding here — the hero photo needs to run edge-to-edge (fix #4), and
    // the filmography rows use MovieListRow exactly as SearchResultsList does, unpadded. The
    // filter row and empty state add their own horizontal inset since they're plain text/controls,
    // not full-bleed media.
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        state.details?.let { details ->
            item { PersonHero(details = details) }
        }

        if (state.showTvTab || state.showDirectorTab) {
            item {
                PersonFilterRow(
                    showTvTab = state.showTvTab,
                    showDirectorTab = state.showDirectorTab,
                    selectedMediaType = state.selectedMediaType,
                    selectedRole = state.selectedRole,
                    onMediaTabSelected = onMediaTabSelected,
                    onRoleTabSelected = onRoleTabSelected
                )
            }
        }

        if (movies.isEmpty()) {
            item {
                val mediaLabel = if (state.selectedMediaType == MediaTab.TV) "TV credits" else "movies"
                val roleLabel = if (state.selectedRole == RoleTab.DIRECTOR) "directed" else "appeared in"
                EmptyState(
                    title = "Nothing here",
                    body = "No known $mediaLabel $roleLabel for ${state.personName}.",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            items(movies, key = { it.id }) { movie ->
                MovieListRow(
                    movie = movie,
                    // TV ids aren't movie ids — TV rows route to the real TV detail screen
                    // (onTvClick), not through the movie detail route the Movies tab uses.
                    onClick = {
                        if (state.selectedMediaType == MediaTab.TV) {
                            onTvClick(movie)
                        } else {
                            onMovieClick(movie)
                        }
                    },
                    // Favoriting stays movie-only for now — the app's Favorites table doesn't yet
                    // distinguish movies from TV shows, and mixing the two in there ahead of real
                    // TV support would just create bad data to clean up later.
                    onToggleFavorite = if (state.selectedMediaType == MediaTab.MOVIES) {
                        { onToggleFavorite(movie) }
                    } else null
                )
            }
        }
    }
}

/**
 * Full-bleed hero photo — no rounded card, no border, edge-to-edge width (fix #4) — with the
 * person's name/profession/age·gender overlaid directly on it as a caption (fix #3), instead of
 * a separate text block underneath. Legibility comes from two stacked scrims instead of a border:
 * a soft radial vignette that darkens the corners/edges all the way around, plus a taller bottom
 * gradient behind the caption text specifically. Only the (optional) biography still lives below
 * the photo, since a multi-paragraph bio can't reasonably be overlaid without hurting legibility.
 * This is a plain item inside the outer LazyColumn (see PersonContent) so it scrolls underneath
 * the pinned, transparent top bar as the user browses — the back button stays put, and this caption
 * is where the name lives at rest; the top bar's own title fades in only after the hero has scrolled
 * past (the scroll-aware bar in [PersonScreen]), so the two never show the name simultaneously.
 */
@Composable
private fun PersonHero(details: com.ahsan.movieapp.domain.model.PersonDetails) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = details.profileUrl,
                contentDescription = details.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Vignette: darkens corners/edges all around via a radial gradient centered on the
            // photo, replacing the old hard-edged border with a soft falloff.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.32f))
                        )
                    )
            )

            // Caption scrim: a taller, stronger gradient behind just the bottom portion so the
            // three-line name/profession/age·gender caption stays legible over any photo.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.55f)
                    .align(Alignment.BottomStart)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Text(
                    text = details.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (details.role != null) {
                    Text(
                        text = details.role,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                // Age is omitted (not "0 years old") when birthday is unknown; gender always shows
                // a value, "Unknown" included, so this line never disappears entirely.
                val ageGenderLine = listOfNotNull(details.ageText, details.genderLabel).joinToString(" · ")
                Text(
                    text = ageGenderLine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        if (!details.biography.isNullOrBlank()) {
            ExpandableBio(
                text = details.biography,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 0.dp)
            )
        }
    }
}

@Composable
private fun ExpandableBio(text: String, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (expanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis
        )
        TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
            Text(if (expanded) "Show less" else "Read more")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonFilterRow(
    showTvTab: Boolean,
    showDirectorTab: Boolean,
    selectedMediaType: MediaTab,
    selectedRole: RoleTab,
    onMediaTabSelected: (MediaTab) -> Unit,
    onRoleTabSelected: (RoleTab) -> Unit
) {
    Column(modifier = Modifier.padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 4.dp)) {
        if (showTvTab) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedMediaType == MediaTab.MOVIES,
                    onClick = { onMediaTabSelected(MediaTab.MOVIES) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("Movies") }
                SegmentedButton(
                    selected = selectedMediaType == MediaTab.TV,
                    onClick = { onMediaTabSelected(MediaTab.TV) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("TV Shows") }
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
        }
        if (showDirectorTab) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedRole == RoleTab.ACTOR,
                    onClick = { onRoleTabSelected(RoleTab.ACTOR) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("As Actor") }
                SegmentedButton(
                    selected = selectedRole == RoleTab.DIRECTOR,
                    onClick = { onRoleTabSelected(RoleTab.DIRECTOR) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("As Director") }
            }
        }
    }
}
