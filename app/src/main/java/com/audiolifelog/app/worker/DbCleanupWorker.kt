package com.audiolifelog.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.audiolifelog.app.data.repository.AudioEventRepository
import com.audiolifelog.app.util.DateTimeUtil
import com.audiolifelog.app.util.PreferencesManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class DbCleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: AudioEventRepository,
    private val preferencesManager: PreferencesManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val retentionDays = preferencesManager.retentionDays.first()
        val cutoffTimestamp = DateTimeUtil.daysAgo(retentionDays)
        repository.deleteOlderThan(cutoffTimestamp)
        return Result.success()
    }
}
