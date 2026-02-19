package com.audiolifelog.app.ui.home

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.audiolifelog.app.data.db.entity.AudioEventEntity
import com.audiolifelog.app.data.repository.AudioEventRepository
import com.audiolifelog.app.service.AudioMonitorService
import com.audiolifelog.app.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val isServiceRunning: Boolean = false,
    val recentEvents: List<AudioEventEntity> = emptyList(),
    val totalEventCount: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AudioEventRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _isServiceRunning = MutableStateFlow(false)

    val uiState: StateFlow<HomeUiState> = combine(
        _isServiceRunning,
        repository.getRecentEvents(20),
        repository.getEventCount()
    ) { isRunning, recentEvents, totalCount ->
        HomeUiState(
            isServiceRunning = isRunning,
            recentEvents = recentEvents,
            totalEventCount = totalCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun startService(context: Context) {
        val intent = Intent(context, AudioMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        _isServiceRunning.value = true
    }

    fun stopService(context: Context) {
        val intent = Intent(context, AudioMonitorService::class.java)
        context.stopService(intent)
        _isServiceRunning.value = false
    }
}
