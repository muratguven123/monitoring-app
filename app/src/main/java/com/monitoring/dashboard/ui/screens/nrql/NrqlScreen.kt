package com.monitoring.dashboard.ui.screens.nrql

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.monitoring.dashboard.R
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.data.repository.NerdGraphRepository
import com.monitoring.dashboard.ui.components.LoadingIndicator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private val NRQL_TEMPLATES = listOf(
    "SELECT average(duration) FROM Transaction SINCE 1 hour ago",
    "SELECT percentage(count(*), WHERE error IS true) FROM Transaction SINCE 1 hour ago",
    "SELECT apdex(duration) FROM Transaction SINCE 1 hour ago",
)

data class NrqlUiState(
    val query: String = NRQL_TEMPLATES[0],
    val isLoading: Boolean = false,
    val rows: List<String> = emptyList(),
    val rawJson: String? = null,
    val error: String? = null,
    val missingAccountId: Boolean = false,
)

@HiltViewModel
class NrqlViewModel @Inject constructor(
    private val nerdGraphRepository: NerdGraphRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        NrqlUiState(missingAccountId = !nerdGraphRepository.hasAccountId()),
    )
    val uiState = _uiState.asStateFlow()

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun applyTemplate(template: String) {
        _uiState.update { it.copy(query = template) }
    }

    fun run() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    rows = emptyList(),
                    rawJson = null,
                    missingAccountId = !nerdGraphRepository.hasAccountId(),
                )
            }
            if (!nerdGraphRepository.hasAccountId()) {
                _uiState.update { it.copy(isLoading = false, missingAccountId = true) }
                return@launch
            }
            when (val result = nerdGraphRepository.runNrql(_uiState.value.query)) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        rows = result.data.rows,
                        rawJson = result.data.rawJson,
                    )
                }
                is NetworkResult.Error -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.message,
                        missingAccountId = result.message == "MISSING_ACCOUNT_ID",
                    )
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NrqlScreen(
    onBackClick: () -> Unit,
    onOpenSettings: () -> Unit = {},
    viewModel: NrqlViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_nrql_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (uiState.missingAccountId) {
                item {
                    Text(
                        text = stringResource(R.string.nrql_missing_account),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                item {
                    OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.action_open_settings))
                    }
                }
            }
            item {
                Text(
                    text = stringResource(R.string.nrql_templates),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            items(NRQL_TEMPLATES) { template ->
                FilterChip(
                    selected = uiState.query == template,
                    onClick = { viewModel.applyTemplate(template) },
                    label = { Text(template.take(48) + if (template.length > 48) "…" else "") },
                )
            }
            item {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChanged,
                    label = { Text(stringResource(R.string.nrql_query_label)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
            }
            item {
                Button(
                    onClick = viewModel::run,
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(stringResource(R.string.action_run_nrql))
                }
            }
            if (uiState.isLoading) {
                item { LoadingIndicator() }
            }
            uiState.error?.let { error ->
                item {
                    Text(text = error, color = MaterialTheme.colorScheme.error)
                }
            }
            if (uiState.rows.isNotEmpty()) {
                items(uiState.rows) { row ->
                    Text(text = row, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                uiState.rawJson?.let { raw ->
                    item {
                        Text(text = raw, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
