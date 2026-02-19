package com.audiolifelog.app.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.audiolifelog.app.data.db.dao.DailySummary
import com.audiolifelog.app.data.repository.AudioEventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

enum class Period(val label: String) {
    DAY("Day"),
    WEEK("Week"),
    MONTH("Month")
}

data class PieChartSlice(
    val label: String,
    val value: Float,
    val count: Int
)

data class StatisticsUiState(
    val selectedPeriod: Period = Period.DAY,
    val pieChartData: List<PieChartSlice> = emptyList(),
    val summaryList: List<DailySummary> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val repository: AudioEventRepository
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(Period.DAY)

    val uiState: StateFlow<StatisticsUiState> = _selectedPeriod
        .flatMapLatest { period ->
            val (startTime, endTime) = calculateTimeRange(period)
            repository.getDailySummary(startTime, endTime).map { summaries ->
                val totalCount = summaries.sumOf { it.count }
                val pieData = summaries.map { summary ->
                    PieChartSlice(
                        label = summary.eventLabel,
                        value = if (totalCount > 0) summary.count.toFloat() / totalCount else 0f,
                        count = summary.count
                    )
                }
                StatisticsUiState(
                    selectedPeriod = period,
                    pieChartData = pieData,
                    summaryList = summaries
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatisticsUiState()
        )

    fun selectPeriod(period: Period) {
        _selectedPeriod.value = period
    }

    private fun calculateTimeRange(period: Period): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        when (period) {
            Period.DAY -> { /* already at start of today */ }
            Period.WEEK -> calendar.add(Calendar.DAY_OF_YEAR, -7)
            Period.MONTH -> calendar.add(Calendar.MONTH, -1)
        }
        val startTime = calendar.timeInMillis

        return startTime to endTime
    }
}
