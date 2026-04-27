package com.monitoring.dashboard.ui.screens.grafana

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.monitoring.dashboard.data.local.SecurePreferencesManager
import com.monitoring.dashboard.data.remote.dto.DashboardDetailResponseDto
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.data.repository.GrafanaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GrafanaDashboardDetailUiState(
    val isLoading: Boolean = true,
    val dashboard: DashboardDetailResponseDto? = null,
    val errorMessage: String? = null,
    /** Grafana base URL (e.g. "https://host.grafana.net") — used to build panel render URLs. */
    val grafanaBaseUrl: String = "",
)

@HiltViewModel
class GrafanaDashboardDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val grafanaRepository: GrafanaRepository,
    private val securePreferencesManager: SecurePreferencesManager,
    /** Auth-enabled ImageLoader — used by the screen to load Grafana panel renders. */
    val imageLoader: ImageLoader,
) : ViewModel() {

    private val uid: String = checkNotNull(savedStateHandle["uid"])

    private val _uiState = MutableStateFlow(GrafanaDashboardDetailUiState())
    val uiState: StateFlow<GrafanaDashboardDetailUiState> = _uiState.asStateFlow()

    init {
        // Load base URL once so the screen can build panel render URLs
        val rawBase = securePreferencesManager.getGrafanaBaseUrl() ?: ""
        val baseUrl = rawBase.trimEnd('/')
        _uiState.update { it.copy(grafanaBaseUrl = baseUrl) }
        loadDashboard()
    }

    fun loadDashboard() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = grafanaRepository.getDashboardByUid(uid)) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(isLoading = false, dashboard = result.data, errorMessage = null)
                }
                is NetworkResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message ?: "Failed to load dashboard")
                }
                is NetworkResult.Loading -> {}
            }
        }
    }
}
