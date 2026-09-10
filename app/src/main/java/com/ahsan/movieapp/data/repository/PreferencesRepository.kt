package com.ahsan.movieapp.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tiny local session store: whether the person has chosen to continue as a guest, and
 * (once Firebase Auth lands in a later phase) their signed-in user id. This is intentionally
 * separate from Firebase itself so Explore/Trending/Favorites never have to know or care
 * whether real auth is wired up yet.
 */
@Singleton
class PreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val IS_GUEST = booleanPreferencesKey("is_guest")
        val USER_ID = stringPreferencesKey("user_id")
        val HAS_ONBOARDED = booleanPreferencesKey("has_onboarded")
        val SEARCH_VIEW_MODE = stringPreferencesKey("search_view_mode")
    }

    val sessionState: Flow<SessionState> = dataStore.data.map { prefs ->
        when {
            !prefs[Keys.HAS_ONBOARDED].orFalse() -> SessionState.SignedOut
            !prefs[Keys.USER_ID].isNullOrBlank() -> SessionState.SignedIn(prefs[Keys.USER_ID]!!)
            prefs[Keys.IS_GUEST].orFalse() -> SessionState.Guest
            else -> SessionState.SignedOut
        }
    }

    /** Remembers the user's last-picked search results layout across app restarts. */
    val searchViewMode: Flow<SearchViewMode> = dataStore.data.map { prefs ->
        SearchViewMode.entries.find { it.name == prefs[Keys.SEARCH_VIEW_MODE] } ?: SearchViewMode.GRID
    }

    suspend fun setSearchViewMode(mode: SearchViewMode) {
        dataStore.edit { it[Keys.SEARCH_VIEW_MODE] = mode.name }
    }

    suspend fun continueAsGuest() {
        dataStore.edit {
            it[Keys.IS_GUEST] = true
            it[Keys.HAS_ONBOARDED] = true
            it.remove(Keys.USER_ID)
        }
    }

    suspend fun setSignedInUser(userId: String) {
        dataStore.edit {
            it[Keys.USER_ID] = userId
            it[Keys.IS_GUEST] = false
            it[Keys.HAS_ONBOARDED] = true
        }
    }

    suspend fun signOut() {
        dataStore.edit {
            it.remove(Keys.USER_ID)
            it[Keys.IS_GUEST] = false
        }
    }

    private fun Boolean?.orFalse() = this ?: false
}

sealed interface SessionState {
    data object SignedOut : SessionState
    data object Guest : SessionState
    data class SignedIn(val userId: String) : SessionState
}

/** How the search screen lays out its results — user-picked, remembered via DataStore. */
enum class SearchViewMode { LIST, GRID, GRID_DENSE }
