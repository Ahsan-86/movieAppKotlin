package com.ahsan.movieapp.ui.search

import com.ahsan.movieapp.R

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import com.ahsan.movieapp.data.repository.SearchViewMode
import com.ahsan.movieapp.domain.model.DiscoverFilters
import com.ahsan.movieapp.domain.model.GenreChip
import com.ahsan.movieapp.domain.model.MediaType
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.domain.model.Person
import com.ahsan.movieapp.ui.components.EmptyState
import com.ahsan.movieapp.ui.components.FilterPanelSection
import com.ahsan.movieapp.ui.components.FilterSummaryBar
import com.ahsan.movieapp.ui.components.FullScreenError
import com.ahsan.movieapp.ui.components.FullScreenLoading
import com.ahsan.movieapp.ui.components.MovieListRow
import com.ahsan.movieapp.ui.components.MoviePosterCard
import com.ahsan.movieapp.ui.components.PagingAppendFooter
import kotlinx.coroutines.flow.Flow

@Composable
fun SearchScreen(
    onMovieClick: (Movie) -> Unit,
    onPersonClick: (Person) -> Unit,
    // TV search results ("TV Shows" row) route to the real TV detail screen, same as Explore's
    // Popular TV Shows row — the item is still modeled as [Movie] (its `id` is a TV id).
    onTvClick: (Movie) -> Unit,
    onGenreClick: (GenreChip) -> Unit,
    // Fires when the user re-taps the Search icon while already on this screen — same "scroll
    // back to top" behavior as re-tapping an already-selected bottom nav tab. Null outside the
    // nav graph (e.g. previews) since there's nothing to reset to.
    scrollToTopEvents: Flow<Unit>? = null,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Subscribed unconditionally, same as every other paginated screen — each flow internally
    // switches to a fresh Pager per submitted query / applied filter set (flatMapLatest, see
    // SearchViewModel), so there's nothing to gate on state.query here; an inactive one just sits
    // on PagingData.empty() until its query/filters are non-null.
    val pagedSearchMovies = viewModel.pagedSearchMovies.collectAsLazyPagingItems()
    val pagedFilteredMovies = viewModel.pagedFilteredMovies.collectAsLazyPagingItems()

    // Live favorite-id set + the applied filters' total — see SearchViewModel; the filtered branch
    // consumes both below (count via FilterSummaryBar, ids via the grid/list re-stamping).
    val favoriteIds by viewModel.favoriteIds.collectAsState(emptySet())
    val filteredResultCount by viewModel.filteredResultCount.collectAsState()
    val searchResultCount by viewModel.searchResultCount.collectAsState()

    // One state per Lazy container this screen can show — only one is ever composed at a time
    // (they're mutually exclusive branches below), so it's safe to reuse each across every mode
    // it covers rather than needing a state per view mode.
    val blankListState = rememberLazyListState()
    val resultsListState = rememberLazyListState()
    val resultsGridState = rememberLazyGridState()

    // Session 6 — the results branches key off what was actually COMMITTED, not the typed text
    // (`query`). Typing without submitting stays on the blank home content, so a first keystroke
    // can never flip the screen into the results layout while its Paging flow is still unloaded
    // (that was the "full-screen loader while typing on the first search" bug — the never-loaded
    // flow reported refresh Loading, and typing alone was enough to enter that branch).
    val hasActiveTextResults = state.committedQuery.isNotBlank() && state.query.isNotBlank()
    val hasActiveFilterResults = state.isFilterApplied && state.query.isBlank()

    LaunchedEffect(scrollToTopEvents) {
        scrollToTopEvents?.collect {
            when {
                !hasActiveTextResults && !hasActiveFilterResults -> blankListState.animateScrollToItem(0)
                // Filtered results reuse the same list/grid states as text-search results below —
                // the two are mutually exclusive (filters only apply while the query is blank).
                hasActiveFilterResults -> {
                    if (state.viewMode == SearchViewMode.LIST) resultsListState.animateScrollToItem(0)
                    else resultsGridState.animateScrollToItem(0)
                }
                pagedSearchMovies.itemCount == 0 && state.people.isEmpty() && state.tvShows.isEmpty() &&
                    pagedSearchMovies.loadState.refresh !is LoadState.Loading && !state.isSearchingPeople && !state.isSearchingTv -> {
                    // EmptyState — a centered message, nothing scrollable to reset.
                }
                state.viewMode == SearchViewMode.LIST -> resultsListState.animateScrollToItem(0)
                else -> resultsGridState.animateScrollToItem(0)
            }
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        OutlinedTextField(
            value = state.query,
            // Session 6 — typing only edits the text; the query executes on submit (IME search
            // action or the trailing button) via onSearchSubmit, never as you type.
            onValueChange = viewModel::onQueryInput,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                // The movie grid has its own loadState-driven loading UI (FullScreenLoading /
                // PagingAppendFooter); this spinner just reflects the two one-shot fetches (people
                // + TV Shows rows), the last "single Result call per submitted query"s left on
                // this screen. When not searching, a committed (non-blank) query shows the submit
                // button — the other way to run the search besides the IME's Search key.
                if (state.isSearchingPeople || state.isSearchingTv) {
                    CircularProgressIndicator(modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp), strokeWidth = 2.dp)
                } else if (state.query.isNotBlank()) {
                    IconButton(onClick = {
                        viewModel.onSearchSubmit()
                        keyboardController?.hide()
                    }) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search))
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                viewModel.onSearchSubmit()
                keyboardController?.hide()
            })
        )

        when {
            hasActiveFilterResults -> Column(modifier = Modifier.fillMaxSize()) {
                FilterSummaryBar(
                    filters = state.filters,
                    onEdit = viewModel::onEditFilters,
                    onClear = viewModel::onClearFilters,
                    resultCount = filteredResultCount
                )
                val refreshState = pagedFilteredMovies.loadState.refresh
                when {
                    refreshState is LoadState.Loading && pagedFilteredMovies.itemCount == 0 -> FullScreenLoading()
                    refreshState is LoadState.Error && pagedFilteredMovies.itemCount == 0 -> FullScreenError(
                        message = refreshState.error.message ?: stringResource(R.string.search_couldnt_load_results),
                        onRetry = { pagedFilteredMovies.retry() }
                    )
                    pagedFilteredMovies.itemCount == 0 -> EmptyState(
                        title = stringResource(R.string.search_no_matches),
                        body = stringResource(R.string.search_no_matches_body)
                    )
                    else -> {
                        ViewModeRow(selected = state.viewMode, onSelected = viewModel::onViewModeSelected)
                        when (state.viewMode) {
                            SearchViewMode.LIST -> SearchResultsList(
                                movies = pagedFilteredMovies,
                                people = emptyList(),
                                tvShows = emptyList(),
                                onMovieClick = onMovieClick,
                                onTvClick = onTvClick,
                                onPersonClick = onPersonClick,
                                onToggleFavorite = viewModel::toggleFavorite,
                                listState = resultsListState,
                                favoriteIds = favoriteIds
                            )
                            else -> SearchResultsGrid(
                                movies = pagedFilteredMovies,
                                people = emptyList(),
                                tvShows = emptyList(),
                                columns = if (state.viewMode == SearchViewMode.GRID_DENSE) GridCells.Fixed(4) else GridCells.Adaptive(minSize = 128.dp),
                                posterWidth = null,
                                onMovieClick = onMovieClick,
                                onTvClick = onTvClick,
                                onPersonClick = onPersonClick,
                                onToggleFavorite = viewModel::toggleFavorite,
                                gridState = resultsGridState,
                                favoriteIds = favoriteIds
                            )
                        }
                    }
                }
            }
            hasActiveTextResults -> {
                val moviesRefresh = pagedSearchMovies.loadState.refresh
                val nothingLoadedYet = pagedSearchMovies.itemCount == 0 && state.people.isEmpty() && state.tvShows.isEmpty()
                when {
                    nothingLoadedYet && (moviesRefresh is LoadState.Loading || state.isSearchingPeople || state.isSearchingTv) -> FullScreenLoading()
                    nothingLoadedYet && moviesRefresh is LoadState.Error -> FullScreenError(
                        message = moviesRefresh.error.message ?: stringResource(R.string.search_failed),
                        onRetry = { pagedSearchMovies.retry() }
                    )
                    nothingLoadedYet -> EmptyState(
                        title = stringResource(R.string.search_no_results),
                        body = stringResource(R.string.search_no_results_body, state.query)
                    )
                    else -> {
                        // The view-mode toggle only makes sense once there's something to lay out —
                        // it used to sit above the search field permanently, which showed it even on
                        // the blank first-open screen with no results to switch the layout of.
                        if (searchResultCount != null) {
                            val count = searchResultCount
                            Text(
                                text = stringResource(R.string.filter_results_found, count!!),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                        ViewModeRow(selected = state.viewMode, onSelected = viewModel::onViewModeSelected)
                        when (state.viewMode) {
                            SearchViewMode.LIST -> SearchResultsList(
                                movies = pagedSearchMovies,
                                people = state.people,
                                tvShows = state.tvShows,
                                onMovieClick = onMovieClick,
                                onTvClick = onTvClick,
                                onPersonClick = onPersonClick,
                                onToggleFavorite = viewModel::toggleFavorite,
                                listState = resultsListState,
                                favoriteIds = favoriteIds
                            )
                            SearchViewMode.GRID -> SearchResultsGrid(
                                movies = pagedSearchMovies,
                                people = state.people,
                                tvShows = state.tvShows,
                                columns = GridCells.Adaptive(minSize = 128.dp),
                                posterWidth = null,
                                onMovieClick = onMovieClick,
                                onTvClick = onTvClick,
                                onPersonClick = onPersonClick,
                                onToggleFavorite = viewModel::toggleFavorite,
                                gridState = resultsGridState,
                                favoriteIds = favoriteIds
                            )
                            SearchViewMode.GRID_DENSE -> SearchResultsGrid(
                                movies = pagedSearchMovies,
                                people = state.people,
                                tvShows = state.tvShows,
                                columns = GridCells.Fixed(4),
                                posterWidth = null,
                                onMovieClick = onMovieClick,
                                onTvClick = onTvClick,
                                onPersonClick = onPersonClick,
                                onToggleFavorite = viewModel::toggleFavorite,
                                gridState = resultsGridState,
                                favoriteIds = favoriteIds
                            )
                        }
                    }
                }
            }
            else -> BlankSearchContent(
                recentSearches = state.recentSearches,
                genreChips = state.genreChips,
                onRecentClick = viewModel::onRecentSearchClick,
                onRemoveRecent = viewModel::onDeleteRecentSearch,
                onClearAll = viewModel::clearSearchHistory,
                onGenreClick = onGenreClick,
                listState = blankListState,
                filters = state.filters,
                isFilterPanelExpanded = state.isFilterPanelExpanded,
                onToggleFilterPanel = viewModel::onToggleFilterPanel,
                onGenreFilterSelected = viewModel::onGenreFilterSelected,
                onYearFilterSelected = viewModel::onYearFilterSelected,
                onLanguageFilterSelected = viewModel::onLanguageFilterSelected,
                onMinRatingFilterChanged = viewModel::onMinRatingFilterChanged,
                onApplyFilters = viewModel::onApplyFilters
            )
        }
    }
}

