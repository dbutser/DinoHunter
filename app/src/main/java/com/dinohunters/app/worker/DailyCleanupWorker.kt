// Путь: app/src/main/java/com/dinohunters/app/worker/DailyCleanupWorker.kt

package com.dinohunters.app.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dinohunters.app.data.repository.DinoRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DailyCleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    // Hilt автоматически предоставит нам наш репозиторий
    private val dinoRepository: DinoRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "DailyZoneCleanup"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(WORK_NAME, "Starting the Great Purge...")
            dinoRepository.performGreatPurge()
            Log.d(WORK_NAME, "The Great Purge has been successfully completed.")
            Result.success()
        } catch (e: Exception) {
            Log.e(WORK_NAME, "The Great Purge failed.", e)
            // Если произошла ошибка, можно попробовать повторить позже
            Result.retry()
        }
    }
}