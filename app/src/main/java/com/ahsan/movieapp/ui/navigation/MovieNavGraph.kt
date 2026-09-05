package com.ahsan.movieapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.ui.account.AccountScreen
import com.ahsan.movieapp.ui.account.AccountViewModel
import com.ahsan.movieapp.ui.detail.MovieDetailScreen
import com.ahsan.movieapp.ui.favorites.FavoritesScreen
import com.ahsan.movieapp.ui.home.HomeScreen
import com.ahsan.movieapp.ui.search.SearchScreen
import com.ahsan.movieapp.ui.trending.TrendingScreen

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

@Composable
private fun MainNavHost() {
    val navController = rememberNavController()

    Scaffold(
        topBar = { MovieTopBar(navController) },
        bottomBar = { MovieBottomBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Explore.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destination.Explore.route) {
                HomeScreen(onMovieClick = { navController.navigateToDetail(it) })
            }
            composable(Destination.Trending.route) {
                TrendingScreen(onMovieClick = { navController.navigateToDetail(it) })
            }
            composable(Destination.Favorites.route) {
                FavoritesScreen(onMovieClick = { navController.navigateToDetail(it) })
            }
            composable(Destination.Search.route) {
                SearchScreen(onMovieClick = { navController.navigateToDetail(it) })
            }
            composable(Destination.Account.route) {
                AccountScreen()
            }
            composable(
                route = Destination.MovieDetail.route,
                arguments = listOf(androidx.navigation.navArgument("movieId") { type = androidx.navigation.NavType.IntType })
            ) {
                MovieDetailScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun NavHostController.navigateToDetail(movie: Movie) {
    navigate(Destination.MovieDetail.createRoute(movie.id))
}

@Composable
private fun MovieTopBar(navController: NavHostController) {
    TopAppBar(
        title = { androidx.compose.material3.Text("Movie App") },
        actions = {
            IconButton(onClick = { navController.navigate(Destination.Search.route) }) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
            }
            IconButton(onClick = { navController.navigate(Destination.Account.route) }) {
                Icon(Icons.Filled.AccountCircle, contentDescription = "Account")
            }
        }
    )
}

@Composable
private fun MovieBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    NavigationBar {
        bottomNavItems.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.destination.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
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
