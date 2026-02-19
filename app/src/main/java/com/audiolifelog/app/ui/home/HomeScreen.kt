package com.audiolifelog.app.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.audiolifelog.app.data.db.entity.AudioEventEntity
import com.audiolifelog.app.ui.theme.ActiveGreen
import com.audiolifelog.app.ui.theme.InactiveGray
import com.audiolifelog.app.util.AudioPermissionHelper
import com.audiolifelog.app.util.DateTimeUtil

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.startService(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Audio Life Log",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        StatusIndicator(isRunning = uiState.isServiceRunning)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Total events: ${uiState.totalEventCount}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        ToggleServiceButton(
            isRunning = uiState.isServiceRunning,
            onClick = {
                if (uiState.isServiceRunning) {
                    viewModel.stopService(context)
                } else {
                    val hasAudio = AudioPermissionHelper.hasRecordPermission(context)
                    val hasNotification = AudioPermissionHelper.hasNotificationPermission(context)
                    if (hasAudio && hasNotification) {
                        viewModel.startService(context)
                    } else {
                        permissionLauncher.launch(AudioPermissionHelper.requiredPermissions())
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Recent Events",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        if (uiState.recentEvents.isEmpty()) {
            EmptyState()
        } else {
            val listState = rememberLazyListState()

            LaunchedEffect(uiState.recentEvents.firstOrNull()?.id) {
                listState.animateScrollToItem(0)
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = uiState.recentEvents,
                    key = { it.id }
                ) { event ->
                    EventCard(event = event)
                }
            }
        }
    }
}

@Composable
private fun StatusIndicator(isRunning: Boolean) {
    val color by animateColorAsState(
        targetValue = if (isRunning) ActiveGreen else InactiveGray,
        label = "statusColor"
    )
    val statusText = if (isRunning) "Listening..." else "Stopped"

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(12.dp)) {
            drawCircle(color = color)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }
}

@Composable
private fun ToggleServiceButton(
    isRunning: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isRunning) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.primary,
        label = "fabColor"
    )

    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.size(80.dp),
        shape = CircleShape,
        containerColor = backgroundColor
    ) {
        Icon(
            imageVector = if (isRunning) Icons.Default.MicOff else Icons.Default.Mic,
            contentDescription = if (isRunning) "Stop monitoring" else "Start monitoring",
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
fun EventCard(event: AudioEventEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.eventLabel,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = DateTimeUtil.formatDateTime(event.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${(event.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${event.durationMs}ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No events detected yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap the button above to start monitoring",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        }
    }
}
