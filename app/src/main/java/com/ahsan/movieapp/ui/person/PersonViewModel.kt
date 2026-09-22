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

/**
 * One cell of the single-bucket credit filter (Review-queue item 4 → option A, 2026-09-20):
 * an (Acting/Directed) × (Movies/TV) combination with its credit count. Only buckets with
 * credits present are shown, so the row never has empty states.
 */
data class CreditBucket(
    val media: MediaTab,
    val role: RoleTab,
    val count: Int
) {
    val exists: Boolean get() = count > 0
}

data class PersonUiState(
    val personName: String = "",
    val details: PersonDetails? = null,
    val credits: PersonCredits? = null,
    val selectedMediaType: MediaTab = MediaTab.MOVIES,
    val selectedRole: RoleTab = RoleTab.ACTOR,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    // Present credit buckets in Ahsan's canonical order (2026-09-20): Acting-Movies, Acting-TV,
    // Directed-Movies, Directed-TV; empty buckets are dropped so the chips never have empty states.
    val creditBuckets: List<CreditBucket>
        get() {
            val c = credits ?: return emptyList()
            return listOf(
                CreditBucket(MediaTab.MOVIES, RoleTab.ACTOR, c.actingMovies.size),
                CreditBucket(MediaTab.TV, RoleTab.ACTOR, c.actingTvShows.size),
                CreditBucket(MediaTab.MOVIES, RoleTab.DIRECTOR, c.directingMovies.size),
                CreditBucket(MediaTab.TV, RoleTab.DIRECTOR, c.directingTvShows.size)
            ).filter { it.exists }
        }

    val displayedMovies: List<Movie>
        get() {
            val c = credits ?: return emptyList()
            return when (selectedMediaType) {
                MediaTab.MOVIES -> if (selectedRole == RoleTab.ACTOR) c.actingMovies else c.directingMovies
                MediaTab.TV -> if (selectedRole == RoleTab.ACTOR) c.actingTvShows else c.directingTvShows
            }
        }

    // Person filmography grouped by release year, newest year first; the unknown-year bucket
    // ("—" releaseYear, or any non-numeric year) sorts last. Within a year, the credits keep their
    // original order. Backs the year-sectioned list on PersonScreen (Session 5 → option 2, chosen
    // 2026-09-20).
    val filmographyByYear: List<Pair<String, List<Movie>>>
        get() = displayedMovies
            .groupBy { it.releaseYear }
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<String, List<Movie>>> { entry ->
                    entry.key.toIntOrNull() ?: Int.MIN_VALUE
                }
            )
            .map { it.key to it.value }
}

/**
 * Backs the redesigned person screen: bio/photo header, then a single row of credit-bucket
 * chips in the filmography year-chip style (Review-queue item 4 → option A) — "Acting - Movies",
 * "Directed - Movies", etc., one per present (Acting/Directed) × (Movies/TV) combination, so
 * switching media type OR role is one tap. Filtering is pure client-side — [PersonCredits] is
 * fetched once via TMDB's combined_credits and already split into all four buckets, so switching
 * never re-hits the network.
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
                    } else null
                )
            }
        }
    }

    // Single-tap bucket select (Review-queue item 4 → option A): sets media type AND role at once.
    fun selectBucket(media: MediaTab, role: RoleTab) {
        _uiState.update { it.copy(selectedMediaType = media, selectedRole = role) }
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
