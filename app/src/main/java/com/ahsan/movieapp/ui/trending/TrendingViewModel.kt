package com.ahsan.movieapp.ui.trending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ahsan.movieapp.data.repository.MovieRepository
import com.ahsan.movieapp.domain.model.Movie
import com.ahsan.movieapp.domain.model.MovieCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Phase 4 (pagination) Round 1 — Trending is the first screen converted from a single cached
 * snapshot ([com.ahsan.movieapp.util.Resource]-wrapped list) to an infinite-scroll
 * [PagingData] stream. [MovieRepository.getPagedCategory] owns the offline-first/staleness and
 * TMDB-paging logic; [androidx.paging.Pager]'s own loading/error states (surfaced to the UI via
 * `LazyPagingItems.loadState`) replace this ViewModel's old hand-rolled isLoading/errorMessage
 * fields and refreshTrigger-based retry.
 */
@HiltViewModel
class TrendingViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    // .cachedIn(viewModelScope) is what survives a configuration change / recomposition without
    // re-running the whole Pager — the same role WhileSubscribed(5_000) played for the old
    // StateFlow-based screens, but Paging 3's own idiom for it.
    val pagedMovies: Flow<PagingData<Movie>> =
        repository.getPagedCategory(MovieCategory.TRENDING_TODAY).cachedIn(viewModelScope)

    fun toggleFavorite(movie: Movie) {
        viewModelScope.launch { repository.toggleFavorite(movie) }
    }
}
