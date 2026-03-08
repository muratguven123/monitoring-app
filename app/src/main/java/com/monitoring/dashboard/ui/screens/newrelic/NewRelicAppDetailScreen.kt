package com.monitoring.dashboard.ui.screens.newrelic

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monitoring.dashboard.ui.components.ErrorMessage
import com.monitoring.dashboard.ui.components.LoadingIndicator
import com.monitoring.dashboard.ui.components.MetricItem
import com.monitoring.dashboard.ui.components.ServiceHealth
import com.monitoring.dashboard.ui.components.ServiceStatusCard
import com.monitoring.dashboard.ui.theme.StatusCritical
import com.monitoring.dashboard.ui.theme.StatusWarning

@Composable
fun NewRelicAppDetailScreen(
    onBackClick: () -> Unit,
    viewModel: NewRelicAppDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = uiState.application?.name ?: "Application",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
        }

        when {
            uiState.isLoading -> LoadingIndicator()
            uiState.errorMessage != null -> ErrorMessage(
                message = uiState.errorMessage!!,
                onRetry = viewModel::loadAppDetail,
            )
            uiState.application != null -> {
                val app = uiState.application!!

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Health status
                    item {
                        ServiceStatusCard(
                            serviceName = app.name,
                            serviceType = app.language ?: "Application",
                            health = when (app.healthStatus) {
                                "green" -> ServiceHealth.HEALTHY
                                "orange" -> ServiceHealth.WARNING
                                "red" -> ServiceHealth.CRITICAL
                                else -> ServiceHealth.UNKNOWN
                            },
                            statusText = when (app.healthStatus) {
                                "green" -> "Healthy"
                                "orange" -> "Warning"
                                "red" -> "Critical"
                                else -> "Not Reporting"
                            },
                            details = if (app.reporting) "Reporting" else "Not Reporting",
                        )
                    }

                    // Application Summary Metrics
                    app.applicationSummary?.let { summary ->
                        item {
                            Text(
                                text = "Performance Summary",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                    ) {
                                        summary.responseTime?.let {
                                            MetricItem(
                                                label = "Response Time",
                                                value = "${String.format("%.0f", it)}ms",
                                            )
                                        }
                                        summary.throughput?.let {
                                            MetricItem(
                                                label = "Throughput",
                                                value = "${String.format("%.1f", it)} rpm",
                                            )
                                        }
                                        summary.errorRate?.let {
                                            MetricItem(
                                                label = "Error Rate",
                                                value = "${String.format("%.2f", it)}%",
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                    ) {
                                        summary.apdexScore?.let {
                                            MetricItem(
                                                label = "Apdex Score",
                                                value = String.format("%.2f", it),
                                            )
                                        }
                                        summary.hostCount?.let {
                                            MetricItem(
                                                label = "Hosts",
                                                value = it.toString(),
                                            )
                                        }
                                        summary.instanceCount?.let {
                                            MetricItem(
                                                label = "Instances",
                                                value = it.toString(),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // End User Summary
                    app.endUserSummary?.let { endUser ->
                        item {
                            Text(
                                text = "End User Summary",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                ) {
                                    endUser.responseTime?.let {
                                        MetricItem(
                                            label = "Page Load",
                                            value = "${String.format("%.0f", it)}ms",
                                        )
                                    }
                                    endUser.throughput?.let {
                                        MetricItem(
                                            label = "Throughput",
                                            value = "${String.format("%.1f", it)} ppm",
                                        )
                                    }
                                    endUser.apdexScore?.let {
                                        MetricItem(
                                            label = "Apdex",
                                            value = String.format("%.2f", it),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Open Violations
                    if (uiState.violations.isNotEmpty()) {
                        item {
                            Text(
                                text = "Open Violations (${uiState.violations.size})",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }

                        items(uiState.violations) { violation ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = if (violation.priority == "critical")
                                            Icons.Default.Error else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (violation.priority == "critical")
                                            StatusCritical else StatusWarning,
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = violation.conditionName ?: "Alert",
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                        violation.policyName?.let {
                                            Text(
                                                text = it,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}
