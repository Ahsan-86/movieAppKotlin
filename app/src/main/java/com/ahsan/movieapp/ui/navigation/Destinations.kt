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

    /**
     * Phase 3 Round A's "view all" — the full cast + director beyond what fits in the Detail
     * screen's horizontal row. A separate route (not a shared ViewModel with MovieDetail) so it
     * re-fetches by movieId like every other full-screen destination in this app; credits are a
     * small, cheap call, so refetching is simpler than threading the list through nav args.
     */
    data object CastCrewList : Destination("movie/{movieId}/cast-crew") {
        fun createRoute(movieId: Int) = "movie/$movieId/cast-crew"
    }

    data object PersonFilmography : Destination("person/{personId}/{personName}") {
        fun createRoute(personId: Int, personName: String) =
            "person/$personId/${java.net.URLEncoder.encode(personName, "UTF-8")}"
    }

    /**
     * A single genre's full-screen browse view. movieGenreId/tvGenreId travel as nav args
     * since a genre chip may lack one of them (e.g. a movie-only genre has no TV id) — nav-compose
     * can't carry a nullable Int arg cleanly, so a missing id is passed through as -1 and decoded
     * back to null on the receiving end.
     */
    data object GenreBrowse : Destination("genre/{genreName}/{movieGenreId}/{tvGenreId}") {
        fun createRoute(genreName: String, movieGenreId: Int?, tvGenreId: Int?) =
            "genre/${java.net.URLEncoder.encode(genreName, "UTF-8")}/${movieGenreId ?: -1}/${tvGenreId ?: -1}"
    }

    /**
     * Phase 3 Round B's collection teaser destination — every movie in a franchise, opened from
     * the Detail screen's compact "part of a collection" row. The name travels as a nav arg (same
     * URL-encoded convention as [PersonFilmography]/[GenreBrowse]) so the top bar has a title to
     * show immediately, before the full collection fetch completes.
     */
    data object CollectionDetail : Destination("collection/{collectionId}/{collectionName}") {
        fun createRoute(collectionId: Int, collectionName: String) =
            "collection/$collectionId/${java.net.URLEncoder.encode(collectionName, "UTF-8")}"
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
