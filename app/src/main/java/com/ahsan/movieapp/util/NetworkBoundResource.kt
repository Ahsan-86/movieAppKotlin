package com.ahsan.movieapp.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Classic offline-first pattern: always emit what's in the local database first (instant,
 * works offline), then try to refresh from the network in the background and save the
 * result back into the database, which re-emits automatically through [query]'s Flow.
 *
 * This is what makes every screen in the app "seamlessly offline": the UI only ever talks
 * to Room; the network is just something that occasionally refills Room. If the network
 * call fails (no connection, TMDB down, etc.) we keep collecting [query] and simply flag
 * the emission as [Resource.Error] while still showing whatever is cached.
 */
inline fun <ResultType, RequestType> networkBoundResource(
    crossinline query: () -> Flow<ResultType>,
    crossinline fetch: suspend () -> RequestType,
    crossinline saveFetchResult: suspend (RequestType) -> Unit,
    crossinline shouldFetch: suspend (ResultType) -> Boolean = { true }
): Flow<Resource<ResultType>> = flow {
    val localData = query().first()

    if (shouldFetch(localData)) {
        emit(Resource.Loading(localData))
        try {
            saveFetchResult(fetch())
            emitAll(query().map { Resource.Success(it) })
        } catch (throwable: Throwable) {
            emitAll(query().map {
                Resource.Error(throwable.toUserMessage(), it)
            })
        }
    } else {
        emitAll(query().map { Resource.Success(it, isFromCache = true) })
    }
}
