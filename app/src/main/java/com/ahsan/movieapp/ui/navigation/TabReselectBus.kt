package com.ahsan.movieapp.ui.navigation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * Re-tapping a bottom-nav item (or a top-bar destination icon, e.g. Search) while already sitting
 * on that screen shouldn't navigate anywhere — `launchSingleTop` already makes a plain re-navigate
 * to the current destination a silent no-op — it should reset that screen, the way re-tapping
 * Instagram's already-selected home icon scrolls the feed back to the top rather than doing
 * nothing. This is the event bus carrying that "you got re-tapped" signal from the bottom nav /
 * top bar down to whichever screen is currently showing, without either side needing a direct
 * reference to the other. One instance is hoisted for the whole nav graph's lifetime.
 */
class TabReselectBus {
    private val _reselected = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val reselected: SharedFlow<String> = _reselected.asSharedFlow()

    /** Call with the route that was just re-tapped while it was already the active destination. */
    fun onReselected(route: String) {
        _reselected.tryEmit(route)
    }
}

/** Narrows the bus down to a `Flow<Unit>` that only fires for one specific route, so a screen
 *  composable doesn't need to know or care about any route but its own. */
fun SharedFlow<String>.forRoute(route: String): Flow<Unit> = filter { it == route }.map { }
