package com.audiolifelog.app.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.audiolifelog.app.data.db.entity.AudioEventEntity
import com.audiolifelog.app.data.repository.AudioEventRepository
import com.audiolifelog.app.util.DateTimeUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import javax.inject.Inject

data class TimelineUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val eventsByHour: Map<Int, List<AudioEventEntity>> = emptyMap()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val repository: AudioEventRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<TimelineUiState> = _selectedDate
        .flatMapLatest { date ->
            val startMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
            repository.getEventsInRange(startMillis, endMillis).map { events ->
                val grouped = events.groupBy { event ->
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = event.timestamp
                    calendar.get(Calendar.HOUR_OF_DAY)
                }.toSortedMap()
                TimelineUiState(
                    selectedDate = date,
                    eventsByHour = grouped
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TimelineUiState()
        )

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }
}
