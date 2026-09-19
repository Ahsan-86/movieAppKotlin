package com.ahsan.movieapp.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ahsan.movieapp.data.repository.SessionState
import com.ahsan.movieapp.domain.model.GenreChip
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.domain.model.Person
import com.ahsan.movieapp.ui.account.AccountScreen
import com.ahsan.movieapp.ui.account.AccountViewModel
import com.ahsan.movieapp.ui.detail.CastCrewListScreen
import com.ahsan.movieapp.ui.detail.CollectionScreen
import com.ahsan.movieapp.ui.detail.MovieDetailScreen
import com.ahsan.movieapp.ui.favorites.FavoritesScreen
import com.ahsan.movieapp.ui.genre.GenreScreen
import com.ahsan.movieapp.ui.home.HomeScreen
import com.ahsan.movieapp.ui.person.PersonScreen
import com.ahsan.movieapp.ui.search.SearchScreen
import com.ahsan.movieapp.ui.components.TrailerPlayerScreen
import com.ahsan.movieapp.ui.trending.TrendingScreen
import com.ahsan.movieapp.ui.tv.SeasonEpisodesScreen
import com.ahsan.movieapp.ui.tv.TvDetailScreen

/**
 * App entry point: gates on session state (first launch shows the guest/login chooser,
 * reusing [AccountScreen]) and otherwise shows the main bottom-nav experience.
 */
@Composable
fun MovieAppRootScaffold() {
    val accountViewModel: AccountViewModel = hiltViewModel()
    val session by accountViewModel.sessionState.collectAsState()

    if (session == SessionState.SignedOut) {
        AccountScreen(viewModel = accountViewModel)
    } else {
        MainNavHost()
    }
}

/**
 * Root-level tabs get the persistent "Movie App" bar + bottom nav; everything reached by tapping
 * into something (a movie, a person, a genre) is a full-screen destination with its own dynamic
 * top bar (title = that thing's name, a real back button) and no bottom nav — hence this check.
 */
private val TOP_LEVEL_ROUTES = setOf(
    Destination.Explore.route,
    Destination.Trending.route,
    Destination.Favorites.route,
    Destination.Search.route,
    Destination.Account.route
)

