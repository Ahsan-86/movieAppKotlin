package com.ahsan.movieapp.ui.person

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ahsan.movieapp.R
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.ui.components.DOT_SEPARATOR
import com.ahsan.movieapp.ui.components.EmptyState
import com.ahsan.movieapp.ui.components.FullScreenError
import com.ahsan.movieapp.ui.components.FullScreenLoading
import com.ahsan.movieapp.ui.components.MovieListRow
import com.ahsan.movieapp.ui.components.SeparatorDiamond
import com.ahsan.movieapp.ui.components.backgroundSwatch
import com.ahsan.movieapp.ui.components.rememberBackdropPalette

/**
 * Full screen with a real, pinned top bar — title is the person's name, a normal back arrow next
 * to it — matching the same pattern as GenreScreen and the search/genre destinations: the app bar
 * is dynamic per screen rather than the generic "Movie App" bar the root tabs share (that one is
 * hidden entirely on this screen — see MovieNavGraph's TOP_LEVEL_ROUTES check). Everything below
 * the bar — the hero photo, bio, toggles, filmography — is one scrollable `LazyColumn`; the
 * filmography renders as the shared full-width `MovieListRow` rows (same as the search screen's
 * list-view mode), grouped by release year with a year header per section (Session 5 → option 2,
 * chosen 2026-09-20) and sorted newest-year-first.
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
            // The scroll threshold is derived from the snapshot-backed LazyListState fields, so it
            // recomposes only when the derived value actually flips. Loading/error have no hero
            // behind the bar, so they default to the opaque titled bar.
            val heroPeekPx = with(LocalDensity.current) { 100.dp.toPx() }
            val scrolledPastHero by remember(listState) {
                derivedStateOf {
                    listState.firstVisibleItemIndex > 0 ||
                        listState.firstVisibleItemScrollOffset > heroPeekPx
                }
            }
            val barOpaque = scrolledPastHero ||
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = barContentColor)
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
                    FullScreenError(message = state.errorMessage ?: stringResource(R.string.person_load_error))
                else -> PersonContent(
                    state = state,
                    listState = listState,
                    profileTone = profileTone,
                    onMovieClick = onMovieClick,
                    onTvClick = onTvClick,
                    onBucketSelected = viewModel::selectBucket,
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
    profileTone: Color?,
    onMovieClick: (Movie) -> Unit,
    onTvClick: (Movie) -> Unit,
    onBucketSelected: (MediaTab, RoleTab) -> Unit,
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

        if (state.creditBuckets.size > 1) {
            item {
                PersonFilterRow(
                    buckets = state.creditBuckets,
                    selectedMediaType = state.selectedMediaType,
                    selectedRole = state.selectedRole,
                    tone = profileTone,
                    onBucketSelected = onBucketSelected
                )
            }
        }

        if (movies.isEmpty()) {
            item {
                val mediaLabel = stringResource(
                    if (state.selectedMediaType == MediaTab.TV) R.string.person_tv_credits else R.string.person_movies
                )
                val roleLabel = stringResource(
                    if (state.selectedRole == RoleTab.DIRECTOR) R.string.person_directed else R.string.person_appeared_in
                )
                EmptyState(
                    title = stringResource(R.string.nothing_here),
                    body = stringResource(R.string.person_no_known_credits, mediaLabel, roleLabel, state.personName),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            // Year-grouped filmography (Session 5 → option 2, chosen 2026-09-20): still the same
            // vertical full-width MovieListRow rows as before, just sorted newest-year-first and
            // split into year sections by the header below.
            state.filmographyByYear.forEach { (year, movies) ->
                item(key = "filmography_year_$year") {
                    FilmographyYearHeader(year = year, tone = profileTone)
                }
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
                        // Favoriting stays movie-only for now — the app's Favorites table doesn't
                        // yet distinguish movies from TV shows, and mixing the two in there ahead
                        // of real TV support would just create bad data to clean up later.
                        onToggleFavorite = if (state.selectedMediaType == MediaTab.MOVIES) {
                            { onToggleFavorite(movie) }
                        } else null
                    )
                }
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
                // a value, "Unknown" included, so this line never disappears entirely. Both labels
                // are mapped here from raw values so they localize with the rest of the app.
                val ageSegment = details.age?.let { stringResource(R.string.person_years_old, it) }
                val genderSegment = stringResource(
                    when (details.gender) {
                        1 -> R.string.person_gender_female
                        2 -> R.string.person_gender_male
                        3 -> R.string.person_gender_nonbinary
                        else -> R.string.unknown
                    }
                )
                val ageGenderLine = listOfNotNull(ageSegment, genderSegment).joinToString(DOT_SEPARATOR)
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
            Text(if (expanded) stringResource(R.string.person_show_less) else stringResource(R.string.person_read_more))
        }
    }
}

@Composable
private fun PersonFilterRow(
    buckets: List<CreditBucket>,
    selectedMediaType: MediaTab,
    selectedRole: RoleTab,
    tone: Color?,
    onBucketSelected: (MediaTab, RoleTab) -> Unit
) {
    // One organized chip group per present credit bucket (Review-queue item 4 → option A, chosen
    // 2026-09-20, layout/labels finalized 2026-09-26). Chip labels are exact and ordered: Acting -
    // Movies, Acting - TV Shows, Directed - Movies, Directed - TV Shows.
    //
    // Count-driven layout (2026-09-26): with exactly two buckets the chips sit on **one horizontal
    // row and the labels wrap** (the palette gradient grows with the wrapped text); with more than
    // two they **stack vertically as full-width pills with centered text** — wrap stays off, so the
    // label is single-line and the gradient spans the whole row, never a scattered multi-line wrap.
    //
    // The whole group sits inside a soft rounded container (2026-09-26 — A1: the palette tone at
    // low alpha, or surfaceVariant when no tone was sampled) so the filters read as one unit. In the
    // two-chip horizontal layout a hairline divider in the palette tone splits the two chips
    // (B2 — see Progress.md; B3 is the standing fallback if B2 doesn't look right).
    val labelOf: @Composable (CreditBucket) -> String = { bucket ->
        val labelRes: Int? = when (bucket.role to bucket.media) {
            RoleTab.ACTOR to MediaTab.MOVIES -> R.string.person_acting_movies
            RoleTab.ACTOR to MediaTab.TV -> R.string.person_acting_tv_shows
            RoleTab.DIRECTOR to MediaTab.MOVIES -> R.string.person_directed_movies
            RoleTab.DIRECTOR to MediaTab.TV -> R.string.person_directed_tv_shows
            else -> null
        }
        labelRes?.let { stringResource(it) } ?: ""
    }

    val vertical = buckets.size > 2
    val chip: @Composable (CreditBucket) -> Unit = { bucket ->
        PersonCreditChip(
            label = labelOf(bucket),
            selected = selectedMediaType == bucket.media && selectedRole == bucket.role,
            tone = tone,
            wrapText = !vertical,
            fillWidth = vertical,
            onClick = { onBucketSelected(bucket.media, bucket.role) }
        )
    }

    val containerModifier = Modifier
        .fillMaxWidth()
        .padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 4.dp)

    Column(modifier = containerModifier) {
        // A3 (2026-09-26 — chosen over the A4 gradient band): hairline framing only — a thin
        // full-width divider above and below the group, no box, so the chips keep their own
        // gradients without being overshadowed.
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Box(modifier = Modifier.padding(vertical = 10.dp)) {
            if (vertical) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    buckets.forEach { chip(it) }
                }
            } else {
                // Two chips: pin one to each edge of the row with a small palette-tone diamond between
                // them (B3 — the 2-chip accent, chosen 2026-09-26 over the B2 hairline).
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    chip(buckets[0])
                    SeparatorDiamond(
                        size = 6.dp,
                        color = tone ?: MaterialTheme.colorScheme.secondary
                    )
                    chip(buckets[1])
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/**
 * One credit-filter chip in the filmography year-chip style (2026-09-20): same 10dp radius,
 * 12/6 padding and bold titleMedium text as [FilmographyYearHeader]. The selected bucket fills
 * with the person palette gradient (tone → darkened tone) and adapts its text color to the tone's
 * luminance, exactly like the year chips; unselected buckets carry a faint wash of the same
 * gradient (2026-09-26 — no more bare outline rectangles) plus a soft border and muted text.
 *
 * [wrapText] allows the label to break to a second line with the gradient growing to wrap it too
 * (the two-chip horizontal row); otherwise it stays on one line, ellipsized if needed. [fillWidth]
 * stretches the chip to the full row width with the text centered (2026-09-26 — the vertical stack
 * reads as a single-column list, each chip a full-width pill).
 */
