package com.ahsan.movieapp.ui.detail

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
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
import com.ahsan.movieapp.domain.model.CollectionSummary
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.domain.model.MovieDetails
import com.ahsan.movieapp.domain.model.WatchProvider
import com.ahsan.movieapp.domain.model.WatchProviderRegion
import com.ahsan.movieapp.ui.components.FullScreenError
import com.ahsan.movieapp.ui.components.FullScreenLoading
import com.ahsan.movieapp.ui.components.MoviePosterCard
import com.ahsan.movieapp.ui.components.TrailerShareRow

/**
 * Detail screen: backdrop, poster, key facts, genres, favorite toggle, then a Watch Trailer + Share
 * row (Phase 2.6 Session 2) inside that same title/meta/genre info column beside the poster —
 * repositioned there on Ahsan's 2026-09-12 post-ship feedback (originally a separate full-width row
 * below that column, mirroring [com.ahsan.movieapp.ui.tv.TvDetailScreen]'s identical move); then
 * overview, cast/crew (Phase 3 Round A), Information/Similar/Recommendations (added the same round
 * on Ahsan's post-build feedback), a collection teaser (Round B), and streaming availability
 * (Round C).
 *
 * Real, pinned top bar (title = the movie's name, a normal back arrow) rather than a back button
 * floating over the backdrop — same dynamic-per-screen pattern as PersonScreen/GenreScreen. The
 * generic "Movie App" bar the root tabs share is hidden entirely here (see MovieNavGraph's
 * TOP_LEVEL_ROUTES check); everything below this bar scrolls as one column.
 */
@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.details?.title.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
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
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isLoading -> FullScreenLoading()
                state.details == null -> FullScreenError(message = state.errorMessage ?: "Couldn't load this movie")
                else -> {
                    val details = state.details!!
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        AsyncImage(
                            model = details.backdropUrl ?: details.posterUrl,
                            contentDescription = details.title,
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
                                Text(text = details.title, style = MaterialTheme.typography.headlineMedium)
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
                                    MetaChip(icon = Icons.Filled.Star, text = details.ratingOutOfTen)
                                    Text(text = details.releaseYear, style = MaterialTheme.typography.labelLarge)
                                    details.runtimeFormatted?.let {
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
                                    shareTitle = details.title,
                                    shareUrl = "https://www.themoviedb.org/movie/${details.id}",
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
                            CastCrewSection(
                                cast = state.cast,
                                director = state.director,
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

                        if (state.similarMovies.isNotEmpty()) {
                            PosterRowSection(title = "Similar", movies = state.similarMovies, onMovieClick = onMovieClick)
                        }

                        if (state.recommendedMovies.isNotEmpty()) {
                            PosterRowSection(title = "Recommendations", movies = state.recommendedMovies, onMovieClick = onMovieClick)
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
        details.originalTitleIfPresent?.let { add("Original Title" to it) }
        details.statusIfPresent?.let { add("Status" to it) }
        details.releaseDate.takeIf { it.isNotBlank() }?.let { add("Release Date" to it) }
        details.runtimeFormatted?.let { add("Runtime" to it) }
        details.countriesFormatted?.let { add("Countries" to it) }
        details.productionCompaniesFormatted?.let { add("Production Companies" to it) }
        details.budgetFormatted?.let { add("Budget" to it) }
        details.revenueFormatted?.let { add("Revenue" to it) }
    }

    if (rows.isEmpty() && website == null) return

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text = "Information", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))

        rows.forEach { (label, value) -> InfoRow(label = label, value = value) }

        if (website != null) {
            val uriHandler = LocalUriHandler.current
            InfoRow(label = "Website", value = website, onClick = { uriHandler.openUri(website) })
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, onClick: (() -> Unit)? = null) {
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
                text = "Part of a collection",
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
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "View collection")
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
            Text(text = "Where to Watch", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            RegionDropdown(selectedRegion = selectedRegion, availableRegions = availableRegions, onRegionChange = onRegionChange)
        }

        if (region == null || region.isEmpty) {
            Text(
                text = "Not available to stream, rent, or buy in $selectedRegion.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            val uriHandler = LocalUriHandler.current
            val onOpenProviders: (() -> Unit)? = region.link
                ?.takeIf { it.isNotBlank() }
                ?.let { link -> { uriHandler.openUri(link) } }

            ProviderRow(label = "Stream", providers = region.flatrate, onProviderClick = onOpenProviders)
            ProviderRow(label = "Rent", providers = region.rent, onProviderClick = onOpenProviders)
            ProviderRow(label = "Buy", providers = region.buy, onProviderClick = onOpenProviders)
            Text(
                text = "Streaming data provided by JustWatch.",
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
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = "Change region") }
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
 * Shared layout for the Similar and Recommendations sections — a heading followed by a horizontal
 * poster row, reusing [MoviePosterCard] like every other carousel in the app. The two sections are
 * deliberately separate lists/API calls (see MovieRepository.getSimilarMovies vs
 * getRecommendedMovies) and are not deduped against each other.
 */
@Composable
private fun PosterRowSection(title: String, movies: List<Movie>, onMovieClick: (Movie) -> Unit) {
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
            items(movies, key = { it.id }) { movie ->
                MoviePosterCard(movie = movie, onClick = { onMovieClick(movie) }, onToggleFavorite = null, width = 128.dp)
            }
        }
    }
}

/**
 * Phase 3 Round A's cast/crew section — sits directly below the Overview. A "view all" arrow next
 * to the heading opens [com.ahsan.movieapp.ui.detail.CastCrewListScreen] with the full cast (this
 * row only shows the first [CAST_ROW_LIMIT]) plus the director. Cast + director only, per the
 * confirmed Phase 3 scope — no other crew roles.
 */
@Composable
private fun CastCrewSection(
    cast: List<CastMember>,
    director: com.ahsan.movieapp.domain.model.Person?,
    onPersonClick: (personId: Int, personName: String) -> Unit,
    onViewAll: () -> Unit
) {
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Cast & Crew", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onViewAll) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "View all cast & crew")
            }
        }

        if (director != null) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
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
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
        }

        if (cast.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(cast.take(CAST_ROW_LIMIT), key = { it.id }) { member ->
                    CastMemberChip(member = member, onClick = { onPersonClick(member.id, member.name) })
                }
            }
        }
    }
}

private const val CAST_ROW_LIMIT = 15

/**
 * Shows the actor's real name AND the character they played, same "who / as whom" pairing
 * [com.ahsan.movieapp.ui.detail.CastCrewListScreen]'s `PersonRow` already shows for the "view all"
 * list — the name is the primary (larger, bolder) line, with the character underneath in a
 * smaller, dimmer style, matching how a secondary/subtitle line reads elsewhere in the app (e.g.
 * PersonScreen's role label under the person's name).
 */
@Composable
private fun CastMemberChip(member: CastMember, onClick: () -> Unit) {
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
private fun MetaChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.height(16.dp))
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

private fun Modifier.offsetUp(): Modifier = this.offset(y = (-40).dp)
