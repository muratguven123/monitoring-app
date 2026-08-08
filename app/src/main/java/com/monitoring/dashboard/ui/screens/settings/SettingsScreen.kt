package com.monitoring.dashboard.ui.screens.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monitoring.dashboard.R
import com.monitoring.dashboard.data.local.SecurePreferencesManager
import com.monitoring.dashboard.ui.theme.GrafanaOrange
import com.monitoring.dashboard.ui.theme.NewRelicGreen

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

        item {
            Text(
                text = stringResource(R.string.settings_environment_section),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    SecurePreferencesManager.PROFILE_DEFAULT to R.string.profile_default,
                    SecurePreferencesManager.PROFILE_STAGING to R.string.profile_staging,
                    SecurePreferencesManager.PROFILE_PROD to R.string.profile_prod,
                ).forEach { (id, labelRes) ->
                    FilterChip(
                        selected = uiState.activeProfileId == id,
                        onClick = { viewModel.switchProfile(id) },
                        label = { Text(stringResource(labelRes)) },
                    )
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.settings_grafana_section),
                style = MaterialTheme.typography.titleMedium,
                color = GrafanaOrange,
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

        item {
            Text(
                text = stringResource(R.string.settings_newrelic_section),
                style = MaterialTheme.typography.titleMedium,
                color = NewRelicGreen,
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
                supportingText = { Text(stringResource(R.string.settings_newrelic_account_helper)) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            Text(
                text = stringResource(R.string.settings_github_section),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        item {
            OutlinedTextField(
                value = uiState.githubToken,
                onValueChange = viewModel::onGithubTokenChanged,
                label = { Text(stringResource(R.string.settings_github_token_label)) },
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = uiState.githubRepo,
                onValueChange = viewModel::onGithubRepoChanged,
                label = { Text(stringResource(R.string.settings_github_repo_label)) },
                placeholder = { Text(stringResource(R.string.settings_github_repo_placeholder)) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            Text(
                text = stringResource(R.string.settings_notifications_section),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        item {
            PreferenceSwitch(
                title = stringResource(R.string.settings_critical_only),
                checked = uiState.notificationPreferences.criticalOnly,
                onCheckedChange = viewModel::setCriticalOnly,
            )
        }
        item {
            PreferenceSwitch(
                title = stringResource(R.string.settings_quiet_hours),
                checked = uiState.notificationPreferences.quietHoursEnabled,
                onCheckedChange = viewModel::setQuietHoursEnabled,
            )
        }
        item {
            PreferenceSwitch(
                title = stringResource(R.string.settings_app_lock),
                checked = uiState.appLockEnabled,
                onCheckedChange = viewModel::setAppLockEnabled,
            )
        }

        item {
            Text(
                text = stringResource(R.string.settings_thresholds_section),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.settings_thresholds_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            ThresholdField(
                label = stringResource(R.string.metric_apdex_score) + " green ≥",
                value = uiState.metricThresholds.apdexGreen.toString(),
                onValueChange = { v ->
                    v.toDoubleOrNull()?.let { d ->
                        viewModel.updateThresholds(uiState.metricThresholds.copy(apdexGreen = d))
                    }
                },
            )
        }
        item {
            ThresholdField(
                label = stringResource(R.string.metric_response_time) + " green < ms",
                value = uiState.metricThresholds.responseTimeGreenMs.toInt().toString(),
                onValueChange = { v ->
                    v.toDoubleOrNull()?.let { d ->
                        viewModel.updateThresholds(uiState.metricThresholds.copy(responseTimeGreenMs = d))
                    }
                },
            )
        }
        item {
            ThresholdField(
                label = stringResource(R.string.metric_error_rate) + " green < %",
                value = uiState.metricThresholds.errorRateGreen.toString(),
                onValueChange = { v ->
                    v.toDoubleOrNull()?.let { d ->
                        viewModel.updateThresholds(uiState.metricThresholds.copy(errorRateGreen = d))
                    }
                },
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = viewModel::connectAndSave,
                enabled = !uiState.isConnecting,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    when {
                        uiState.isConnecting -> stringResource(R.string.action_connecting)
                        uiState.connectionSuccess == true -> stringResource(R.string.action_saved)
                        else -> stringResource(R.string.action_connect_and_save)
                    },
                )
            }
        }
        uiState.connectionMessage?.let { message ->
            item {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (uiState.connectionSuccess == true) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }
        }
        item {
            OutlinedButton(
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
        if (uiState.saveError) {
            item {
                Text(
                    text = stringResource(R.string.error_settings_save),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
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

@Composable
private fun PreferenceSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ThresholdField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
