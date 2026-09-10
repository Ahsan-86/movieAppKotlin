package com.ahsan.movieapp.util

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Turns a raw exception from a failed network refresh into a short, actionable message instead of
 * whatever Java's exception message happens to be (often blank, or something like "failed to
 * connect to api.themoviedb.org" that reads like a stack trace). Used everywhere
 * [networkBoundResource] swallows a failed `fetch()` and falls back to showing cached data, so the
 * person always sees *why* they're looking at saved data instead of fresh results.
 */
fun Throwable.toUserMessage(): String = when (this) {
    is UnknownHostException, is ConnectException -> "No internet connection — showing saved data"
    is SocketTimeoutException -> "Request timed out — showing saved data"
    is IOException -> "Network error — showing saved data"
    else -> message?.takeIf { it.isNotBlank() } ?: "Something went wrong — showing saved data"
}
