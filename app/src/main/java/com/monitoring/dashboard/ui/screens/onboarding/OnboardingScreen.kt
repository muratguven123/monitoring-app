package com.monitoring.dashboard.ui.screens.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monitoring.dashboard.R
import com.monitoring.dashboard.ui.TestTags
import com.monitoring.dashboard.ui.screens.settings.NewRelicRegionSelector
import com.monitoring.dashboard.ui.screens.settings.SettingsViewModel
import com.monitoring.dashboard.ui.theme.GrafanaOrange
import com.monitoring.dashboard.ui.theme.NewRelicGreen

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var step by remember { mutableIntStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        step = 3
    }

    LaunchedEffect(uiState.connectionSuccess) {
        if (uiState.connectionSuccess == true && step == 3) {
            onComplete()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.onboarding_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (step + 1) / 4f },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        when (step) {
            0 -> {
                item {
                    Text(
                        text = stringResource(R.string.settings_grafana_section),
                        style = MaterialTheme.typography.titleMedium,
                        color = GrafanaOrange,
                    )
                }
                item {
                    OutlinedTextField(
                        value = uiState.grafanaBaseUrl,
                        onValueChange = viewModel::onGrafanaBaseUrlChanged,
                        label = { Text(stringResource(R.string.settings_grafana_url_label)) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(TestTags.GRAFANA_URL_FIELD),
                    )
                }
                item {
                    OutlinedTextField(
                        value = uiState.grafanaApiKey,
                        onValueChange = viewModel::onGrafanaApiKeyChanged,
                        label = { Text(stringResource(R.string.settings_grafana_key_label)) },
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(TestTags.GRAFANA_KEY_FIELD),
                    )
                }
                item {
                    Button(
                        onClick = { step = 1 },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(stringResource(R.string.action_next))
                    }
                }
                item {
                    TextButton(onClick = { step = 1 }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.action_skip_grafana))
                    }
                }
            }
            1 -> {
                item {
                    Text(
                        text = stringResource(R.string.settings_newrelic_section),
                        style = MaterialTheme.typography.titleMedium,
                        color = NewRelicGreen,
                    )
                }
                item {
                    OutlinedTextField(
                        value = uiState.newRelicApiKey,
                        onValueChange = viewModel::onNewRelicApiKeyChanged,
                        label = { Text(stringResource(R.string.settings_newrelic_key_label)) },
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(TestTags.NEW_RELIC_KEY_FIELD),
                    )
                }
                item {
                    OutlinedTextField(
                        value = uiState.newRelicAccountId,
                        onValueChange = viewModel::onNewRelicAccountIdChanged,
                        label = { Text(stringResource(R.string.settings_newrelic_account_label)) },
                        supportingText = {
                            Text(stringResource(R.string.settings_newrelic_account_helper))
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    NewRelicRegionSelector(
                        selected = uiState.newRelicRegion,
                        onSelect = viewModel::onNewRelicRegionChanged,
                    )
                }
                item {
                    Button(
                        onClick = { step = 2 },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(stringResource(R.string.action_next))
                    }
                }
                item {
                    TextButton(onClick = { step = 0 }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.action_back))
                    }
                }
            }
            2 -> {
                item {
                    Text(
                        text = stringResource(R.string.onboarding_notifications_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                item {
                    Text(
                        text = stringResource(R.string.onboarding_notifications_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                step = 3
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(stringResource(R.string.action_allow_notifications))
                    }
                }
                item {
                    TextButton(onClick = { step = 3 }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.action_skip_for_now))
                    }
                }
            }
            3 -> {
                item {
                    Text(
                        text = stringResource(R.string.onboarding_connect_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                item {
                    Text(
                        text = stringResource(R.string.onboarding_connect_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
                    Button(
                        onClick = viewModel::connectAndSave,
                        enabled = !uiState.isConnecting,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            if (uiState.isConnecting) {
                                stringResource(R.string.action_connecting)
                            } else {
                                stringResource(R.string.action_connect_and_save)
                            },
                        )
                    }
                }
                if (uiState.connectionSuccess == true) {
                    item {
                        Button(
                            onClick = onComplete,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(stringResource(R.string.action_get_started))
                        }
                    }
                }
                item {
                    TextButton(onClick = { step = 1 }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.action_back))
                    }
                }
            }
        }
    }
}