/**
 * Custom-drawn 4×4 grid of squares — the dense grid mode actually renders `GridCells.Fixed(4)`,
 * and no stock Material icon reads unambiguously as "4 columns" (the built-in "apps"/"grid_on"
 * glyphs read as 3×3 to most eyes), so this draws the exact thing rather than guessing at a named
 * icon again.
 */
private val GridFourByFourIcon: ImageVector by lazy {
    val cell = 4f
    val gap = 1.6f
    val margin = 1.6f
    ImageVector.Builder(
        name = "GridFourByFour",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        for (row in 0 until 4) {
            for (col in 0 until 4) {
                val x = margin + col * (cell + gap)
                val y = margin + row * (cell + gap)
                path(fill = SolidColor(Color.Black)) {
                    moveTo(x, y)
                    lineTo(x + cell, y)
                    lineTo(x + cell, y + cell)
                    lineTo(x, y + cell)
                    close()
                }
            }
        }
    }.build()
}

@Composable
private fun ViewModeRow(selected: SearchViewMode, onSelected: (SearchViewMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        ViewModeButton(icon = Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.search_list_view), isSelected = selected == SearchViewMode.LIST) {
            onSelected(SearchViewMode.LIST)
        }
        ViewModeButton(icon = Icons.Filled.GridView, contentDescription = stringResource(R.string.search_grid_view), isSelected = selected == SearchViewMode.GRID) {
            onSelected(SearchViewMode.GRID)
        }
        ViewModeButton(icon = GridFourByFourIcon, contentDescription = stringResource(R.string.search_dense_grid_view), isSelected = selected == SearchViewMode.GRID_DENSE) {
            onSelected(SearchViewMode.GRID_DENSE)
        }
    }
}

