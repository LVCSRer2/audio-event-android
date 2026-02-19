package com.audiolifelog.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    onNavigateToExport: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val rmsThreshold by viewModel.rmsThreshold.collectAsStateWithLifecycle()
    val minConfidence by viewModel.minConfidence.collectAsStateWithLifecycle()
    val retentionDays by viewModel.retentionDays.collectAsStateWithLifecycle()
    val nnapiEnabled by viewModel.nnapiEnabled.collectAsStateWithLifecycle()
    val deleteResult by viewModel.deleteResult.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(deleteResult) {
        deleteResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearDeleteResult()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("로그 전체 삭제") },
            text = { Text("모든 오디오 이벤트 로그를 삭제합니다. 이 작업은 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllLogs()
                        showDeleteDialog = false
                    }
                ) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        SettingCard(
            label = "RMS Threshold",
            description = "Minimum audio level to trigger classification. Lower values detect quieter sounds."
        ) {
            Text(
                text = String.format("%.3f", rmsThreshold),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Slider(
                value = rmsThreshold,
                onValueChange = { viewModel.updateRmsThreshold(it) },
                valueRange = 0.001f..0.1f,
                modifier = Modifier.fillMaxWidth()
            )
        }

        SettingCard(
            label = "Min Confidence",
            description = "Minimum classification confidence to record an event. Higher values reduce false positives."
        ) {
            Text(
                text = String.format("%.2f", minConfidence),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Slider(
                value = minConfidence,
                onValueChange = {
                    val rounded = (it * 100).roundToInt() / 100f
                    viewModel.updateMinConfidence(rounded)
                },
                valueRange = 0.01f..0.99f,
                steps = 97,
                modifier = Modifier.fillMaxWidth()
            )
        }

        SettingCard(
            label = "Retention Days",
            description = "Number of days to keep events before automatic cleanup."
        ) {
            Text(
                text = "$retentionDays days",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Slider(
                value = retentionDays.toFloat(),
                onValueChange = { viewModel.updateRetentionDays(it.roundToInt()) },
                valueRange = 7f..90f,
                steps = 82,
                modifier = Modifier.fillMaxWidth()
            )
        }

        SettingCard(
            label = "NNAPI Acceleration",
            description = "Use Android Neural Networks API for hardware-accelerated inference. May improve performance on supported devices."
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (nnapiEnabled) "Enabled" else "Disabled",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Switch(
                    checked = nnapiEnabled,
                    onCheckedChange = { viewModel.updateNnapiEnabled(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onNavigateToExport,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Export Data")
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Delete All Logs")
        }

        SnackbarHost(hostState = snackbarHostState)
    }
}

@Composable
private fun SettingCard(
    label: String,
    description: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )
            content()
        }
    }
}
