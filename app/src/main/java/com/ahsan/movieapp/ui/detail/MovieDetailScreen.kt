package com.ahsan.movieapp.ui.detail

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ahsan.movieapp.R
import com.ahsan.movieapp.domain.model.CastMember
import com.ahsan.movieapp.domain.model.CollectionSummary
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.domain.model.MovieDetails
import com.ahsan.movieapp.domain.model.WatchProvider
import com.ahsan.movieapp.domain.model.WatchProviderRegion
import com.ahsan.movieapp.ui.components.CastMemberCard
import com.ahsan.movieapp.ui.components.DetailHeroCaption
import com.ahsan.movieapp.ui.components.FullScreenError
import com.ahsan.movieapp.ui.components.FullScreenLoading
import com.ahsan.movieapp.ui.components.MoviePosterCard
import com.ahsan.movieapp.ui.components.TrailerShareRow
import com.ahsan.movieapp.ui.components.UrlAutoSizeText
import com.ahsan.movieapp.ui.components.backgroundSwatch
import com.ahsan.movieapp.ui.components.rememberBackdropPalette

/**
 * Detail screen: a big full-width portrait poster hero (PersonHero-style, 2026-09-22) with the
 * title/tagline/rating/year/runtime/genres overlaid on its bottom edge via the shared
 * [DetailHeroCaption] — the backdrop is gone and the poster is the hero; then the Overview section
 * directly below it with the Watch Trailer + Share row (Phase 2.6 Session 2) at the end of that
 * section — repositioned on Ahsan's 2026-09-12 post-ship feedback (originally a separate full-width
 * row below, mirroring [com.ahsan.movieapp.ui.tv.TvDetailScreen]'s identical move); then
 * cast/crew (Phase 3 Round A), Information/Similar/Recommendations (added the same round on Ahsan's
 * post-build feedback), a collection teaser (Round B), and streaming availability (Round C).
 *
 * Transparent top bar over the hero — the PersonScreen treatment: a pinned title + normal
 * back arrow floating over a short gradient scrim, with the hero bleeding edge-to-edge behind
 * it (contentWindowInsets zeroed, content padding discarded). Same dynamic-per-screen pattern as
 * PersonScreen/GenreScreen. The generic "Movie App" bar the root tabs share is hidden entirely here
 * (see MovieNavGraph's TOP_LEVEL_ROUTES check); everything below this bar scrolls as one column.
 */
