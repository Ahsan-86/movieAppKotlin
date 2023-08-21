package com.ahsan.kotlinpractice.models

data class MovieMetadata(
    val page: Int,
    val results: List<Movie>,
    val total_pages: Int,
    val total_results: Int
)