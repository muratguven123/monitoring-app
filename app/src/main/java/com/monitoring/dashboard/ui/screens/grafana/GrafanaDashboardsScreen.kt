package com.monitoring.dashboard.ui.screens.grafana

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonitorHeart
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
import com.monitoring.dashboard.ui.theme.GrafanaOrange

@Composable
fun GrafanaDashboardsScreen(
    onDashboardClick: (String) -> Unit,
    viewModel: GrafanaDashboardsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp),
    ) {
        Text(
            text = "Grafana Dashboards",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::onSearchQueryChanged,
            placeholder = { Text("Search dashboards...") },
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
                onRetry = viewModel::loadDashboards,
            )
            uiState.dashboards.isEmpty() -> EmptyState("No dashboards found")
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.dashboards) { dashboard ->
                        MonitoringCard(
                            title = dashboard.title,
                            subtitle = buildString {
                                dashboard.folderTitle?.let { append(it) }
                                if (dashboard.tags.isNotEmpty()) {
                                    if (isNotEmpty()) append(" | ")
                                    append(dashboard.tags.joinToString(", "))
                                }
                            }.ifEmpty { "General" },
                            icon = Icons.Default.MonitorHeart,
                            iconTint = GrafanaOrange,
                            onClick = { onDashboardClick(dashboard.uid) },
                        )
                    }
                }
            }
        }
    }
}
