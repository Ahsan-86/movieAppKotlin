package com.ahsan.movieapp.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ahsan.movieapp.domain.model.CastMember
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.domain.model.Person
import com.ahsan.movieapp.domain.model.Season
import com.ahsan.movieapp.domain.model.TvShowDetails
import com.ahsan.movieapp.ui.components.FullScreenError
import com.ahsan.movieapp.ui.components.FullScreenLoading
import com.ahsan.movieapp.ui.components.MoviePosterCard
import com.ahsan.movieapp.ui.components.TrailerShareRow

/**
 * Phase 2.6's TV detail screen: backdrop, poster, key facts, genres, then a Watch Trailer + Share
 * row (Session 2), all inside the same title/meta info column beside the poster — repositioned
 * there on Ahsan's 2026-09-12 post-ship feedback (originally a separate full-width row below that
 * column); then overview, Cast & Crew, a collapsible Seasons section (Session 2, collapsed by
 * default per the same feedback round), Information, Similar, and Recommendations sections
 * (Session 1, following the same layout pattern as [com.ahsan.movieapp.ui.detail.MovieDetailScreen]
 * — the last three added on Ahsan's post-build feedback, "where is information and similar and
 * recommendation sections?", after the initial narrower build shipped). Still deliberately excludes
 * what these rounds don't cover: no favorite toggle (needs Session 6's Favorites schema migration),
 * no collection-teaser/streaming-availability sections (movie-specific — TV has no TMDB "collection"
 * concept and no round has extended Round C's watch-providers work to TV), and no "view all cast &
 * crew" sub-screen — this screen shows the same capped cast row
 * [com.ahsan.movieapp.ui.detail.MovieDetailScreen] does, with nowhere further to drill into for
 * cast yet.
 *
 * Same full-screen pattern as every other detail-type screen (Movie/Person/Genre): its own
 * Scaffold + dynamic TopAppBar with a real back button, gated out of `MovieNavGraph`'s
 * `TOP_LEVEL_ROUTES` set, no bottom nav. [onTvClick] lets Similar/Recommendations posters push
 * another TV detail screen onto the back stack (TV -> Similar -> Similar chains the same way
 * Movie -> Similar -> Similar already does); [onSeasonClick] opens
 * [com.ahsan.movieapp.ui.tv.SeasonEpisodesScreen] for a tapped season; [onWatchTrailer] opens
 * [com.ahsan.movieapp.ui.components.TrailerPlayerScreen] for the trailer key
 * [com.ahsan.movieapp.ui.components.TrailerShareRow] passes it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvDetailScreen(
    onBack: () -> Unit,
    onPersonClick: (personId: Int, personName: String) -> Unit,
    onTvClick: (Movie) -> Unit,
    onSeasonClick: (tvId: Int, seasonNumber: Int, seasonName: String) -> Unit,
    onWatchTrailer: (videoId: String) -> Unit,
    viewModel: TvDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.details?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isLoading -> FullScreenLoading()
                state.details == null -> FullScreenError(message = state.errorMessage ?: "Couldn't load this show")
                else -> {
                    val details = state.details!!
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        AsyncImage(
                            model = details.backdropUrl ?: details.posterUrl,
                            contentDescription = details.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 10f)
                        )

                        Row(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().offsetUp()) {
                            AsyncImage(
                                model = details.posterUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(110.dp)
                                    .aspectRatio(2f / 3f)
                                    .clip(RoundedCornerShape(12.dp))
                            )

                            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                                Text(text = details.name, style = MaterialTheme.typography.headlineMedium)
                                if (!details.tagline.isNullOrBlank()) {
                                    Text(
                                        text = "“${details.tagline}”",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(
                                    modifier = Modifier.padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    TvMetaChip(icon = Icons.Filled.Star, text = details.ratingOutOfTen)
                                    Text(text = details.releaseYear, style = MaterialTheme.typography.labelLarge)
                                    details.seasonsFormatted?.let {
                                        Text(text = it, style = MaterialTheme.typography.labelLarge)
                                    }
                                    details.episodeRuntimeFormatted?.let {
                                        Text(text = it, style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                                if (details.genres.isNotEmpty()) {
                                    Text(
                                        text = details.genres.joinToString(" • "),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                TrailerShareRow(
                                    trailerKey = state.trailerKey,
                                    shareTitle = details.name,
                                    shareUrl = "https://www.themoviedb.org/tv/${details.id}",
                                    onWatchTrailer = onWatchTrailer,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Overview", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = details.overview.ifBlank { "No description available." },
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        if (state.cast.isNotEmpty() || state.director != null) {
                            TvCastCrewSection(
                                cast = state.cast,
                                director = state.director,
                                onPersonClick = onPersonClick
                            )
                        }

                        if (details.seasons.isNotEmpty()) {
                            TvSeasonsSection(
                                seasons = details.seasons,
                                onSeasonClick = { season -> onSeasonClick(details.id, season.seasonNumber, season.name) }
                            )
                        }

                        TvInformationSection(details)

                        if (state.similarTvShows.isNotEmpty()) {
                            TvPosterRowSection(title = "Similar", shows = state.similarTvShows, onTvClick = onTvClick)
                        }

                        if (state.recommendedTvShows.isNotEmpty()) {
                            TvPosterRowSection(title = "Recommendations", shows = state.recommendedTvShows, onTvClick = onTvClick)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

/**
 * Same shape as [com.ahsan.movieapp.ui.detail.MovieDetailScreen]'s cast/crew section, minus the
 * "view all" arrow — there's nowhere to drill into yet for TV cast (see the screen doc above).
 */
