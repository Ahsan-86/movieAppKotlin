package com.ahsan.movieapp.util

import android.content.Context
import android.widget.Toast

/**
 * TMDB's TV and movie ids are separate namespaces — every screen that shows TV items alongside
 * movies (Genre's TV tab, Explore's "Popular TV Shows" row, PersonScreen's TV filmography) reuses
 * the shared [com.ahsan.movieapp.domain.model.Movie] model for them (see
 * [com.ahsan.movieapp.data.mapper.toMovie]), but a TV item's `id` was never meant to be looked up
 * as a movie id. Routing a tapped TV item into [com.ahsan.movieapp.ui.detail.MovieDetailScreen]
 * the same way a movie poster does either 404s (no movie with that id) or — worse — silently opens
 * a real but unrelated movie that happens to share the id (found via user testing, 2026-09-10).
 *
 * Phase 2.6 (full app-wide TV support) is what retires this everywhere — a real TV detail screen
 * now exists (`ui/tv/TvDetailScreen.kt`, Session 1) and Explore's "Popular TV Shows" row already
 * routes there instead of here (see `MovieNavGraph.kt`'s `navigateToTvDetail`). Genre's TV tab and
 * PersonScreen's TV filmography still route through this toast — that's Session 4's job.
 */
fun Context.showTvDetailsUnavailableToast() {
    Toast.makeText(this, "TV show details aren't available yet — coming in a future update", Toast.LENGTH_SHORT).show()
}
