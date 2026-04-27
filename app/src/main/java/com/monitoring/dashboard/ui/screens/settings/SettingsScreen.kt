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
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monitoring.dashboard.R

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
                text = stringResource(R.string.screen_settings_title),
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        // ── Grafana Settings ────────────────────────────────────────
        item {
            Text(
                text = stringResource(R.string.settings_grafana_section),
                style = MaterialTheme.typography.titleMedium,
                color = com.monitoring.dashboard.ui.theme.GrafanaOrange,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        item {
            OutlinedTextField(
                value = uiState.grafanaBaseUrl,
                onValueChange = viewModel::onGrafanaBaseUrlChanged,
                label = { Text(stringResource(R.string.settings_grafana_url_label)) },
                placeholder = { Text(stringResource(R.string.settings_grafana_url_placeholder)) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            OutlinedTextField(
                value = uiState.grafanaApiKey,
                onValueChange = viewModel::onGrafanaApiKeyChanged,
                label = { Text(stringResource(R.string.settings_grafana_key_label)) },
                placeholder = { Text(stringResource(R.string.settings_grafana_key_placeholder)) },
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // ── New Relic Settings ──────────────────────────────────────
        item {
            Text(
                text = stringResource(R.string.settings_newrelic_section),
                style = MaterialTheme.typography.titleMedium,
                color = com.monitoring.dashboard.ui.theme.NewRelicGreen,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        item {
            OutlinedTextField(
                value = uiState.newRelicApiKey,
                onValueChange = viewModel::onNewRelicApiKeyChanged,
                label = { Text(stringResource(R.string.settings_newrelic_key_label)) },
                placeholder = { Text(stringResource(R.string.settings_newrelic_key_placeholder)) },
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
                label = { Text(stringResource(R.string.settings_newrelic_account_label)) },
                placeholder = { Text(stringResource(R.string.settings_newrelic_account_placeholder)) },
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
                Text(
                    if (uiState.isSaved) stringResource(R.string.action_saved)
                    else stringResource(R.string.action_save),
                )
            }
        }

        item {
            OutlinedButton(
                onClick = viewModel::clearAllSettings,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(R.string.action_clear_all))
            }
        }

        // ── Save error feedback ─────────────────────────────────────
        if (uiState.saveError) {
            item {
                Text(
                    text = "Ayarlar kaydedilemedi. Lütfen tekrar deneyin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB00020),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.settings_security_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