@Composable
private fun ViewModeButton(
    icon: ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * First-open state: recent searches (if any) plus genre chips, replacing what used to be a blank
 * "Search TMDB" prompt. Recent searches stay a small, bounded, wrapping FlowRow (<=10 chips) —
 * nothing like the search-results grid, so that's still safe; it was the *results* grid growing
 * unbounded that caused the original Phase 2 regression, not chip rows.
 */
@Composable
private fun BlankSearchContent(
    recentSearches: List<String>,
    genreChips: List<GenreChip>,
    onRecentClick: (String) -> Unit,
    onRemoveRecent: (String) -> Unit,
    onClearAll: () -> Unit,
    onGenreClick: (GenreChip) -> Unit,
    listState: LazyListState,
    filters: DiscoverFilters,
    isFilterPanelExpanded: Boolean,
    onToggleFilterPanel: () -> Unit,
    onGenreFilterSelected: (Int?) -> Unit,
    onYearFilterSelected: (Int?) -> Unit,
    onLanguageFilterSelected: (String?) -> Unit,
    onMinRatingFilterChanged: (Float?) -> Unit,
    onApplyFilters: () -> Unit
) {
    // LazyColumn, not a manually-scrolled Column — a LazyRow nested inside a Column.verticalScroll()
    // only measures/scrolls its first couple of items correctly (the "only two genres show" bug
    // this hit before); nesting lazy content inside a LazyColumn item {} instead is the robust
    // pattern, same lesson as the Phase 2 results-grid fix. This also gives the hoisted
    // [listState] something to drive "scroll to top" on when the Search icon is re-tapped.
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        item {
            FilterPanelSection(
                filters = filters,
                isExpanded = isFilterPanelExpanded,
                onToggleExpanded = onToggleFilterPanel,
                onYearSelected = onYearFilterSelected,
                onLanguageSelected = onLanguageFilterSelected,
                onMinRatingChanged = onMinRatingFilterChanged,
                onApply = onApplyFilters,
                onReset = {
                    onGenreFilterSelected(null)
                    onYearFilterSelected(null)
                    onLanguageFilterSelected(null)
                    onMinRatingFilterChanged(null)
                },
                genres = genreChips,
                onGenreSelected = onGenreFilterSelected
            )
        }
        if (recentSearches.isNotEmpty()) {
            item {
                RecentSearchesSection(recentSearches = recentSearches, onRecentClick = onRecentClick, onRemoveRecent = onRemoveRecent, onClearAll = onClearAll)
            }
        }
        item {
            GenreChipsSection(genres = genreChips, onGenreClick = onGenreClick)
        }
        if (recentSearches.isEmpty() && genreChips.isEmpty()) {
            item {
                EmptyState(title = stringResource(R.string.search_tmdb), body = stringResource(R.string.search_tmdb_body))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecentSearchesSection(
    recentSearches: List<String>,
    onRecentClick: (String) -> Unit,
    onRemoveRecent: (String) -> Unit,
    onClearAll: () -> Unit
) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = stringResource(R.string.search_recent_searches), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onClearAll) { Text(stringResource(R.string.search_clear_all)) }
        }
        FlowRow(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            recentSearches.forEach { query ->
                AssistChip(
                    onClick = { onRecentClick(query) },
                    label = { Text(query) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            // The chip's core onClick runs the search again; this dismisses just
                            // this one history entry instead of the whole list (Clear all).
                            onClick = { onRemoveRecent(query) },
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.search_remove_recent),
                                modifier = Modifier.size(AssistChipDefaults.IconSize)
                            )
                        }
                    }
                )
            }
        }
    }
}

