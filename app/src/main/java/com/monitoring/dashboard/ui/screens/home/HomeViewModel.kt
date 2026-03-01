package com.monitoring.dashboard.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monitoring.dashboard.data.remote.dto.DashboardSearchHitDto
import com.monitoring.dashboard.data.remote.dto.GrafanaHealthDto
import com.monitoring.dashboard.data.remote.dto.newrelic.AlertViolationDto
import com.monitoring.dashboard.data.remote.dto.newrelic.NewRelicApplicationDto
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.data.repository.GrafanaRepository
import com.monitoring.dashboard.data.repository.NewRelicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val grafanaHealth: GrafanaHealthDto? = null,
    val grafanaHealthError: String? = null,
    val grafanaDashboards: List<DashboardSearchHitDto> = emptyList(),
    val newRelicApps: List<NewRelicApplicationDto> = emptyList(),
    val newRelicAppsError: String? = null,
    val openViolations: List<AlertViolationDto> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val grafanaRepository: GrafanaRepository,
    private val newRelicRepository: NewRelicRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            launch { loadGrafanaHealth() }
            launch { loadGrafanaDashboards() }
            launch { loadNewRelicApps() }
            launch { loadOpenViolations() }
        }
    }

    private suspend fun loadGrafanaHealth() {
        when (val result = grafanaRepository.getHealth()) {
            is NetworkResult.Success -> _uiState.update {
                it.copy(grafanaHealth = result.data, grafanaHealthError = null, isLoading = false)
            }
            is NetworkResult.Error -> _uiState.update {
                it.copy(grafanaHealthError = result.message ?: "Connection failed", isLoading = false)
            }
            is NetworkResult.Loading -> {}
        }
    }

    private suspend fun loadGrafanaDashboards() {
        when (val result = grafanaRepository.searchDashboards(type = "dash-db", limit = 5)) {
            is NetworkResult.Success -> _uiState.update {
                it.copy(grafanaDashboards = result.data)
            }
            is NetworkResult.Error -> {}
            is NetworkResult.Loading -> {}
        }
    }

    private suspend fun loadNewRelicApps() {
        when (val result = newRelicRepository.getApplications()) {
            is NetworkResult.Success -> _uiState.update {
                it.copy(newRelicApps = result.data, newRelicAppsError = null, isLoading = false)
            }
            is NetworkResult.Error -> _uiState.update {
                it.copy(newRelicAppsError = result.message ?: "Connection failed", isLoading = false)
            }
            is NetworkResult.Loading -> {}
        }
    }

    private suspend fun loadOpenViolations() {
        when (val result = newRelicRepository.getAlertViolations(onlyOpen = true)) {
            is NetworkResult.Success -> _uiState.update {
                it.copy(openViolations = result.data)
            }
            is NetworkResult.Error -> {}
            is NetworkResult.Loading -> {}
        }
    }
}
