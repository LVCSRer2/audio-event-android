package com.audiolifelog.app.service

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.audiolifelog.app.audio.AudioClassificationPipeline
import com.audiolifelog.app.audio.ClassificationResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AudioMonitorService : Service() {

    @Inject lateinit var pipeline: AudioClassificationPipeline
    @Inject lateinit var notificationHelper: ServiceNotificationHelper

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val binder = LocalBinder()

    private var eventCount = 0
    private var lastEvent: String? = null

    val classificationFlow: SharedFlow<ClassificationResult>
        get() = pipeline.classificationFlow

    inner class LocalBinder : Binder() {
        fun getService(): AudioMonitorService = this@AudioMonitorService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        val notification = notificationHelper.createNotification(0, null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                ServiceNotificationHelper.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(ServiceNotificationHelper.NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        pipeline.start(serviceScope)

        serviceScope.launch {
            pipeline.classificationFlow.collect { result ->
                eventCount++
                lastEvent = result.label

                if (eventCount % 5 == 0 || eventCount == 1) {
                    val notification = notificationHelper.createNotification(eventCount, lastEvent)
                    val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                    manager.notify(ServiceNotificationHelper.NOTIFICATION_ID, notification)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        pipeline.stop()
        serviceScope.cancel()
    }

    companion object {
        fun serviceConnection(onConnected: (AudioMonitorService) -> Unit): ServiceConnection {
            return object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    val binder = service as LocalBinder
                    onConnected(binder.getService())
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    // no-op
                }
            }
        }
    }
}