private val GENRE_CARD_HEIGHT = 108.dp

/**
 * A 2-column vertical grid of genre cards — was a horizontally-scrolling LazyRow, changed on
 * request since a vertical grid surfaces every genre at a glance instead of requiring a
 * horizontal swipe to discover ones further down the list.
 *
 * This is deliberately a plain chunked `Column` of `Row`s rather than a `LazyVerticalGrid`: the
 * genre list is small and fixed (12 curated genres, never paginated — see
 * `MovieRepositoryImpl.CURATED_GENRES`), so there's nothing to virtualize, and a real
 * `LazyVerticalGrid` nested inside this screen's outer `LazyColumn` item {} would hit the same
 * "lazy layout with no bounded height inside another lazy scrollable" problem this app already
 * hit twice (see the "only two genres show" bug in the project log) — it would need either a
 * hand-computed fixed height or `userScrollEnabled = false` to avoid crashing/misbehaving.
 * Eagerly laying out 7 short rows is cheap and sidesteps that entirely while rendering as an
 * identical 2-column grid.
 */
@Composable
private fun GenreChipsSection(genres: List<GenreChip>, onGenreClick: (GenreChip) -> Unit) {
    if (genres.isEmpty()) return
    Column(modifier = Modifier.padding(top = 24.dp)) {
        Text(stringResource(R.string.search_browse_by_genre), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        genres.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pair.forEach { genre ->
                    GenreChipItem(genre = genre, onClick = { onGenreClick(genre) }, modifier = Modifier.weight(1f))
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun GenreChipItem(genre: GenreChip, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(GENRE_CARD_HEIGHT)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick)
    ) {
        if (genre.imageUrl != null) {
            AsyncImage(
                model = genre.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f)))
        }
        Text(
            text = genre.name,
            color = if (genre.imageUrl != null) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 12.dp)
        )
    }
}

