package com.ahsan.movieapp.ui.tv

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.domain.model.Season
import com.ahsan.movieapp.domain.model.TvShowDetails
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
 * Phase 2.6's TV detail screen: a big full-width portrait poster hero (PersonHero-style,
 * 2026-09-22) with the title/tagline/rating/year/seasons·episode runtime/genres overlaid on its
 * bottom edge via the shared [DetailHeroCaption] — the backdrop is gone and the poster is the hero;
 * then the Overview section directly below it with the Watch Trailer + Share row (Session 2) at the
 * end of that section — repositioned on Ahsan's 2026-09-12 post-ship feedback (originally a
 * separate full-width row below, mirroring [com.ahsan.movieapp.ui.detail.MovieDetailScreen]'s
 * identical move); then Cast & Crew, a collapsible Seasons section (Session 2, collapsed by default
 * per the same feedback round), Information, Similar, and Recommendations sections (Session 1,
 * following the same layout pattern as [com.ahsan.movieapp.ui.detail.MovieDetailScreen] — the last
 * three added on Ahsan's post-build feedback, "where is information and similar and recommendation
 * sections?", after the initial narrower build shipped). Still deliberately excludes what these
 * rounds don't cover: no favorite toggle (needs Session 6's Favorites schema migration), and no
 * collection-teaser/streaming-availability sections (movie-specific — TV has no TMDB "collection"
 * concept and no round has extended Round C's watch-providers work to TV). The Cast & Crew heading
 * carries the same "view all" arrow as [com.ahsan.movieapp.ui.detail.MovieDetailScreen]'s, opening
 * the media-agnostic cast & crew list for this show's full cast + director.
 *
 * Same full-screen pattern as every other detail-type screen (Movie/Person/Genre): its own
 * Scaffold + transparent TopAppBar over a short gradient scrim (the PersonScreen treatment — the
 * hero poster bleeds edge-to-edge behind the bar, contentWindowInsets zeroed), with a real back
 * button, gated out of `MovieNavGraph`'s
 * `TOP_LEVEL_ROUTES` set, no bottom nav. [onTvClick] lets Similar/Recommendations posters push
 * another TV detail screen onto the back stack (TV -> Similar -> Similar chains the same way
 * Movie -> Similar -> Similar already does); [onSeasonClick] opens
 * [com.ahsan.movieapp.ui.tv.SeasonEpisodesScreen] for a tapped season; [onViewAllCastCrew] opens
 * the full cast & crew list for this show; [onWatchTrailer] opens
 * [com.ahsan.movieapp.ui.components.TrailerPlayerScreen] for the trailer key
 * [com.ahsan.movieapp.ui.components.TrailerShareRow] passes it.
 */
@OptIn(ExperimentalMaterial3Api::class)
// contentWindowInsets is deliberately (0,0,0,0) below (see the comment on that param) and the
// Scaffold's content padding is deliberately discarded as `_`. With a topBar present, Material3's
// Scaffold sets its top value to the topBar's measured height, so applying it would push the
// backdrop down past the transparent bar instead of behind it. The lint check can't tell
// "deliberately unused" from "forgot to apply it", hence the suppress.
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun TvDetailScreen(
    onBack: () -> Unit,
    onPersonClick: (personId: Int, personName: String) -> Unit,
    onTvClick: (Movie) -> Unit,
    onSeasonClick: (tvId: Int, seasonNumber: Int, seasonName: String) -> Unit,
    onViewAllCastCrew: (tvId: Int) -> Unit,
    onWatchTrailer: (videoId: String) -> Unit,
    viewModel: TvDetailViewModel = hiltViewModel()
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
                            Text(state.details?.name.orEmpty(), color = barContentColor)
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> FullScreenLoading()
                state.details == null -> FullScreenError(message = state.errorMessage ?: stringResource(R.string.tv_couldnt_load_show))
                else -> {
                    val details = state.details!!
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                        // Big portrait poster hero (PersonHero-style, 2026-09-22): the 2:3 poster
                        // fills a full-width 3:4 hero and carries the title/tagline/rating/year/
                        // seasons·episode-runtime/genres caption on its bottom edge (DetailHeroCaption)
                        // — no more backdrop, no more small overlapping poster + side info column.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(3f / 4f)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            AsyncImage(
                                model = details.posterUrl,
                                contentDescription = details.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            DetailHeroCaption(
                                title = details.name,
                                tagline = details.tagline,
                                rating = details.ratingOutOfTen,
                                metaLabels = buildList {
                                    add(details.releaseYear)
                                    details.seasonsFormatted?.let { add(it) }
                                    details.episodeRuntimeFormatted?.let { add(it) }
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
                                shareTitle = details.name,
                                shareUrl = "https://www.themoviedb.org/tv/${details.id}",
                                onWatchTrailer = onWatchTrailer,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }

                        if (state.cast.isNotEmpty()) {
                            TvCastCrewSection(
                                cast = state.cast,
                                onPersonClick = onPersonClick,
                                onViewAll = { onViewAllCastCrew(details.id) }
                            )
                        }

                        if (details.seasons.isNotEmpty()) {
                            TvSeasonsSection(
                                seasons = details.seasons,
                                onSeasonClick = { season -> onSeasonClick(details.id, season.seasonNumber, season.name) }
                            )
                        }

                        TvInformationSection(details)

                        if (state.moreLikeThis.isNotEmpty()) {
                            TvPosterRowSection(shows = state.moreLikeThis, onTvClick = onTvClick)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

/**
 * Same shape as [com.ahsan.movieapp.ui.detail.MovieDetailScreen]'s cast/crew section — a "view
 * all" arrow next to the heading opens [com.ahsan.movieapp.ui.detail.CastCrewListScreen] with the
 * full cast plus the director, mirroring the movie screen's navigation.
 */
@Composable
private fun TvCastCrewSection(
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

        Spacer(modifier = Modifier.height(8.dp))

        if (cast.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(cast.take(TV_CAST_ROW_LIMIT), key = { it.id }) { member ->
                    CastMemberCard(member = member, onClick = { onPersonClick(member.id, member.name) })
                }
            }
        }
    }
}

private const val TV_CAST_ROW_LIMIT = 15

/**
 * Phase 2.6 Session 2 — every season of the show, each with its poster image, name, air year,
 * episode count, and a short overview snippet. Tapping a season opens
 * [com.ahsan.movieapp.ui.tv.SeasonEpisodesScreen] for that season's full episode list. A plain
 * `Column` + `forEach`, not a `LazyColumn` — this section already lives inside
 * [TvDetailScreen]'s outer `.verticalScroll()` Column, and nesting a lazy list inside another
 * scrolling container is the project's standing lazy-inside-scrolling-parent pitfall. A show's
 * season count is always small (TMDB shows rarely exceed a few dozen), so this is safe.
 *
 * Collapsed by default (Ahsan's 2026-09-12 post-ship feedback), and the collapsed heading is
 * deliberately styled as the same rounded-card row as
 * [com.ahsan.movieapp.ui.detail.MovieDetailScreen]'s "Part of a collection" teaser (poster
 * thumbnail + small label/bold-value text pair on a `surfaceVariant` card) rather than a plain
 * section title — a second round of the same feedback, after an initial plain-heading version.
 * The whole card is the expand/collapse toggle (an `ExpandMore`/`ExpandLess` icon takes the
 * teaser's arrow-icon slot); the thumbnail is simply the first season's poster, since there's no
 * single "the" season image the way a collection has one poster. Tapping an individual season row
 * (once expanded) still opens its episode list exactly as before — the collapse/expand state is
 * purely about this section's own list, unrelated to [onSeasonClick].
 */
@Composable
private fun TvSeasonsSection(seasons: List<Season>, onSeasonClick: (Season) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { expanded = !expanded }
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
                    model = seasons.firstOrNull()?.posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    text = stringResource(R.string.tv_seasons),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (seasons.size == 1) {
                        stringResource(R.string.tv_season_count_one, seasons.size)
                    } else {
                        stringResource(R.string.tv_season_count_other, seasons.size)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) stringResource(R.string.tv_collapse_seasons) else stringResource(R.string.tv_expand_seasons)
            )
        }
        if (expanded) {
            seasons.forEach { season ->
                TvSeasonRow(season = season, onClick = { onSeasonClick(season) })
            }
        }
    }
}

