package com.audiolifelog.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.audiolifelog.app.ui.navigation.AppNavigation
import com.audiolifelog.app.ui.theme.AudioLifeLogTheme
import com.audiolifelog.app.worker.DbCleanupWorker
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        scheduleDbCleanup()

        setContent {
            AudioLifeLogTheme {
                AppNavigation()
            }
        }
    }

    private fun scheduleDbCleanup() {
        val cleanupRequest = PeriodicWorkRequestBuilder<DbCleanupWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            CLEANUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )
    }

    companion object {
        private const val CLEANUP_WORK_NAME = "db_cleanup_work"
    }
}
