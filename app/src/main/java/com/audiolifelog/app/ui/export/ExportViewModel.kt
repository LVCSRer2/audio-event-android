package com.audiolifelog.app.ui.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.audiolifelog.app.data.export.CsvExporter
import com.audiolifelog.app.data.repository.AudioEventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ExportUiState(
    val startDate: LocalDate = LocalDate.now().minusDays(7),
    val endDate: LocalDate = LocalDate.now(),
    val isExporting: Boolean = false,
    val exportResult: ExportResult? = null
)

sealed class ExportResult {
    data class Success(val eventCount: Int) : ExportResult()
    data class Error(val message: String) : ExportResult()
}

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val repository: AudioEventRepository,
    private val csvExporter: CsvExporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    fun setStartDate(date: LocalDate) {
        _uiState.update { it.copy(startDate = date, exportResult = null) }
    }

    fun setEndDate(date: LocalDate) {
        _uiState.update { it.copy(endDate = date, exportResult = null) }
    }

    fun clearResult() {
        _uiState.update { it.copy(exportResult = null) }
    }

    fun export(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportResult = null) }

            try {
                val state = _uiState.value
                val startMillis = state.startDate
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                val endMillis = state.endDate
                    .plusDays(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli() - 1

                val events = repository.getEventsInRange(startMillis, endMillis).first()

                if (events.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            exportResult = ExportResult.Error("No events found in the selected date range.")
                        )
                    }
                    return@launch
                }

                val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
                val fileName = "audio_events_${state.startDate.format(formatter)}_${state.endDate.format(formatter)}.csv"
                val cacheDir = File(context.cacheDir, "exports")
                cacheDir.mkdirs()
                val csvFile = File(cacheDir, fileName)

                csvFile.outputStream().use { outputStream ->
                    csvExporter.export(events, outputStream)
                }

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    csvFile
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Audio Event Log Export")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                context.startActivity(Intent.createChooser(shareIntent, "Share CSV Export"))

                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportResult = ExportResult.Success(events.size)
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportResult = ExportResult.Error(e.message ?: "Export failed")
                    )
                }
            }
        }
    }
}
