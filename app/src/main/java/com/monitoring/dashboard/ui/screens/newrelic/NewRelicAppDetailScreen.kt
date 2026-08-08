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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monitoring.dashboard.R
import com.monitoring.dashboard.ui.components.ColoredMetricItem
import com.monitoring.dashboard.ui.components.ErrorMessage
import com.monitoring.dashboard.ui.components.LoadingIndicator
import com.monitoring.dashboard.ui.components.MetricItem
import com.monitoring.dashboard.ui.components.ServiceHealth
import com.monitoring.dashboard.ui.components.ServiceStatusCard
import com.monitoring.dashboard.domain.model.MetricHealth
import com.monitoring.dashboard.domain.model.MetricThresholdEvaluator
import com.monitoring.dashboard.ui.theme.StatusCritical
import com.monitoring.dashboard.ui.theme.StatusHealthy
import com.monitoring.dashboard.ui.theme.StatusWarning

@Composable
fun NewRelicAppDetailScreen(
    onBackClick: () -> Unit,
    onMetricClick: (appId: Long, metricName: String, valueKey: String, displayName: String, unit: String) -> Unit = { _, _, _, _, _ -> },
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
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            Text(
                text = uiState.application?.name ?: stringResource(R.string.screen_newrelic_app_detail_title),
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
                            serviceType = app.language ?: stringResource(R.string.screen_newrelic_app_detail_title),
                            health = when (app.healthStatus) {
                                "green" -> ServiceHealth.HEALTHY
                                "orange" -> ServiceHealth.WARNING
                                "red" -> ServiceHealth.CRITICAL
                                else -> ServiceHealth.UNKNOWN
                            },
                            statusText = when (app.healthStatus) {
                                "green" -> stringResource(R.string.status_healthy)
                                "orange" -> stringResource(R.string.status_warning)
                                "red" -> stringResource(R.string.status_critical)
                                else -> stringResource(R.string.status_not_reporting)
                            },
                            details = if (app.reporting) stringResource(R.string.status_reporting)
                                      else stringResource(R.string.status_not_reporting),
                        )
                    }

                    // Application Summary Metrics — color coded
                    app.applicationSummary?.let { summary ->
                        item {
                            Text(
                                text = stringResource(R.string.newrelic_performance_summary),
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
                                            ColoredMetricItem(
                                                label = stringResource(R.string.metric_response_time),
                                                value = "${String.format("%.0f", it)}ms",
                                                color = healthColor(
                                                    MetricThresholdEvaluator.responseTimeMs(it, uiState.thresholds),
                                                ),
                                            )
                                        }
                                        summary.throughput?.let {
                                            MetricItem(
                                                label = stringResource(R.string.metric_throughput),
                                                value = "${String.format("%.1f", it)} rpm",
                                            )
                                        }
                                        summary.errorRate?.let {
                                            ColoredMetricItem(
                                                label = stringResource(R.string.metric_error_rate),
                                                value = "${String.format("%.2f", it)}%",
                                                color = healthColor(
                                                    MetricThresholdEvaluator.errorRatePercent(it, uiState.thresholds),
                                                ),
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                    ) {
                                        summary.apdexScore?.let {
                                            ColoredMetricItem(
                                                label = stringResource(R.string.metric_apdex_score),
                                                value = String.format("%.2f", it),
                                                color = healthColor(
                                                    MetricThresholdEvaluator.apdex(it, uiState.thresholds),
                                                ),
                                            )
                                        }
                                        summary.hostCount?.let {
                                            MetricItem(
                                                label = stringResource(R.string.metric_hosts),
                                                value = it.toString(),
                                            )
                                        }
                                        summary.instanceCount?.let {
                                            MetricItem(
                                                label = stringResource(R.string.metric_instances),
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
                                text = stringResource(R.string.newrelic_end_user_summary),
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
                                        ColoredMetricItem(
                                            label = stringResource(R.string.metric_page_load),
                                            value = "${String.format("%.0f", it)}ms",
                                            color = healthColor(
                                                MetricThresholdEvaluator.pageLoadMs(it, uiState.thresholds),
                                            ),
                                        )
                                    }
                                    endUser.throughput?.let {
                                        MetricItem(
                                            label = stringResource(R.string.metric_throughput),
                                            value = "${String.format("%.1f", it)} ppm",
                                        )
                                    }
                                    endUser.apdexScore?.let {
                                        ColoredMetricItem(
                                            label = stringResource(R.string.metric_apdex),
                                            value = String.format("%.2f", it),
                                            color = apdexColor(it.toFloat()),
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
                                text = stringResource(R.string.newrelic_open_violations, uiState.violations.size),
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
                                            text = violation.conditionName
                                                ?: stringResource(R.string.severity_alert),
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

                    // ── Metric Grafikleri ─────────────────────────────────
                    item {
                        Text(
                            text = "Performans Grafikleri",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    val charts = listOf(
                        uiState.responseTimeChart,
                        uiState.throughputChart,
                        uiState.errorRateChart,
                        uiState.apdexChart,
                    )

                    items(charts) { chartData ->
                        MetricChartCard(
                            chartData = chartData,
                            onClick   = {
                                onMetricClick(
                                    viewModel.appId,
                                    chartData.metricName,
                                    chartData.valueKey,
                                    chartData.displayName,
                                    chartData.unit,
                                )
                            },
                        )
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

private fun healthColor(health: MetricHealth): Color = when (health) {
    MetricHealth.HEALTHY -> StatusHealthy
    MetricHealth.WARNING -> StatusWarning
    MetricHealth.CRITICAL -> StatusCritical
}
