package com.ahsan.movieapp.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahsan.movieapp.data.repository.MovieRepository
import com.ahsan.movieapp.domain.model.MediaType
import com.ahsan.movieapp.domain.model.Movie
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The Favorites screen's Movies / TV Shows tab (Session 6). */
enum class FavoritesTab { MOVIES, TV }

/**
 * Session 6 — Favorites now has a MediaType-aware ViewModel: movie and TV-show favorites are
 * split into the two tab lists off one shared favorites flow (a TV id and a same-numbered movie id
 * are distinct favorites now, keyed by `(id, mediaType)`), each newest-added first.
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    private val favorites = repository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoriteMovies: StateFlow<List<Movie>> = favorites
        .map { list -> list.filter { it.mediaType == MediaType.MOVIE } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoriteTvShows: StateFlow<List<Movie>> = favorites
        .map { list -> list.filter { it.mediaType == MediaType.TV } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedTab = MutableStateFlow(FavoritesTab.MOVIES)
    val selectedTab: StateFlow<FavoritesTab> = _selectedTab.asStateFlow()

    fun onTabSelected(tab: FavoritesTab) {
        _selectedTab.value = tab
    }

    fun toggleFavorite(movie: Movie) {
        viewModelScope.launch { repository.toggleFavorite(movie) }
    }
}