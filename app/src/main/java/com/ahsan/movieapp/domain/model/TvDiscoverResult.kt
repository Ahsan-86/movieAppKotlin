package com.ahsan.movieapp.domain.model

/**
 * The TV half of the search screen's first-screen filter results: the first page of `/discover/tv`
 * (capped, shown as the "TV Shows" row above the movies, like the text-search results) plus TMDB's
 * [totalResults] for the whole filter combination, so the summary can state a real count.
 */
data class TvDiscoverResult(
    val shows: List<Movie>,
    val totalResults: Int
)
