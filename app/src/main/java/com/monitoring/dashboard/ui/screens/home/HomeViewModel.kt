package com.monitoring.dashboard.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monitoring.dashboard.data.DataRefreshBus
import com.monitoring.dashboard.data.local.SecurePreferencesManager
import com.monitoring.dashboard.data.local.UserPreferencesRepository
import com.monitoring.dashboard.data.remote.dto.newrelic.AlertViolationDto
import com.monitoring.dashboard.data.remote.dto.newrelic.NewRelicApplicationDto
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.data.repository.NewRelicRepository
import com.monitoring.dashboard.domain.model.Dashboard
import com.monitoring.dashboard.domain.model.GrafanaHealth
import com.monitoring.dashboard.domain.usecase.CheckGrafanaHealthUseCase
import com.monitoring.dashboard.domain.usecase.GetDashboardsUseCase
import com.monitoring.dashboard.domain.usecase.GetNewRelicApplicationsUseCase
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
    val isShowingCachedData: Boolean = false,
    val grafanaHealth: GrafanaHealth? = null,
    val grafanaHealthError: String? = null,
    val grafanaDashboards: List<Dashboard> = emptyList(),
    val newRelicApps: List<NewRelicApplicationDto> = emptyList(),
    val newRelicAppsError: String? = null,
    val openViolations: List<AlertViolationDto> = emptyList(),
    val watchlistDashboards: List<Dashboard> = emptyList(),
    val watchlistApps: List<NewRelicApplicationDto> = emptyList(),
    val secondsUntilRefresh: Int = HomeViewModel.AUTO_REFRESH_INTERVAL_SECONDS,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val checkGrafanaHealthUseCase: CheckGrafanaHealthUseCase,
    private val getDashboardsUseCase: GetDashboardsUseCase,
    private val getNewRelicApplicationsUseCase: GetNewRelicApplicationsUseCase,
    private val newRelicRepository: NewRelicRepository,
    private val securePreferencesManager: SecurePreferencesManager,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val dataRefreshBus: DataRefreshBus,
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
        viewModelScope.launch {
            dataRefreshBus.events.collect { refresh() }
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
                isShowingCachedData = false,
                secondsUntilRefresh = AUTO_REFRESH_INTERVAL_SECONDS,
            )
        }
        if (!configured) return
        startAutoRefresh()
        viewModelScope.launch { loadAllData(showLoadingSpinner = true) }
    }

    private suspend fun loadAllData(showLoadingSpinner: Boolean) {
        if (showLoadingSpinner) _uiState.update { it.copy(isLoading = true, isShowingCachedData = false) }
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
        when (val result = checkGrafanaHealthUseCase()) {
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
        when (val result = getDashboardsUseCase(limit = 20)) {
            is NetworkResult.Success -> {
                _uiState.update {
                    it.copy(
                        grafanaDashboards = result.data.take(5),
                        isShowingCachedData = it.isShowingCachedData || result.fromCache,
                    )
                }
                recomputeWatchlist()
            }
            is NetworkResult.Error -> {}
            is NetworkResult.Loading -> {}
        }
    }

    private suspend fun loadNewRelicApps() {
        when (val result = getNewRelicApplicationsUseCase()) {
            is NetworkResult.Success -> {
                _uiState.update {
                    it.copy(
                        newRelicApps = result.data,
                        newRelicAppsError = null,
                        isLoading = false,
                        isShowingCachedData = it.isShowingCachedData || result.fromCache,
                    )
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
                it.copy(
                    openViolations = result.data,
                    isShowingCachedData = it.isShowingCachedData || result.fromCache,
                )
            }
            is NetworkResult.Error -> {}
            is NetworkResult.Loading -> {}
        }
    }

    private fun recomputeWatchlist() {
        _uiState.update { state ->
            val watchDash = state.grafanaDashboards.filter {
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