@Composable
private fun MainNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val isTopLevel = backStackEntry?.destination?.route in TOP_LEVEL_ROUTES
    // Carries "you got re-tapped while already selected" from the bottom nav / top bar down to
    // whichever tab screen is currently showing, so it can scroll itself back to the top — the
    // Instagram-home-icon pattern. See TabReselectBus.kt.
    val tabReselectBus = remember { TabReselectBus() }

    Scaffold(
        topBar = { if (isTopLevel) MovieTopBar(navController, tabReselectBus) },
        bottomBar = { if (isTopLevel) MovieBottomBar(navController, tabReselectBus) },
        // Without this, Scaffold still reserves a status-bar-height inset in innerPadding even
        // when topBar renders nothing (every non-top-level route) — and each detail-type screen
        // (Person/MovieDetail/Genre) has its OWN inner Scaffold+TopAppBar that reserves that same
        // inset a second time for itself. The result was a visible empty strip, roughly a second
        // status-bar's worth of space, sitting above the dynamic TopAppBar — "leftover space where
        // the old app bar was." Disabling insets on this outer Scaffold makes exactly one place
        // (whichever TopAppBar/NavigationBar is actually on screen) responsible for avoiding the
        // system bars, instead of two.
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Explore.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destination.Explore.route) {
                val scrollToTopEvents = remember(tabReselectBus) { tabReselectBus.reselected.forRoute(Destination.Explore.route) }
                HomeScreen(
                    onMovieClick = { navController.navigateToDetail(it) },
                    // Phase 2.6 Session 1 — Popular TV Shows row routes to the real TV detail
                    // screen. The row itself stays one-shot/uncached until Session 3 rebuilds it
                    // with Room-backed pagination.
                    onTvClick = { navController.navigateToTvDetail(it) },
                    onGenreClick = { navController.navigateToGenre(it) },
                    scrollToTopEvents = scrollToTopEvents
                )
            }
            composable(Destination.Trending.route) {
                val scrollToTopEvents = remember(tabReselectBus) { tabReselectBus.reselected.forRoute(Destination.Trending.route) }
                TrendingScreen(
                    onMovieClick = { navController.navigateToDetail(it) },
                    scrollToTopEvents = scrollToTopEvents
                )
            }
            composable(Destination.Favorites.route) {
                val scrollToTopEvents = remember(tabReselectBus) { tabReselectBus.reselected.forRoute(Destination.Favorites.route) }
                FavoritesScreen(
                    onMovieClick = { navController.navigateToDetail(it) },
                    scrollToTopEvents = scrollToTopEvents
                )
            }
            composable(Destination.Search.route) {
                val scrollToTopEvents = remember(tabReselectBus) { tabReselectBus.reselected.forRoute(Destination.Search.route) }
                SearchScreen(
                    onMovieClick = { navController.navigateToDetail(it) },
                    onPersonClick = { navController.navigateToPerson(it) },
                    // TV search results ("TV Shows" row) route to the real TV detail screen.
                    onTvClick = { navController.navigateToTvDetail(it) },
                    onGenreClick = { navController.navigateToGenre(it) },
                    scrollToTopEvents = scrollToTopEvents
                )
            }
            composable(Destination.Account.route) {
                AccountScreen()
            }
            composable(
                route = Destination.MovieDetail.route,
                arguments = listOf(androidx.navigation.navArgument("movieId") { type = androidx.navigation.NavType.IntType })
            ) {
                MovieDetailScreen(
                    onBack = { navController.popBackStack() },
                    onPersonClick = { personId, personName -> navController.navigateToPerson(personId, personName) },
                    onViewAllCastCrew = { movieId -> navController.navigate(Destination.CastCrewList.createRoute(movieId)) },
                    // Similar/Recommendations posters push a new Detail screen onto the back stack
                    // (plain navigate(), same as every other "tap a poster" spot in this app) —
                    // tapping "back" from the new one returns to this movie, not out of Detail
                    // entirely, so a Movie -> Similar -> Similar chain works like Person -> Movie does.
                    onMovieClick = { navController.navigateToDetail(it) },
                    onCollectionClick = { collectionId, collectionName ->
                        navController.navigateToCollection(collectionId, collectionName)
                    },
                    onWatchTrailer = { videoId -> navController.navigateToTrailer(videoId) }
                )
            }
            composable(
                route = Destination.TvDetail.route,
                arguments = listOf(androidx.navigation.navArgument("tvId") { type = androidx.navigation.NavType.IntType })
            ) {
                TvDetailScreen(
                    onBack = { navController.popBackStack() },
                    onPersonClick = { personId, personName -> navController.navigateToPerson(personId, personName) },
                    // Similar/Recommendations posters push a new TV Detail screen onto the back
                    // stack, same "plain navigate()" convention as MovieDetailScreen's onMovieClick
                    // above — a TV -> Similar -> Similar chain works the same way.
                    onTvClick = { navController.navigateToTvDetail(it) },
                    // Phase 2.6 Session 2 — a tapped season opens its full episode list.
                    onSeasonClick = { tvId, seasonNumber, seasonName ->
                        navController.navigate(Destination.SeasonEpisodes.createRoute(tvId, seasonNumber, seasonName))
                    },
                    // Same arrow next to "Cast & Crew" as MovieDetailScreen — the full list plus
                    // director, fetched by tvId on the media-agnostic CastCrewListScreen.
                    onViewAllCastCrew = { tvId -> navController.navigate(Destination.TvCastCrewList.createRoute(tvId)) },
                    onWatchTrailer = { videoId -> navController.navigateToTrailer(videoId) }
                )
            }
            composable(
                route = Destination.SeasonEpisodes.route,
                arguments = listOf(
                    androidx.navigation.navArgument("tvId") { type = androidx.navigation.NavType.IntType },
                    androidx.navigation.navArgument("seasonNumber") { type = androidx.navigation.NavType.IntType },
                    androidx.navigation.navArgument("seasonName") { type = androidx.navigation.NavType.StringType }
                )
            ) {
                SeasonEpisodesScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Destination.TrailerPlayer.route,
                arguments = listOf(androidx.navigation.navArgument("videoId") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val videoId = backStackEntry.arguments?.getString("videoId").orEmpty()
                TrailerPlayerScreen(videoId = videoId, onBack = { navController.popBackStack() })
            }
            composable(
                route = Destination.CastCrewList.route,
                arguments = listOf(androidx.navigation.navArgument("movieId") { type = androidx.navigation.NavType.IntType })
            ) {
                CastCrewListScreen(
                    onBack = { navController.popBackStack() },
                    onPersonClick = { personId, personName -> navController.navigateToPerson(personId, personName) }
                )
            }
            composable(
                route = Destination.TvCastCrewList.route,
                arguments = listOf(androidx.navigation.navArgument("tvId") { type = androidx.navigation.NavType.IntType })
            ) {
                CastCrewListScreen(
                    onBack = { navController.popBackStack() },
                    onPersonClick = { personId, personName -> navController.navigateToPerson(personId, personName) }
                )
            }
            composable(
                route = Destination.CollectionDetail.route,
                arguments = listOf(
                    androidx.navigation.navArgument("collectionId") { type = androidx.navigation.NavType.IntType },
                    androidx.navigation.navArgument("collectionName") { type = androidx.navigation.NavType.StringType }
                )
            ) {
                CollectionScreen(
                    onBack = { navController.popBackStack() },
                    onMovieClick = { navController.navigateToDetail(it) }
                )
            }
            composable(
                route = Destination.PersonFilmography.route,
                arguments = listOf(
                    androidx.navigation.navArgument("personId") { type = androidx.navigation.NavType.IntType },
                    androidx.navigation.navArgument("personName") { type = androidx.navigation.NavType.StringType }
                )
            ) {
                PersonScreen(
                    onBack = { navController.popBackStack() },
                    onMovieClick = { navController.navigateToDetail(it) },
                    // TV filmography rows route to the real TV detail screen (toast retired
                    // 2026-09-22).
                    onTvClick = { navController.navigateToTvDetail(it) }
                )
            }
            composable(
                route = Destination.GenreBrowse.route,
                arguments = listOf(
                    androidx.navigation.navArgument("genreName") { type = androidx.navigation.NavType.StringType },
                    androidx.navigation.navArgument("movieGenreId") { type = androidx.navigation.NavType.IntType },
                    androidx.navigation.navArgument("tvGenreId") { type = androidx.navigation.NavType.IntType }
                )
            ) {
                GenreScreen(
                    onBack = { navController.popBackStack() },
                    onMovieClick = { navController.navigateToDetail(it) },
                    // Genre's TV tab routes to the real TV detail screen (toast retired
                    // 2026-09-22).
                    onTvClick = { navController.navigateToTvDetail(it) }
                )
            }
        }
    }
}

