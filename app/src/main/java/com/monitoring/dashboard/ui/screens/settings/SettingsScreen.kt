package com.monitoring.dashboard.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        // ── Grafana Settings ────────────────────────────────────────
        item {
            Text(
                text = "Grafana Configuration",
                style = MaterialTheme.typography.titleMedium,
                color = com.monitoring.dashboard.ui.theme.GrafanaOrange,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        item {
            OutlinedTextField(
                value = uiState.grafanaBaseUrl,
                onValueChange = viewModel::onGrafanaBaseUrlChanged,
                label = { Text("Grafana Base URL") },
                placeholder = { Text("https://grafana.example.com") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            OutlinedTextField(
                value = uiState.grafanaApiKey,
                onValueChange = viewModel::onGrafanaApiKeyChanged,
                label = { Text("Grafana API Key") },
                placeholder = { Text("Bearer token or API key") },
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // ── New Relic Settings ──────────────────────────────────────
        item {
            Text(
                text = "New Relic Configuration",
                style = MaterialTheme.typography.titleMedium,
                color = com.monitoring.dashboard.ui.theme.NewRelicGreen,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        item {
            OutlinedTextField(
                value = uiState.newRelicApiKey,
                onValueChange = viewModel::onNewRelicApiKeyChanged,
                label = { Text("New Relic API Key") },
                placeholder = { Text("User API key (NRAK-...)") },
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            OutlinedTextField(
                value = uiState.newRelicAccountId,
                onValueChange = viewModel::onNewRelicAccountIdChanged,
                label = { Text("New Relic Account ID") },
                placeholder = { Text("e.g. 1234567") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // ── Actions ─────────────────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = viewModel::saveSettings,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(if (uiState.isSaved) "Saved!" else "Save Settings")
            }
        }

        item {
            OutlinedButton(
                onClick = viewModel::clearAllSettings,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Clear All Settings")
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "API keys are stored securely using Android EncryptedSharedPreferences.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
