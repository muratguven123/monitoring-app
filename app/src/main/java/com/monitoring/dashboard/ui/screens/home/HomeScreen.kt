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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monitoring.dashboard.R
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
    onNavigateToSettings: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onNavigateToGrafanaDashboard: (String) -> Unit,
    onNavigateToNewRelicApp: (Long) -> Unit,
    onNavigateToGithub: () -> Unit = {},
    onNavigateToNrql: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (!uiState.isConfigured) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.home_setup_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_setup_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNavigateToSettings) {
                Text(stringResource(R.string.action_configure_now))
            }
        }
        return
    }

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
                    text = stringResource(R.string.screen_home_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.home_refresh_countdown, uiState.secondsUntilRefresh),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_refresh))
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onNavigateToNrql) {
                    Text(stringResource(R.string.action_nrql))
                }
                TextButton(onClick = onNavigateToGithub) {
                    Text(stringResource(R.string.action_github_status))
                }
            }
        }

        // ── Alert Violations Banner ─────────────────────────────────
        if (uiState.openViolations.isNotEmpty()) {
            item {
                MonitoringCard(
                    title = stringResource(R.string.home_open_violations, uiState.openViolations.size),
                    subtitle = uiState.openViolations.firstOrNull()?.let {
                        "${it.policyName ?: ""} - ${it.conditionName ?: ""}"
                    },
                    icon = Icons.Default.Warning,
                    iconTint = StatusCritical,
                    onClick = onNavigateToAlerts,
                )
            }
        }

        if (uiState.watchlistDashboards.isNotEmpty() || uiState.watchlistApps.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.home_watchlist),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(uiState.watchlistDashboards) { dashboard ->
                MonitoringCard(
                    title = dashboard.title,
                    subtitle = dashboard.folderTitle ?: stringResource(R.string.grafana_folder_default),
                    icon = Icons.Default.MonitorHeart,
                    iconTint = GrafanaOrange,
                    onClick = { onNavigateToGrafanaDashboard(dashboard.uid) },
                )
            }
            items(uiState.watchlistApps) { app ->
                MonitoringCard(
                    title = app.name,
                    subtitle = app.language ?: stringResource(R.string.status_no_data),
                    icon = Icons.Default.Insights,
                    iconTint = NewRelicGreen,
                    onClick = { onNavigateToNewRelicApp(app.id) },
                )
            }
        }

        // ── Service Status ──────────────────────────────────────────
        item {
            Text(
                text = stringResource(R.string.home_service_status),
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
                    serviceName = stringResource(R.string.nav_grafana),
                    serviceType = stringResource(R.string.service_type_dashboards_metrics),
                    health = if (uiState.grafanaHealth != null) {
                        if (uiState.grafanaHealth?.database == "ok") ServiceHealth.HEALTHY
                        else ServiceHealth.WARNING
                    } else if (uiState.grafanaHealthError != null) {
                        ServiceHealth.CRITICAL
                    } else {
                        ServiceHealth.UNKNOWN
                    },
                    statusText = when {
                        uiState.grafanaHealth?.database == "ok" -> stringResource(R.string.status_connected)
                        uiState.grafanaHealthError != null -> stringResource(R.string.status_disconnected)
                        else -> stringResource(R.string.status_unknown)
                    },
                    details = uiState.grafanaHealth?.version?.let { "v$it" },
                    onClick = onNavigateToGrafana,
                    modifier = Modifier.weight(1f),
                )

                ServiceStatusCard(
                    serviceName = stringResource(R.string.nav_newrelic),
                    serviceType = stringResource(R.string.service_type_apm_alerts),
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
                        uiState.newRelicApps.isNotEmpty() -> stringResource(R.string.home_apps_count, uiState.newRelicApps.size)
                        uiState.newRelicAppsError != null -> stringResource(R.string.status_disconnected)
                        else -> stringResource(R.string.status_no_api_key)
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
                    title = stringResource(R.string.home_grafana_dashboards),
                    onSeeAll = onNavigateToGrafana,
                )
            }
            items(uiState.grafanaDashboards) { dashboard ->
                MonitoringCard(
                    title = dashboard.title,
                    subtitle = dashboard.folderTitle ?: stringResource(R.string.grafana_folder_default),
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
                    title = stringResource(R.string.home_newrelic_apps),
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
                    }.ifEmpty { stringResource(R.string.status_no_data) },
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
                            label = app.healthStatus?.replaceFirstChar { it.uppercase() } ?: stringResource(R.string.status_na),
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
            Text(stringResource(R.string.action_see_all))
        }
    }
}