@OptIn(ExperimentalMaterial3Api::class)
// contentWindowInsets is deliberately (0,0,0,0) below (see the comment on that param) and the
// Scaffold's content padding is deliberately discarded as `_`. With a topBar present, Material3's
// Scaffold sets its top value to the topBar's measured height, so applying it would push the
// backdrop down past the transparent bar instead of behind it. The lint check can't tell
// "deliberately unused" from "forgot to apply it", hence the suppress.
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MovieDetailScreen(
    onBack: () -> Unit,
    onPersonClick: (personId: Int, personName: String) -> Unit,
    onViewAllCastCrew: (movieId: Int) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onCollectionClick: (collectionId: Int, collectionName: String) -> Unit,
    onWatchTrailer: (videoId: String) -> Unit,
    viewModel: MovieDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // The hero is now the poster itself (2026-09-22, PersonHero-style), so the palette comes from
    // the poster — the tinted background continues its colors, not the discarded backdrop's.
    val posterUrl = state.details?.posterUrl
    val palette = rememberBackdropPalette(posterUrl)
    val posterTone = palette?.backgroundSwatch?.rgb?.let { Color(it) }
    // Bright posters get a stronger scrim behind the transparent bar so the white title/back stay
    // legible; dark ones keep the lighter scrim. Defaults to a dark tone (0.2 luminance) so the
    // scrim never disappears when there's no sampled color (loading/error states).
    val scrimAlpha = (0.5f + 0.25f * (posterTone?.luminance() ?: 0.2f)).coerceIn(0.5f, 0.9f)
    // The screen background is the theme background tinted toward the poster's color, so the
    // sections below the hero continue the image's palette instead of a flat theme color. The
    // theme tint still rules — palette nudges it about a third of the way.
    val background = posterTone?.let { lerp(MaterialTheme.colorScheme.background, it, 0.35f) }
        ?: MaterialTheme.colorScheme.background

    Scaffold(
        containerColor = background,
        topBar = {
            // 100dp of scroll is safely past the hero; ScrollState.value is snapshot-backed, so
            // reading it here recomposes only this lambda as the page scrolls. Loading/error have
            // no hero behind the bar, so they default to the opaque titled bar.
            val barOpaque = scrollState.value > with(LocalDensity.current) { 100.dp.toPx() } ||
                state.isLoading || state.details == null
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
                            Text(state.details?.title.orEmpty(), color = barContentColor)
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
        // Zero content insets — paired with the transparent bar above, this lets the hero poster
        // (and, in the loading/error states, the plain background) run all the way to the top of
        // the screen instead of stopping below a reserved app-bar-height gap. Same treatment as
        // PersonScreen.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            state.details?.let { details ->
                FloatingActionButton(
                    onClick = { viewModel.toggleFavorite() },
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(
                        imageVector = if (details.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null
                    )
                }
            }
        }
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> FullScreenLoading()
                state.details == null -> FullScreenError(message = state.errorMessage ?: stringResource(R.string.detail_couldnt_load_movie))
                else -> {
                    val details = state.details!!
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                        // Big portrait poster hero (PersonHero-style, 2026-09-22): the 2:3 poster
                        // fills a full-width 3:4 hero and carries the title/tagline/rating/year/
                        // runtime/genres caption on its bottom edge (DetailHeroCaption) — no more
                        // backdrop, no more small overlapping poster + side info column.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(3f / 4f)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            AsyncImage(
                                model = details.posterUrl,
                                contentDescription = details.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            DetailHeroCaption(
                                title = details.title,
                                tagline = details.tagline,
                                rating = details.ratingOutOfTen,
                                metaLabels = buildList {
                                    add(details.releaseYear)
                                    details.runtimeFormatted?.let { add(it) }
                                },
                                genres = details.genres,
                                showMetaDiamond = true
                            )
                        }

                        // Overview sits directly below the hero poster, per 2026-09-22 feedback; the
                        // Watch Trailer + Share row lives at the end of this section.
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = stringResource(R.string.overview_title), style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = details.overview.ifBlank { stringResource(R.string.detail_no_description) },
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            TrailerShareRow(
                                trailerKey = state.trailerKey,
                                shareTitle = details.title,
                                shareUrl = "https://www.themoviedb.org/movie/${details.id}",
                                onWatchTrailer = onWatchTrailer,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }

                        if (state.cast.isNotEmpty()) {
                            CastCrewSection(
                                cast = state.cast,
                                onPersonClick = onPersonClick,
                                onViewAll = { onViewAllCastCrew(details.id) }
                            )
                        }

                        InformationSection(details)

                        details.collection?.let { collection ->
                            CollectionTeaser(
                                collection = collection,
                                onClick = { onCollectionClick(collection.id, collection.name) }
                            )
                        }

                        if (state.watchRegions.isNotEmpty()) {
                            WatchProvidersSection(
                                region = state.watchProviders,
                                availableRegions = state.watchRegions,
                                selectedRegion = state.selectedRegion,
                                onRegionChange = { viewModel.setWatchRegion(it) }
                            )
                        }

                        if (state.moreLikeThis.isNotEmpty()) {
                            PosterRowSection(movies = state.moreLikeThis, onMovieClick = onMovieClick)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Key facts about the movie — Original Title, Status, Release Date, Runtime, Countries,
 * Production Companies (names only, comma-separated — no logos), Budget, Revenue, Website, in
 * that order per Ahsan's request. Every row is individually optional: a null/blank/zero value
 * simply doesn't render its row, rather than showing an empty or "$0" value. Renders nothing at
 * all if every field is absent. The Website row uses the same "Label / value" [InfoRow] layout as
 * every other field here — labeled "Website", value = the raw URL as plain (but clickable-styled)
 * text — rather than a standalone icon+link row, so it reads as one consistent list.
 */
@Composable
private fun InformationSection(details: MovieDetails) {
    val website = details.homepageIfPresent

    val rows = buildList {
        details.originalTitleIfPresent?.let { add(stringResource(R.string.detail_info_original_title) to it) }
        details.statusIfPresent?.let { add(stringResource(R.string.detail_info_status) to it) }
        details.releaseDate.takeIf { it.isNotBlank() }?.let { add(stringResource(R.string.detail_info_release_date) to it) }
        details.runtimeFormatted?.let { add(stringResource(R.string.detail_info_runtime) to it) }
        details.countriesFormatted?.let { add(stringResource(R.string.detail_info_countries) to it) }
        details.productionCompaniesFormatted?.let { add(stringResource(R.string.detail_info_production_companies) to it) }
        details.budgetFormatted?.let { add(stringResource(R.string.detail_info_budget) to it) }
        details.revenueFormatted?.let { add(stringResource(R.string.detail_info_revenue) to it) }
    }

    if (rows.isEmpty() && website == null) return

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text = stringResource(R.string.detail_information), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))

        rows.forEach { (label, value) -> InfoRow(label = label, value = value) }

        if (website != null) {
            val uriHandler = LocalUriHandler.current
            // The Website value is the one row whose value can be a long unbroken URL — run it
            // through UrlAutoSizeText so it shrinks to fit the row instead of clipping.
            InfoRow(label = stringResource(R.string.detail_info_website), value = website, onClick = { uriHandler.openUri(website) }, autoShrink = true)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, onClick: (() -> Unit)? = null, autoShrink: Boolean = false) {
    Row(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(140.dp)
        )
        if (autoShrink) {
            UrlAutoSizeText(
                url = value,
                color = if (onClick != null) MaterialTheme.colorScheme.primary else Color.Unspecified,
                modifier = Modifier.weight(1f)
            )
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (onClick != null) MaterialTheme.colorScheme.primary else Color.Unspecified,
                textDecoration = if (onClick != null) TextDecoration.Underline else null,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Phase 3 Round B's compact "part of a collection" teaser — poster thumbnail + collection name,
 * tappable through to a new full-screen [com.ahsan.movieapp.ui.detail.CollectionScreen] listing
 * every movie in the franchise. Only rendered when [MovieDetails.collection] is non-null (most
 * movies aren't part of one). Sits directly below Information, since it's another "key fact" about
 * the movie, and above Similar/Recommendations.
 */
@Composable
private fun CollectionTeaser(collection: CollectionSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(56.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            AsyncImage(
                model = collection.posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(
                text = stringResource(R.string.detail_part_of_collection),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = collection.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.detail_view_collection))
    }
}

/**
 * Phase 3 Round C's streaming-availability section — grouped Stream/Rent/Buy provider-logo rows
 * for one TMDB region, with a region dropdown to switch (locale-derived default, US fallback —
 * see [MovieDetailViewModel]'s region-resolution logic). Switching regions is instant/local, not
 * a re-fetch — TMDB already returned every region in one call. The caller only renders this
 * composable when [availableRegions] is non-empty (TMDB has data for at least one region); if the
 * currently *selected* region has none, the dropdown still shows (so the user can pick one that
 * does) with a short "not available" line in place of the provider rows.
 *
 * TMDB's `/watch/providers` response doesn't hand back a deep link per provider — only one
 * `link` per region, to TMDB's own watch-providers page for this movie (which forwards through
 * JustWatch to the actual services). So every logo in this region, plus the attribution line
 * itself, opens that same [WatchProviderRegion.link] — that's also what TMDB's API terms require
 * showing as a real, working link whenever this data is displayed, not just decorative text.
 */
@Composable
private fun WatchProvidersSection(
    region: WatchProviderRegion?,
    availableRegions: List<String>,
    selectedRegion: String,
    onRegionChange: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.detail_where_to_watch), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            RegionDropdown(selectedRegion = selectedRegion, availableRegions = availableRegions, onRegionChange = onRegionChange)
        }

        if (region == null || region.isEmpty) {
            Text(
                text = stringResource(R.string.detail_not_available_in_region, selectedRegion),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            val uriHandler = LocalUriHandler.current
            val onOpenProviders: (() -> Unit)? = region.link
                ?.takeIf { it.isNotBlank() }
                ?.let { link -> { uriHandler.openUri(link) } }

            ProviderRow(label = stringResource(R.string.detail_provider_stream), providers = region.flatrate, onProviderClick = onOpenProviders)
            ProviderRow(label = stringResource(R.string.detail_provider_rent), providers = region.rent, onProviderClick = onOpenProviders)
            ProviderRow(label = stringResource(R.string.detail_provider_buy), providers = region.buy, onProviderClick = onOpenProviders)
            Text(
                text = stringResource(R.string.detail_provider_attribution),
                style = MaterialTheme.typography.labelSmall,
                color = if (onOpenProviders != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                textDecoration = if (onOpenProviders != null) TextDecoration.Underline else null,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .then(if (onOpenProviders != null) Modifier.clickable(onClick = onOpenProviders) else Modifier)
            )
        }
    }
}

/** A compact chip + dropdown menu of every region TMDB has watch-provider data for, ISO codes as-is (e.g. "US", "GB"). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegionDropdown(selectedRegion: String, availableRegions: List<String>, onRegionChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(selectedRegion) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = stringResource(R.string.detail_change_region)) }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            availableRegions.forEach { code ->
                DropdownMenuItem(
                    text = { Text(code) },
                    onClick = {
                        onRegionChange(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** One Stream/Rent/Buy row — renders nothing if this region has no providers for that category. */
@Composable
private fun ProviderRow(label: String, providers: List<WatchProvider>, onProviderClick: (() -> Unit)?) {
    if (providers.isEmpty()) return
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(
            contentPadding = PaddingValues(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(providers, key = { it.id }) { provider ->
                ProviderLogo(provider = provider, onClick = onProviderClick)
            }
        }
    }
}

/**
 * TMDB doesn't hand back a per-provider deep link (see [WatchProvidersSection]'s doc comment), so
 * every logo shares the same [onClick] — the region's one TMDB watch-providers link — same
 * nullable-callback convention as [InfoRow]/[CollectionTeaser] elsewhere on this screen.
 */
@Composable
private fun ProviderLogo(provider: WatchProvider, onClick: (() -> Unit)?) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        AsyncImage(
            model = provider.logoUrl,
            contentDescription = provider.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Layout for the "More Like This" shelf (Review-queue item 2 → option A): a heading followed by a
 * uniform horizontal poster row at 144dp, reusing [MoviePosterCard] like every other carousel in
 * the app. Option B (spotlight + queue) was tried here but Ahsan's intent was the Person screen's
 * filmography — this shelf stays in the app's standard row language. The shelf shows
 * Recommendations only (Similar dropped 2026-09-20: not relevant enough); title unchanged from
 * Review-queue item 1 → option C.
 */
@Composable
private fun PosterRowSection(movies: List<Movie>, onMovieClick: (Movie) -> Unit) {
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) {
        Text(
            text = stringResource(R.string.similar_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(movies, key = { it.id }) { movie ->
                MoviePosterCard(movie = movie, onClick = { onMovieClick(movie) }, onToggleFavorite = null, width = 144.dp)
            }
        }
    }
}

/**
 * Phase 3 Round A's cast/crew section — sits directly below the Overview. A "view all" arrow next
 * to the heading opens [com.ahsan.movieapp.ui.detail.CastCrewListScreen] with the full cast (this
 * row only shows the first [CAST_ROW_LIMIT]). Cast only on the detail itself — the director is
 * deliberately not duplicated here since the cast & crew list (where the director is shown) is one
 * arrow-tap away.
 */
@Composable
private fun CastCrewSection(
    cast: List<CastMember>,
    onPersonClick: (personId: Int, personName: String) -> Unit,
    onViewAll: () -> Unit
) {
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.castcrew_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onViewAll) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.castcrew_view_all))
            }
        }

        if (cast.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(cast.take(CAST_ROW_LIMIT), key = { it.id }) { member ->
                    CastMemberCard(member = member, onClick = { onPersonClick(member.id, member.name) })
                }
            }
        }
    }
}

private const val CAST_ROW_LIMIT = 15
