package com.monitoring.dashboard.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monitoring.dashboard.ui.components.LoadingIndicator
import com.monitoring.dashboard.ui.components.MonitoringCard
import com.monitoring.dashboard.ui.components.ServiceHealth
import com.monitoring.dashboard.ui.components.ServiceStatusCard
import com.monitoring.dashboard.ui.theme.GrafanaOrange
import com.monitoring.dashboard.ui.theme.NewRelicGreen
import com.monitoring.dashboard.ui.theme.StatusCritical
import com.monitoring.dashboard.ui.theme.StatusWarning

@Composable
fun HomeScreen(
    onNavigateToGrafana: () -> Unit,
    onNavigateToNewRelic: () -> Unit,
    onNavigateToGrafanaDashboard: (String) -> Unit,
    onNavigateToNewRelicApp: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading && uiState.grafanaHealth == null && uiState.newRelicApps.isEmpty()) {
        LoadingIndicator()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Header ──────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Monitoring Dashboard",
                    style = MaterialTheme.typography.headlineMedium,
                )
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        }

        // ── Alert Violations Banner ─────────────────────────────────
        if (uiState.openViolations.isNotEmpty()) {
            item {
                MonitoringCard(
                    title = "${uiState.openViolations.size} Open Alert Violations",
                    subtitle = uiState.openViolations.firstOrNull()?.let {
                        "${it.policyName ?: ""} - ${it.conditionName ?: ""}"
                    },
                    icon = Icons.Default.Warning,
                    iconTint = StatusCritical,
                    onClick = onNavigateToNewRelic,
                )
            }
        }

        // ── Service Status ──────────────────────────────────────────
        item {
            Text(
                text = "Service Status",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ServiceStatusCard(
                    serviceName = "Grafana",
                    serviceType = "Dashboards & Metrics",
                    health = if (uiState.grafanaHealth != null) {
                        if (uiState.grafanaHealth?.database == "ok") ServiceHealth.HEALTHY
                        else ServiceHealth.WARNING
                    } else if (uiState.grafanaHealthError != null) {
                        ServiceHealth.CRITICAL
                    } else {
                        ServiceHealth.UNKNOWN
                    },
                    statusText = when {
                        uiState.grafanaHealth?.database == "ok" -> "Connected"
                        uiState.grafanaHealthError != null -> "Disconnected"
                        else -> "Unknown"
                    },
                    details = uiState.grafanaHealth?.version?.let { "v$it" },
                    onClick = onNavigateToGrafana,
                    modifier = Modifier.weight(1f),
                )

                ServiceStatusCard(
                    serviceName = "New Relic",
                    serviceType = "APM & Alerts",
                    health = when {
                        uiState.newRelicApps.isNotEmpty() -> {
                            val hasRed = uiState.newRelicApps.any { it.healthStatus == "red" }
                            val hasOrange = uiState.newRelicApps.any { it.healthStatus == "orange" }
                            when {
                                hasRed -> ServiceHealth.CRITICAL
                                hasOrange -> ServiceHealth.WARNING
                                else -> ServiceHealth.HEALTHY
                            }
                        }
                        uiState.newRelicAppsError != null -> ServiceHealth.CRITICAL
                        else -> ServiceHealth.UNKNOWN
                    },
                    statusText = when {
                        uiState.newRelicApps.isNotEmpty() -> "${uiState.newRelicApps.size} Apps"
                        uiState.newRelicAppsError != null -> "Disconnected"
                        else -> "No API Key"
                    },
                    onClick = onNavigateToNewRelic,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // ── Grafana Dashboards ──────────────────────────────────────
        if (uiState.grafanaDashboards.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Grafana Dashboards",
                    onSeeAll = onNavigateToGrafana,
                )
            }
            items(uiState.grafanaDashboards) { dashboard ->
                MonitoringCard(
                    title = dashboard.title,
                    subtitle = dashboard.folderTitle ?: "General",
                    icon = Icons.Default.MonitorHeart,
                    iconTint = GrafanaOrange,
                    onClick = { onNavigateToGrafanaDashboard(dashboard.uid) },
                )
            }
        }

        // ── New Relic Applications ──────────────────────────────────
        if (uiState.newRelicApps.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "New Relic Applications",
                    onSeeAll = onNavigateToNewRelic,
                )
            }
            items(uiState.newRelicApps.take(5)) { app ->
                MonitoringCard(
                    title = app.name,
                    subtitle = buildString {
                        app.language?.let { append(it) }
                        app.applicationSummary?.responseTime?.let {
                            if (isNotEmpty()) append(" | ")
                            append("${it}ms")
                        }
                        app.applicationSummary?.errorRate?.let {
                            append(" | ${it}% errors")
                        }
                    }.ifEmpty { "No data" },
                    icon = Icons.Default.Insights,
                    iconTint = NewRelicGreen,
                    trailingContent = {
                        val color = when (app.healthStatus) {
                            "red" -> StatusCritical
                            "orange" -> StatusWarning
                            "green" -> com.monitoring.dashboard.ui.theme.StatusHealthy
                            else -> com.monitoring.dashboard.ui.theme.StatusGray
                        }
                        com.monitoring.dashboard.ui.components.StatusIndicator(
                            color = color,
                            label = app.healthStatus?.replaceFirstChar { it.uppercase() } ?: "N/A",
                        )
                    },
                    onClick = { onNavigateToNewRelicApp(app.id) },
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    onSeeAll: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        TextButton(onClick = onSeeAll) {
            Text("See All")
        }
    }
}