/**
 * Everything here lives inside one LazyVerticalGrid, including the full-width people row and
 * section header — that's deliberate. This is the fix for the reverted Phase 2 attempt: only
 * items that scroll into view get composed (and only then does Coil kick off an image load),
 * whereas the old FlowRow composed and loaded every result at once regardless of what was visible.
 * [columns] varies by view mode (adaptive ~2-3 up vs a fixed 4-up dense grid); [posterWidth] null
 * lets each poster fill its grid cell instead of a fixed 128dp.
 *
 * Phase 4 (pagination) Round 4 — [movies] is now a [LazyPagingItems] source (either
 * [SearchViewModel.pagedSearchMovies] or [SearchViewModel.pagedFilteredMovies], depending on the
 * caller) rather than a plain list; [people] and [tvShows] stay plain, small, non-paginated lists
 * either way (both empty for the filter-results caller, since Discover has no people or TV to
 * show). A [PagingAppendFooter] closes out the movies section, same shared composable every other
 * paginated grid in this app already uses.
 */
@Composable
private fun SearchResultsGrid(
    movies: LazyPagingItems<Movie>,
    people: List<Person>,
    // One-shot [Movie] list for the "TV Shows" row (see SearchViewModel.uiState) — empty for the
    // filter-results caller, since Discover has no TV counterpart.
    tvShows: List<Movie>,
    columns: GridCells,
    posterWidth: androidx.compose.ui.unit.Dp?,
    onMovieClick: (Movie) -> Unit,
    onTvClick: (Movie) -> Unit,
    onPersonClick: (Person) -> Unit,
    onToggleFavorite: (Movie) -> Unit,
    gridState: LazyGridState,
    // Live favorites set — the paged Movie snapshot's isFavorite goes stale after a toggle in
    // this screen, so each item re-stamps itself against this set at render time. See
    // SearchViewModel.favoriteIds for why.
    favoriteIds: Set<Pair<Int, MediaType>>
) {
    LazyVerticalGrid(
        columns = columns,
        state = gridState,
        contentPadding = PaddingValues(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (people.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                PeopleResultsRow(people = people, onPersonClick = onPersonClick)
            }
        }
        if (tvShows.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                TvShowsResultsRow(tvShows = tvShows, onTvClick = onTvClick, onToggleFavorite = onToggleFavorite, favoriteIds = favoriteIds)
            }
        }
        if (movies.itemCount > 0) {
            if (people.isNotEmpty() || tvShows.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = stringResource(R.string.movies),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
            }
            items(count = movies.itemCount, key = movies.itemKey { it.id }) { index ->
                val movie = movies[index] ?: return@items
                val displayMovie = if (movie.isFavorite == ((movie.id to movie.mediaType) in favoriteIds)) movie else movie.copy(isFavorite = (movie.id to movie.mediaType) in favoriteIds)
                MoviePosterCard(
                    movie = displayMovie,
                    onClick = { onMovieClick(displayMovie) },
                    onToggleFavorite = { onToggleFavorite(displayMovie) },
                    width = posterWidth
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                PagingAppendFooter(pagingItems = movies)
            }
        }
    }
}

