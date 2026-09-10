package com.ahsan.movieapp.ui.genre

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ahsan.movieapp.data.repository.MovieRepository
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.ui.person.MediaTab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

data class GenreUiState(
    val genreName: String,
    val selectedMediaType: MediaTab,
    val showMoviesTab: Boolean,
    val showTvTab: Boolean
)

/**
 * Backs the full-screen genre browse view opened by tapping a genre chip on the search screen.
 * Both tabs are Paging 3 infinite-scroll as of Phase 4 Round 3: Movies goes through
 * [MovieRepository.getPagedGenre] (Room + [androidx.paging.RemoteMediator], Round 2); TV goes
 * through [MovieRepository.getPagedGenreTv] (a plain network-only
 * [androidx.paging.PagingSource], Round 3 — this app doesn't persist TV data, so there's no Room
 * table for a `RemoteMediator` to page into). Paging 3's own loadState carries all
 * loading/error/empty state for both tabs now, so [GenreUiState] only tracks which tab is
 * selected and which tabs this chip actually has.
 */
@HiltViewModel
class GenreViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MovieRepository
) : ViewModel() {

    private val movieGenreId: Int? = (savedStateHandle.get<Int>("movieGenreId") ?: -1).takeIf { it != -1 }
    private val tvGenreId: Int? = (savedStateHandle.get<Int>("tvGenreId") ?: -1).takeIf { it != -1 }
    private val genreName: String = URLDecoder.decode(checkNotNull(savedStateHandle.get<String>("genreName")), "UTF-8")

    // Null when this chip has no movie genre at all (a TV-only chip) — nothing to page there.
    val pagedMovies: Flow<PagingData<Movie>>? =
        movieGenreId?.let { repository.getPagedGenre(it).cachedIn(viewModelScope) }

    // Null when this chip has no TV genre at all (e.g. Horror/Romance/Thriller, which are
    // movie-only on TMDB) — same reasoning as pagedMovies above.
    val pagedTvShows: Flow<PagingData<Movie>>? =
        tvGenreId?.let { repository.getPagedGenreTv(it).cachedIn(viewModelScope) }

    private val _uiState = MutableStateFlow(
        GenreUiState(
            genreName = genreName,
            selectedMediaType = if (movieGenreId != null) MediaTab.MOVIES else MediaTab.TV,
            showMoviesTab = movieGenreId != null,
            showTvTab = tvGenreId != null
        )
    )
    val uiState: StateFlow<GenreUiState> = _uiState.asStateFlow()

    fun onMediaTabSelected(tab: MediaTab) {
        _uiState.update { it.copy(selectedMediaType = tab) }
    }

    fun toggleFavorite(movie: Movie) {
        // No optimistic state update needed here: the paged Movies tab reads through Room, whose
        // PagingSource query already joins `favorites` — a toggle re-invalidates it automatically
        // and Paging 3 diffs in just the changed row. (TV shows can't be favorited yet.)
        viewModelScope.launch { repository.toggleFavorite(movie) }
    }
}
