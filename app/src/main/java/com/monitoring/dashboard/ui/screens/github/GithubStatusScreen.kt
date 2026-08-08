package com.monitoring.dashboard.ui.screens.github

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.monitoring.dashboard.data.local.SecurePreferencesManager
import com.monitoring.dashboard.data.remote.GitHubWorkflowRunDto
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.data.repository.GitHubRepository
import com.monitoring.dashboard.ui.components.EmptyState
import com.monitoring.dashboard.ui.components.ErrorMessage
import com.monitoring.dashboard.ui.components.LoadingIndicator
import com.monitoring.dashboard.ui.components.MonitoringCard
import com.monitoring.dashboard.ui.theme.StatusCritical
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GithubUiState(
    val isLoading: Boolean = true,
    val runs: List<GitHubWorkflowRunDto> = emptyList(),
    val error: String? = null,
    val missingConfig: Boolean = false,
)

@HiltViewModel
class GithubStatusViewModel @Inject constructor(
    private val gitHubRepository: GitHubRepository,
    private val securePreferencesManager: SecurePreferencesManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(GithubUiState())
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    private fun isConfigured(): Boolean {
        val token = securePreferencesManager.getGithubToken()
        val repo = securePreferencesManager.getGithubRepo()
        return !token.isNullOrBlank() && !repo.isNullOrBlank() && repo.contains("/")
    }

    fun load() {
        viewModelScope.launch {
            if (!isConfigured()) {
                _uiState.update {
                    it.copy(isLoading = false, missingConfig = true, error = null, runs = emptyList())
                }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, error = null, missingConfig = false) }
            when (val result = gitHubRepository.getRecentWorkflowRuns()) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(isLoading = false, runs = result.data)
                }
                is NetworkResult.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GithubStatusScreen(
    onBackClick: () -> Unit,
    onOpenSettings: () -> Unit = {},
    viewModel: GithubStatusViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_github_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        when {
            uiState.missingConfig -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.github_missing_config),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    item {
                        OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.action_open_settings))
                        }
                    }
                }
            }
            uiState.isLoading -> LoadingIndicator(modifier = Modifier.padding(padding))
            uiState.error != null -> ErrorMessage(
                message = uiState.error ?: stringResource(R.string.error_unknown),
                onRetry = viewModel::load,
                modifier = Modifier.padding(padding),
            )
            uiState.runs.isEmpty() -> EmptyState(
                message = stringResource(R.string.github_empty),
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.runs, key = { it.id }) { run ->
                    val failed = run.conclusion.equals("failure", true) ||
                        run.conclusion.equals("cancelled", true)
                    MonitoringCard(
                        title = run.name ?: "Workflow",
                        subtitle = listOfNotNull(
                            run.headBranch,
                            run.status,
                            run.conclusion,
                        ).joinToString(" · "),
                        icon = Icons.Default.Cloud,
                        iconTint = if (failed) StatusCritical else MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