@Composable
private fun TvSeasonRow(season: Season, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(90.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = season.posterUrl,
                contentDescription = season.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(text = season.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 2.dp)) {
                season.yearFormatted?.let {
                    Text(text = it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                season.episodeCountFormatted?.let {
                    Text(text = it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (season.overview.isNotBlank()) {
                Text(
                    text = season.overview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/**
 * Key facts about the show — Original Name, Status, First Air Date, Episode Runtime, Number of
 * Seasons/Episodes, Countries, Networks, Production Companies (names only, comma-separated — no
 * logos), Website. Same InfoRow-style layout and "every row individually optional, renders
 * nothing if all absent" behavior as
 * com.ahsan.movieapp.ui.detail.MovieDetailScreen's InformationSection, with TV-appropriate
 * fields substituted for the movie-only Budget/Revenue rows. Added on Ahsan's post-build feedback.
 */
@Composable
private fun TvInformationSection(details: TvShowDetails) {
    val website = details.homepageIfPresent

    val rows = buildList {
        details.originalNameIfPresent?.let { add(stringResource(R.string.tv_info_original_name) to it) }
        details.statusIfPresent?.let { add(stringResource(R.string.detail_info_status) to it) }
        details.firstAirDate.takeIf { it.isNotBlank() }?.let { add(stringResource(R.string.tv_info_first_air_date) to it) }
        details.episodeRuntimeFormatted?.let { add(stringResource(R.string.tv_info_episode_runtime) to it) }
        details.seasonsFormatted?.let { add(stringResource(R.string.tv_seasons) to it) }
        details.numberOfEpisodes?.takeIf { it > 0 }?.let { add(stringResource(R.string.tv_episodes) to it.toString()) }
        details.countriesFormatted?.let { add(stringResource(R.string.detail_info_countries) to it) }
        details.networksFormatted?.let { add(stringResource(R.string.tv_info_networks) to it) }
        details.productionCompaniesFormatted?.let { add(stringResource(R.string.detail_info_production_companies) to it) }
    }

    if (rows.isEmpty() && website == null) return

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text = stringResource(R.string.detail_information), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))

        rows.forEach { (label, value) -> TvInfoRow(label = label, value = value) }

        if (website != null) {
            val uriHandler = LocalUriHandler.current
            // The Website value is the one row whose value can be a long unbroken URL — run it
            // through UrlAutoSizeText so it shrinks to fit the row instead of clipping.
            TvInfoRow(label = stringResource(R.string.detail_info_website), value = website, onClick = { uriHandler.openUri(website) }, autoShrink = true)
        }
    }
}

@Composable
private fun TvInfoRow(label: String, value: String, onClick: (() -> Unit)? = null, autoShrink: Boolean = false) {
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
 * Layout for the "More Like This" shelf (Review-queue item 2 → option A) — same shape as
 * [com.ahsan.movieapp.ui.detail.MovieDetailScreen]'s `PosterRowSection`, reusing [MoviePosterCard]
 * (TV shows are modeled as [Movie] throughout this app — their id is a TV id, not a movie id): a
 * uniform 144dp horizontal row. Option B (spotlight + queue) was tried here but Ahsan's intent was
 * the Person screen's filmography — this shelf stays in the standard row language. The shelf shows
 * Recommendations only (Similar dropped 2026-09-20: not relevant enough); title unchanged from
 * Review-queue item 1 → option C.
 */
@Composable
private fun TvPosterRowSection(shows: List<Movie>, onTvClick: (Movie) -> Unit) {
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
            items(shows, key = { it.id }) { show ->
                MoviePosterCard(movie = show, onClick = { onTvClick(show) }, onToggleFavorite = null, width = 144.dp)
            }
        }
    }
}
