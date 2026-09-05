package com.ahsan.movieapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.ui.graphics.vector.ImageVector

/** Every screen the app can navigate to, with its route pattern in one place. */
sealed class Destination(val route: String) {
    data object Explore : Destination("explore")
    data object Trending : Destination("trending")
    data object Favorites : Destination("favorites")
    data object Search : Destination("search")
    data object Account : Destination("account")

    data object MovieDetail : Destination("movie/{movieId}") {
        fun createRoute(movieId: Int) = "movie/$movieId"
    }

    data object PersonFilmography : Destination("person/{personId}/{personName}") {
        fun createRoute(personId: Int, personName: String) =
            "person/$personId/${java.net.URLEncoder.encode(personName, "UTF-8")}"
    }
}

data class BottomNavItem(
    val destination: Destination,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Destination.Explore, "Explore", Icons.Filled.Explore, Icons.Outlined.Explore),
    BottomNavItem(Destination.Trending, "Trending", Icons.Filled.LocalFireDepartment, Icons.Outlined.LocalFireDepartment),
    BottomNavItem(Destination.Favorites, "Favorites", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder)
)
