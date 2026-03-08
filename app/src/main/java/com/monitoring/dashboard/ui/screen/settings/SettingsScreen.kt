package com.monitoring.dashboard.ui.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monitoring.dashboard.ui.theme.GreenOk
import com.monitoring.dashboard.ui.theme.RedError

@Composable
fun SettingsScreen(
    onSetupComplete: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigate out when connection succeeds
    LaunchedEffect(uiState.connectionStatus) {
        if (uiState.connectionStatus is ConnectionStatus.Success) {
            onSetupComplete()
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // ── Header ───────────────────────────────────────────────────
            Spacer(Modifier.height(32.dp))

            Text(
                text  = "Monitoring",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text  = "Dashboard",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text  = "Grafana sunucunuza bağlanın",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(40.dp))

            // ── URL Field ─────────────────────────────────────────────────
            val focusManager = LocalFocusManager.current

            OutlinedTextField(
                value         = uiState.baseUrl,
                onValueChange = viewModel::onBaseUrlChange,
                label         = { Text("Grafana Base URL") },
                placeholder   = { Text("https://grafana.example.com") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction    = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
            )

            Spacer(Modifier.height(16.dp))

            // ── API Key Field ─────────────────────────────────────────────
            var showApiKey by remember { mutableStateOf(false) }

            OutlinedTextField(
                value         = uiState.apiKey,
                onValueChange = viewModel::onApiKeyChange,
                label         = { Text("API Key") },
                placeholder   = { Text("glsa_xxxxxxxxxxxx") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                visualTransformation = if (showApiKey) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction    = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        viewModel.testAndSave(onSetupComplete)
                    }
                ),
                trailingIcon = {
                    IconButton(onClick = { showApiKey = !showApiKey }) {
                        Icon(
                            imageVector = if (showApiKey) Icons.Default.VisibilityOff
                                          else Icons.Default.Visibility,
                            contentDescription = if (showApiKey) "Gizle" else "Göster",
                        )
                    }
                },
            )

            Spacer(Modifier.height(24.dp))

            // ── Connect Button ─────────────────────────────────────────────
            Button(
                onClick  = { viewModel.testAndSave(onSetupComplete) },
                enabled  = uiState.connectionStatus !is ConnectionStatus.Checking,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (uiState.connectionStatus is ConnectionStatus.Checking) {
                    CircularProgressIndicator(
                        modifier  = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color     = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Bağlanıyor...")
                } else {
                    Text("Bağlan ve Kaydet")
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Status Banner ──────────────────────────────────────────────
            AnimatedVisibility(
                visible = uiState.connectionStatus is ConnectionStatus.Failure,
            ) {
                val msg = (uiState.connectionStatus as? ConnectionStatus.Failure)?.message ?: ""
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector        = Icons.Default.Error,
                        contentDescription = null,
                        tint               = RedError,
                        modifier           = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text  = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = RedError,
                    )
                }
            }

            AnimatedVisibility(
                visible = uiState.connectionStatus is ConnectionStatus.Success,
            ) {
                val msg = (uiState.connectionStatus as? ConnectionStatus.Success)?.version ?: ""
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector        = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint               = GreenOk,
                        modifier           = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text  = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = GreenOk,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