@Composable
private fun PersonCreditChip(
    label: String,
    selected: Boolean,
    tone: Color?,
    wrapText: Boolean,
    fillWidth: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val bandStart = tone ?: MaterialTheme.colorScheme.secondary
    val bandEnd = (tone?.let { lerp(it, Color.Black, 0.25f) }) ?: MaterialTheme.colorScheme.primary
    val onBandColor = if ((tone?.luminance() ?: 0f) > 0.5f) Color(0xFF111111) else Color.White
    val gradient = Brush.horizontalGradient(listOf(bandStart, bandEnd))
    val chipStyle = if (selected) {
        Modifier.background(gradient)
    } else {
        Modifier
            .background(
                Brush.horizontalGradient(
                    listOf(bandStart.copy(alpha = 0.22f), bandEnd.copy(alpha = 0.22f))
                )
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = shape
            )
    }

    Box(
        modifier = Modifier
            .clip(shape)
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .then(chipStyle)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) onBandColor else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (wrapText) Int.MAX_VALUE else 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (fillWidth) TextAlign.Center else TextAlign.Start
        )
    }
}

/**
 * Section header for one release year in the year-grouped filmography (Session 5 → option 2,
 * chosen 2026-09-20): a rounded gradient chip sized to the year text only (Ahsan's styling picks
 * 2026-09-20 — no full-width band). The gradient runs from the person photo's palette [tone] into
 * a darkened version of it, so it harmonizes with the palette-tinted screen background instead of
 * clashing; [MaterialTheme.colorScheme]'s secondary → primary is the fallback when no tone was
 * sampled (loading/error). Text is dark on light tones and white on dark ones. The unknown-year
 * bucket (the "—" releaseYear placeholder from [Movie.releaseYear]) reads "Unknown year", not a
 * bare dash.
 */
@Composable
private fun FilmographyYearHeader(year: String, tone: Color?) {
    val label = if (year.toIntOrNull() != null) year else stringResource(R.string.person_unknown_year)
    val bandStart = tone ?: MaterialTheme.colorScheme.secondary
    val bandEnd = (tone?.let { lerp(it, Color.Black, 0.25f) }) ?: MaterialTheme.colorScheme.primary
    val textColor = if ((tone?.luminance() ?: 0f) > 0.5f) Color(0xFF111111) else Color.White
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.horizontalGradient(listOf(bandStart, bandEnd)))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}
