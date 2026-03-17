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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val grafanaHealth: GrafanaHealthDto? = null,
    val grafanaHealthError: String? = null,
    val grafanaDashboards: List<DashboardSearchHitDto> = emptyList(),
    val newRelicApps: List<NewRelicApplicationDto> = emptyList(),
    val newRelicAppsError: String? = null,
    val openViolations: List<AlertViolationDto> = emptyList(),
    val secondsUntilRefresh: Int = HomeViewModel.AUTO_REFRESH_INTERVAL_SECONDS,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val grafanaRepository: GrafanaRepository,
    private val newRelicRepository: NewRelicRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Holds the auto-refresh loop job so it can be cancelled / restarted. */
    private var autoRefreshJob: Job? = null

    init {
        refresh()
        startAutoRefresh()
    }

    // ── Auto-refresh ────────────────────────────────────────────────────────

    /**
     * Starts a ticker that refreshes data every [AUTO_REFRESH_INTERVAL_SECONDS] seconds
     * and exposes a visible countdown in [HomeUiState.secondsUntilRefresh].
     */
    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                // Count down to zero then refresh
                for (remaining in AUTO_REFRESH_INTERVAL_SECONDS downTo 1) {
                    _uiState.update { it.copy(secondsUntilRefresh = remaining) }
                    delay(1_000L)
                    if (!isActive) return@launch
                }
                Timber.d("HomeViewModel: auto-refresh triggered")
                loadAllData(showLoadingSpinner = false)
                _uiState.update { it.copy(secondsUntilRefresh = AUTO_REFRESH_INTERVAL_SECONDS) }
            }
        }
    }

    /** Manual refresh – resets the countdown and immediately reloads. */
    fun refresh() {
        _uiState.update { it.copy(isLoading = true, secondsUntilRefresh = AUTO_REFRESH_INTERVAL_SECONDS) }
        startAutoRefresh()           // restart the countdown
        viewModelScope.launch { loadAllData(showLoadingSpinner = true) }
    }

    private suspend fun loadAllData(showLoadingSpinner: Boolean) {
        if (showLoadingSpinner) _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            launch { loadGrafanaHealth() }
            launch { loadGrafanaDashboards() }
            launch { loadNewRelicApps() }
            launch { loadOpenViolations() }
        }
    }

    companion object {
        /** Seconds between automatic background refreshes on the Home screen. */
        const val AUTO_REFRESH_INTERVAL_SECONDS = 30
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