private fun NavHostController.navigateToDetail(movie: Movie) {
    navigate(Destination.MovieDetail.createRoute(movie.id))
}

/** Routes a tapped TV item (still modeled as [Movie] — its `id` is a TV id, not a movie id) to
 *  the real TV detail screen. Used by Explore's Popular TV row, Search's TV results, Genre's TV
 *  tab, and PersonScreen's TV filmography. */
private fun NavHostController.navigateToTvDetail(movie: Movie) {
    navigate(Destination.TvDetail.createRoute(movie.id))
}

private fun NavHostController.navigateToPerson(person: Person) {
    navigateToPerson(person.id, person.name)
}

/** Used directly by cast/director drill-through (Phase 3 Round A), which only has an id + name,
 * not a full [Person]. */
private fun NavHostController.navigateToPerson(personId: Int, personName: String) {
    navigate(Destination.PersonFilmography.createRoute(personId, personName))
}

private fun NavHostController.navigateToGenre(genre: GenreChip) {
    navigate(Destination.GenreBrowse.createRoute(genre.name, genre.movieGenreId, genre.tvGenreId))
}

/** Detail screen's collection teaser (Phase 3 Round B) -> the full franchise list. */
private fun NavHostController.navigateToCollection(collectionId: Int, collectionName: String) {
    navigate(Destination.CollectionDetail.createRoute(collectionId, collectionName))
}

/** Phase 2.6 Session 2 post-ship — "Watch Trailer" -> the trailer's own full-screen destination
 *  (see [TrailerPlayerScreen]'s doc for why this replaced an in-place Dialog). */
private fun NavHostController.navigateToTrailer(videoId: String) {
    navigate(Destination.TrailerPlayer.createRoute(videoId))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MovieTopBar(navController: NavHostController, tabReselectBus: TabReselectBus) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // Trending and Favorites show their own tab name as the title; every other top-level tab
    // (Explore, Search, Account) keeps the app-wide "Movie App" title, same as before.
    val title = when (currentRoute) {
        Destination.Trending.route -> "Trending"
        Destination.Favorites.route -> "Favorites"
        else -> "Movie App"
    }

    TopAppBar(
        title = { androidx.compose.material3.Text(title) },
        actions = {
            IconButton(onClick = {
                if (currentRoute == Destination.Search.route) {
                    tabReselectBus.onReselected(Destination.Search.route)
                } else {
                    navController.navigate(Destination.Search.route)
                }
            }) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
            }
            IconButton(onClick = {
                if (currentRoute != Destination.Account.route) {
                    navController.navigate(Destination.Account.route)
                }
                // Account has no scrollable content to reset, so re-tapping it while already
                // there is simply a no-op — unlike Search, there's nothing to signal.
            }) {
                Icon(Icons.Filled.AccountCircle, contentDescription = "Account")
            }
        }
    )
}

@Composable
private fun MovieBottomBar(navController: NavHostController, tabReselectBus: TabReselectBus) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    NavigationBar {
        bottomNavItems.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.destination.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (selected) {
                        // Already on this tab — re-navigating here would be a silent no-op thanks
                        // to launchSingleTop, so instead signal the screen to reset itself
                        // (scroll back to top), matching Instagram/YouTube-style re-tap behavior.
                        tabReselectBus.onReselected(item.destination.route)
                    } else {
                        // Search (and any other non-bottom-nav destination) is entered via a plain
                        // navigate() with no popUpTo, so whichever tab was showing right before it
                        // opened is still sitting live in the back stack directly underneath it —
                        // never popped, never saved off. For that case, popBackStack() is the
                        // correct, reliable way back: it pops Search straight off and reveals the
                        // already-live tab underneath. Falling straight to navigate()+popUpTo() for
                        // this case was the bug — that combination silently did nothing when the
                        // navigation target was the same destination already sitting on top right
                        // after its own popUpTo ran. popBackStack() only succeeds when the target
                        // is actually still live on the stack, so this never fires for a tab that
                        // was previously saved off by an earlier bottom-nav switch — that case
                        // correctly falls through to the normal restoreState-based switch below,
                        // unchanged from before.
                        val returnedToLiveTab = navController.popBackStack(item.destination.route, false)
                        if (!returnedToLiveTab) {
                            navController.navigate(item.destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = { androidx.compose.material3.Text(item.label) }
            )
        }
    }
}
