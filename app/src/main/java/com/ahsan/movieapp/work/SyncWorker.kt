package com.ahsan.movieapp.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ahsan.movieapp.data.repository.MovieRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Periodically refreshes every movie list category into Room in the background, so the app
 * has something recent to show offline even if the person hasn't opened it in a while.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: MovieRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val outcome = repository.refreshAllCategories()
        return if (outcome.isSuccess) Result.success() else Result.retry()
    }
}
