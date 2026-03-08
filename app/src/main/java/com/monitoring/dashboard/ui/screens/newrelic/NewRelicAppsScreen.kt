package com.monitoring.dashboard.ui.screens.newrelic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monitoring.dashboard.ui.components.EmptyState
import com.monitoring.dashboard.ui.components.ErrorMessage
import com.monitoring.dashboard.ui.components.LoadingIndicator
import com.monitoring.dashboard.ui.components.MonitoringCard
import com.monitoring.dashboard.ui.components.StatusIndicator
import com.monitoring.dashboard.ui.theme.NewRelicGreen
import com.monitoring.dashboard.ui.theme.StatusCritical
import com.monitoring.dashboard.ui.theme.StatusGray
import com.monitoring.dashboard.ui.theme.StatusHealthy
import com.monitoring.dashboard.ui.theme.StatusWarning

@Composable
fun NewRelicAppsScreen(
    onAppClick: (Long) -> Unit,
    viewModel: NewRelicAppsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp),
    ) {
        Text(
            text = "New Relic Applications",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::onSearchQueryChanged,
            placeholder = { Text("Search applications...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        when {
            uiState.isLoading -> LoadingIndicator()
            uiState.errorMessage != null -> ErrorMessage(
                message = uiState.errorMessage!!,
                onRetry = viewModel::loadApplications,
            )
            uiState.applications.isEmpty() -> EmptyState("No applications found")
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.applications) { app ->
                        val healthColor = when (app.healthStatus) {
                            "green" -> StatusHealthy
                            "orange" -> StatusWarning
                            "red" -> StatusCritical
                            else -> StatusGray
                        }

                        MonitoringCard(
                            title = app.name,
                            subtitle = buildString {
                                app.language?.let { append(it) }
                                app.applicationSummary?.let { summary ->
                                    summary.responseTime?.let {
                                        if (isNotEmpty()) append(" | ")
                                        append("${String.format("%.0f", it)}ms")
                                    }
                                    summary.throughput?.let {
                                        append(" | ${String.format("%.1f", it)} rpm")
                                    }
                                    summary.errorRate?.let {
                                        append(" | ${String.format("%.2f", it)}% err")
                                    }
                                }
                            }.ifEmpty { "No data reported" },
                            icon = Icons.Default.Insights,
                            iconTint = NewRelicGreen,
                            trailingContent = {
                                StatusIndicator(
                                    color = healthColor,
                                    label = app.healthStatus?.replaceFirstChar { it.uppercase() } ?: "N/A",
                                )
                            },
                            onClick = { onAppClick(app.id) },
                        )
                    }
                }
            }
        }
    }
}
