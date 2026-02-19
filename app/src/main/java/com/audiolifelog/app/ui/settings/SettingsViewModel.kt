package com.audiolifelog.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.audiolifelog.app.data.repository.AudioEventRepository
import com.audiolifelog.app.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val repository: AudioEventRepository
) : ViewModel() {

    private val _deleteResult = MutableStateFlow<String?>(null)
    val deleteResult: StateFlow<String?> = _deleteResult.asStateFlow()

    val rmsThreshold: StateFlow<Float> = preferencesManager.rmsThreshold
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.01f)

    val minConfidence: StateFlow<Float> = preferencesManager.minConfidence
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.3f)

    val retentionDays: StateFlow<Int> = preferencesManager.retentionDays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 30)

    val nnapiEnabled: StateFlow<Boolean> = preferencesManager.nnapiEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun updateRmsThreshold(value: Float) {
        viewModelScope.launch {
            preferencesManager.setRmsThreshold(value)
        }
    }

    fun updateMinConfidence(value: Float) {
        viewModelScope.launch {
            preferencesManager.setMinConfidence(value)
        }
    }

    fun updateRetentionDays(value: Int) {
        viewModelScope.launch {
            preferencesManager.setRetentionDays(value)
        }
    }

    fun updateNnapiEnabled(value: Boolean) {
        viewModelScope.launch {
            preferencesManager.setNnapiEnabled(value)
        }
    }

    fun deleteAllLogs() {
        viewModelScope.launch {
            val count = repository.deleteAll()
            _deleteResult.value = "${count}건의 로그가 삭제되었습니다."
        }
    }

    fun clearDeleteResult() {
        _deleteResult.value = null
    }
}
