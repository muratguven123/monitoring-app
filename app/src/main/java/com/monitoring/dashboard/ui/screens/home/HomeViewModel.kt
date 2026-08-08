package com.monitoring.dashboard.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monitoring.dashboard.data.local.SecurePreferencesManager
import com.monitoring.dashboard.data.local.UserPreferencesRepository
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val isConfigured: Boolean = true,
    val grafanaHealth: GrafanaHealthDto? = null,
    val grafanaHealthError: String? = null,
    val grafanaDashboards: List<DashboardSearchHitDto> = emptyList(),
    val newRelicApps: List<NewRelicApplicationDto> = emptyList(),
    val newRelicAppsError: String? = null,
    val openViolations: List<AlertViolationDto> = emptyList(),
    val watchlistDashboards: List<DashboardSearchHitDto> = emptyList(),
    val watchlistApps: List<NewRelicApplicationDto> = emptyList(),
    val secondsUntilRefresh: Int = HomeViewModel.AUTO_REFRESH_INTERVAL_SECONDS,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val grafanaRepository: GrafanaRepository,
    private val newRelicRepository: NewRelicRepository,
    private val securePreferencesManager: SecurePreferencesManager,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(isConfigured = securePreferencesManager.isAnySourceConfigured()),
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null
    private var favoriteDashboardUids: Set<String> = emptySet()
    private var favoriteAppIds: Set<String> = emptySet()

    init {
        viewModelScope.launch {
            combine(
                userPreferencesRepository.favoriteDashboardUids,
                userPreferencesRepository.favoriteAppIds,
            ) { dash, apps -> dash to apps }
                .collect { (dash, apps) ->
                    favoriteDashboardUids = dash
                    favoriteAppIds = apps
                    recomputeWatchlist()
                }
        }
        refresh()
        startAutoRefresh()
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
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

    fun refresh() {
        val configured = securePreferencesManager.isAnySourceConfigured()
        _uiState.update {
            it.copy(
                isConfigured = configured,
                isLoading = configured,
                secondsUntilRefresh = AUTO_REFRESH_INTERVAL_SECONDS,
            )
        }
        if (!configured) return
        startAutoRefresh()
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
        when (val result = grafanaRepository.searchDashboards(type = "dash-db", limit = 20)) {
            is NetworkResult.Success -> {
                _uiState.update { it.copy(grafanaDashboards = result.data.take(5)) }
                recomputeWatchlist()
            }
            is NetworkResult.Error -> {}
            is NetworkResult.Loading -> {}
        }
    }

    private suspend fun loadNewRelicApps() {
        when (val result = newRelicRepository.getApplications()) {
            is NetworkResult.Success -> {
                _uiState.update {
                    it.copy(newRelicApps = result.data, newRelicAppsError = null, isLoading = false)
                }
                recomputeWatchlist()
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

    private fun recomputeWatchlist() {
        _uiState.update { state ->
            val allDash = if (state.grafanaDashboards.isNotEmpty()) state.grafanaDashboards else emptyList()
            // Prefer matching favorites from loaded lists; also keep starred from API if present
            val watchDash = allDash.filter {
                it.uid in favoriteDashboardUids || it.isStarred
            }
            val watchApps = state.newRelicApps.filter { it.id.toString() in favoriteAppIds }
            state.copy(
                watchlistDashboards = watchDash,
                watchlistApps = watchApps,
            )
        }
    }
}
