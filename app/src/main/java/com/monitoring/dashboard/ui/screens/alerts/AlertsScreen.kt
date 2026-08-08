package com.monitoring.dashboard.ui.screens.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monitoring.dashboard.R
import com.monitoring.dashboard.domain.model.AlertViolation
import com.monitoring.dashboard.ui.components.EmptyState
import com.monitoring.dashboard.ui.components.LoadingIndicator
import com.monitoring.dashboard.ui.components.MonitoringCard
import com.monitoring.dashboard.ui.theme.StatusCritical
import com.monitoring.dashboard.ui.theme.StatusWarning
import com.monitoring.dashboard.util.ShareUtils

enum class AlertFilter { ALL, OPEN, CRITICAL, RESOLVED }

@Composable
fun AlertsScreen(
    viewModel: AlertsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (uiState.isLoading && uiState.violations.isEmpty()) {
        LoadingIndicator()
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.screen_alerts_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Row {
                IconButton(
                    onClick = {
                        ShareUtils.shareText(
                            context,
                            ShareUtils.buildAlertSummary(uiState.violations.filter { it.isOpen }),
                        )
                    },
                ) {
                    Icon(Icons.Default.Share, contentDescription = stringResource(R.string.action_share))
                }
                IconButton(
                    onClick = {
                        ShareUtils.shareText(
                            context,
                            ShareUtils.buildAlertJson(uiState.violations),
                            title = context.getString(R.string.action_export),
                        )
                    },
                ) {
                    Icon(Icons.Default.Share, contentDescription = stringResource(R.string.action_export))
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = uiState.filter == AlertFilter.ALL,
                onClick = { viewModel.setFilter(AlertFilter.ALL) },
                label = { Text(stringResource(R.string.alert_filter_all)) },
            )
            FilterChip(
                selected = uiState.filter == AlertFilter.OPEN,
                onClick = { viewModel.setFilter(AlertFilter.OPEN) },
                label = { Text(stringResource(R.string.alert_filter_open)) },
            )
            FilterChip(
                selected = uiState.filter == AlertFilter.CRITICAL,
                onClick = { viewModel.setFilter(AlertFilter.CRITICAL) },
                label = { Text(stringResource(R.string.alert_filter_critical)) },
            )
            FilterChip(
                selected = uiState.filter == AlertFilter.RESOLVED,
                onClick = { viewModel.setFilter(AlertFilter.RESOLVED) },
                label = { Text(stringResource(R.string.alert_filter_resolved)) },
            )
        }

        if (uiState.filtered.isEmpty()) {
            EmptyState(message = stringResource(R.string.alerts_empty))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.filtered, key = { it.id }) { violation ->
                    AlertCard(
                        violation = violation,
                        onMute = { viewModel.muteForHours(violation.id, 4) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertCard(
    violation: AlertViolation,
    onMute: () -> Unit,
) {
    val isCritical = violation.severity.equals("critical", ignoreCase = true)
    MonitoringCard(
        title = violation.label ?: violation.conditionName ?: stringResource(R.string.severity_alert),
        subtitle = buildString {
            violation.policyName?.let { append(it) }
            if (violation.isOpen) {
                if (isNotEmpty()) append(" · ")
                append("OPEN")
            } else {
                if (isNotEmpty()) append(" · ")
                append("RESOLVED")
            }
            violation.severity?.let {
                append(" · ")
                append(it.uppercase())
            }
        },
        icon = Icons.Default.Warning,
        iconTint = if (isCritical) StatusCritical else StatusWarning,
        trailingContent = {
            if (violation.isOpen) {
                IconButton(onClick = onMute) {
                    Icon(
                        Icons.Default.NotificationsOff,
                        contentDescription = stringResource(R.string.action_mute),
                    )
                }
            }
        },
    )
}