/** List-view mode: a LazyColumn of full-width rows instead of a poster grid. See [SearchResultsGrid]'s doc for [movies]/[people]/[tvShows] and [favoriteIds]. */
@Composable
private fun SearchResultsList(
    movies: LazyPagingItems<Movie>,
    people: List<Person>,
    tvShows: List<Movie>,
    onMovieClick: (Movie) -> Unit,
    onTvClick: (Movie) -> Unit,
    onPersonClick: (Person) -> Unit,
    onToggleFavorite: (Movie) -> Unit,
    listState: LazyListState,
    favoriteIds: Set<Pair<Int, MediaType>>
) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(vertical = 16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (people.isNotEmpty()) {
            item { PeopleResultsRow(people = people, onPersonClick = onPersonClick) }
        }
        if (tvShows.isNotEmpty()) {
            item { TvShowsResultsRow(tvShows = tvShows, onTvClick = onTvClick, onToggleFavorite = onToggleFavorite, favoriteIds = favoriteIds) }
        }
        if (movies.itemCount > 0) {
            if (people.isNotEmpty() || tvShows.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.movies),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
            }
            items(count = movies.itemCount, key = movies.itemKey { it.id }) { index ->
                val movie = movies[index] ?: return@items
                val displayMovie = if (movie.isFavorite == ((movie.id to movie.mediaType) in favoriteIds)) movie else movie.copy(isFavorite = (movie.id to movie.mediaType) in favoriteIds)
                MovieListRow(movie = displayMovie, onClick = { onMovieClick(displayMovie) }, onToggleFavorite = { onToggleFavorite(displayMovie) })
            }
            item { PagingAppendFooter(pagingItems = movies) }
        }
    }
}

/** A LazyRow, same as every other people/movie rail in the app — bounded by MAX_PEOPLE_RESULTS upstream. */
@Composable
private fun PeopleResultsRow(people: List<Person>, onPersonClick: (Person) -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.search_people),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(people, key = { it.id }) { person ->
                PersonResultCard(person = person, onClick = { onPersonClick(person) })
            }
        }
    }
}

/**
 * A LazyRow for the search screen's TV results, grouped separately from the movie grid the same
 * way people are. Session 6 — TV cards now render a heart like movies (the composite-key Favorites
 * table can hold both), re-stamping each row's [Movie.isFavorite] against [favoriteIds] at render
 * time, same live-set trick as the movie grid above.
 */
@Composable
private fun TvShowsResultsRow(
    tvShows: List<Movie>,
    onTvClick: (Movie) -> Unit,
    onToggleFavorite: (Movie) -> Unit,
    favoriteIds: Set<Pair<Int, MediaType>>
) {
    Column {
        Text(
            text = stringResource(R.string.tv_shows),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(tvShows, key = { it.id }) { tvShow ->
                val displayTv = if (tvShow.isFavorite == ((tvShow.id to tvShow.mediaType) in favoriteIds)) tvShow else tvShow.copy(isFavorite = (tvShow.id to tvShow.mediaType) in favoriteIds)
                MoviePosterCard(
                    movie = displayTv,
                    onClick = { onTvClick(displayTv) },
                    onToggleFavorite = { onToggleFavorite(displayTv) }
                )
            }
        }
    }
}

@Composable
private fun PersonResultCard(person: Person, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = person.profileUrl,
                contentDescription = person.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            text = person.name,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
