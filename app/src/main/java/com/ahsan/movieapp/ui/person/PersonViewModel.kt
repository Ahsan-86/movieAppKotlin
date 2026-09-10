package com.ahsan.movieapp.ui.person

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahsan.movieapp.data.repository.MovieRepository
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.domain.model.PersonCredits
import com.ahsan.movieapp.domain.model.PersonDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

enum class MediaTab { MOVIES, TV }
enum class RoleTab { ACTOR, DIRECTOR }

data class PersonUiState(
    val personName: String = "",
    val details: PersonDetails? = null,
    val credits: PersonCredits? = null,
    val selectedMediaType: MediaTab = MediaTab.MOVIES,
    val selectedRole: RoleTab = RoleTab.ACTOR,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val showTvTab: Boolean get() = credits?.hasTvCredits == true

    val showDirectorTab: Boolean
        get() = credits?.hasDirectingCredits(isTv = selectedMediaType == MediaTab.TV) == true

    val displayedMovies: List<Movie>
        get() {
            val c = credits ?: return emptyList()
            return when (selectedMediaType) {
                MediaTab.MOVIES -> if (selectedRole == RoleTab.ACTOR) c.actingMovies else c.directingMovies
                MediaTab.TV -> if (selectedRole == RoleTab.ACTOR) c.actingTvShows else c.directingTvShows
            }
        }
}

/**
 * Backs the redesigned person screen: bio/photo header, then a Movies/TV toggle (only shown if
 * the person actually has TV credits) and an Actor/Director toggle (only shown if they have
 * directing credits for the selected media type). Both toggles are pure client-side filtering —
 * [PersonCredits] is fetched once via TMDB's combined_credits and already split into all four
 * buckets, so switching tabs never re-hits the network.
 */
@HiltViewModel
class PersonViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MovieRepository
) : ViewModel() {

    private val personId: Int = checkNotNull(savedStateHandle["personId"])
    private val personName: String = URLDecoder.decode(
        checkNotNull(savedStateHandle.get<String>("personName")),
        "UTF-8"
    )

    private val _uiState = MutableStateFlow(PersonUiState(personName = personName))
    val uiState: StateFlow<PersonUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val (detailsResult, creditsResult) = coroutineScope {
                val details = async { repository.getPersonDetails(personId) }
                val credits = async { repository.getPersonCredits(personId) }
                details.await() to credits.await()
            }

            val details = detailsResult.getOrNull()
            val credits = creditsResult.getOrNull()

            _uiState.update {
                it.copy(
                    details = details ?: PersonDetails(
                        id = personId,
                        name = personName,
                        profileUrl = null,
                        biography = null,
                        role = null
                    ),
                    credits = credits,
                    isLoading = false,
                    errorMessage = if (details == null && credits == null) {
                        detailsResult.exceptionOrNull()?.message
                            ?: creditsResult.exceptionOrNull()?.message
                            ?: "Couldn't load this person"
                    } else null
                )
            }
        }
    }

    fun onMediaTabSelected(tab: MediaTab) {
        // Reset back to Actor when switching media type — directing credits differ per type,
        // and defaulting to Actor avoids landing on a toggle that might now be hidden/empty.
        _uiState.update { it.copy(selectedMediaType = tab, selectedRole = RoleTab.ACTOR) }
    }

    fun onRoleTabSelected(role: RoleTab) {
        _uiState.update { it.copy(selectedRole = role) }
    }

    fun toggleFavorite(movie: Movie) {
        viewModelScope.launch {
            repository.toggleFavorite(movie)
            _uiState.update { state ->
                val credits = state.credits ?: return@update state
                fun flip(list: List<Movie>) = list.map { if (it.id == movie.id) it.copy(isFavorite = !it.isFavorite) else it }
                state.copy(
                    credits = credits.copy(
                        actingMovies = flip(credits.actingMovies),
                        directingMovies = flip(credits.directingMovies)
                    )
                )
            }
        }
    }
}
