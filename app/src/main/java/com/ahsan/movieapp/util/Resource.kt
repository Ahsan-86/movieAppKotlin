package com.ahsan.movieapp.util

/**
 * Wraps a value together with where it came from, so the UI can show
 * "loading from network" vs "showing cached data while offline" differently.
 */
sealed class Resource<out T>(
    val data: T? = null,
    val message: String? = null,
    val isFromCache: Boolean = false
) {
    class Loading<T>(data: T? = null) : Resource<T>(data = data)
    class Success<T>(data: T, isFromCache: Boolean = false) : Resource<T>(data = data, isFromCache = isFromCache)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data = data, message = message)
}