@Composable
private fun TvCastCrewSection(
    cast: List<CastMember>,
    director: Person?,
    onPersonClick: (personId: Int, personName: String) -> Unit
) {
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) {
        Text(
            text = "Cast & Crew",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (director != null) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { onPersonClick(director.id, director.name) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Director",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(text = director.name, style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (cast.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(cast.take(TV_CAST_ROW_LIMIT), key = { it.id }) { member ->
                    TvCastMemberChip(member = member, onClick = { onPersonClick(member.id, member.name) })
                }
            }
        }
    }
}

private const val TV_CAST_ROW_LIMIT = 15

@Composable
private fun TvCastMemberChip(member: CastMember, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(84.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (member.profileUrl != null) {
                AsyncImage(
                    model = member.profileUrl,
                    contentDescription = member.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = member.name,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                )
            }
        }
        Text(
            text = member.name,
            style = MaterialTheme.typography.labelMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
        if (member.character.isNotBlank()) {
            Text(
                text = member.character,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun TvMetaChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.height(16.dp))
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

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
                    text = "Seasons",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${seasons.size} Season${if (seasons.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse seasons" else "Expand seasons"
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
        details.originalNameIfPresent?.let { add("Original Name" to it) }
        details.statusIfPresent?.let { add("Status" to it) }
        details.firstAirDate.takeIf { it.isNotBlank() }?.let { add("First Air Date" to it) }
        details.episodeRuntimeFormatted?.let { add("Episode Runtime" to it) }
        details.seasonsFormatted?.let { add("Seasons" to it) }
        details.numberOfEpisodes?.takeIf { it > 0 }?.let { add("Episodes" to it.toString()) }
        details.countriesFormatted?.let { add("Countries" to it) }
        details.networksFormatted?.let { add("Networks" to it) }
        details.productionCompaniesFormatted?.let { add("Production Companies" to it) }
    }

    if (rows.isEmpty() && website == null) return

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text = "Information", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))

        rows.forEach { (label, value) -> TvInfoRow(label = label, value = value) }

        if (website != null) {
            val uriHandler = LocalUriHandler.current
            TvInfoRow(label = "Website", value = website, onClick = { uriHandler.openUri(website) })
        }
    }
}

@Composable
private fun TvInfoRow(label: String, value: String, onClick: (() -> Unit)? = null) {
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

/**
 * Shared layout for the Similar and Recommendations sections — same shape as
 * [com.ahsan.movieapp.ui.detail.MovieDetailScreen]'s `PosterRowSection`, reusing [MoviePosterCard]
 * (TV shows are modeled as [Movie] throughout this app — see util/TvNavigation.kt). The two
 * sections are deliberately separate lists/API calls (see
 * [com.ahsan.movieapp.data.repository.MovieRepository.getSimilarTvShows] vs
 * [com.ahsan.movieapp.data.repository.MovieRepository.getRecommendedTvShows]) and are not deduped
 * against each other.
 */
@Composable
private fun TvPosterRowSection(title: String, shows: List<Movie>, onTvClick: (Movie) -> Unit) {
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(shows, key = { it.id }) { show ->
                MoviePosterCard(movie = show, onClick = { onTvClick(show) }, onToggleFavorite = null, width = 128.dp)
            }
        }
    }
}

private fun Modifier.offsetUp(): Modifier = this.offset(y = (-40).dp)
