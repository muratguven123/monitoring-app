package com.monitoring.dashboard.ui.screens.grafana

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monitoring.dashboard.data.remote.dto.DashboardSearchHitDto
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.data.repository.GrafanaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GrafanaDashboardsUiState(
    val isLoading: Boolean = true,
    val dashboards: List<DashboardSearchHitDto> = emptyList(),
    val errorMessage: String? = null,
    val searchQuery: String = "",
)

@HiltViewModel
class GrafanaDashboardsViewModel @Inject constructor(
    private val grafanaRepository: GrafanaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GrafanaDashboardsUiState())
    val uiState: StateFlow<GrafanaDashboardsUiState> = _uiState.asStateFlow()

    init {
        loadDashboards()
    }

    fun loadDashboards() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val query = _uiState.value.searchQuery.ifBlank { null }
            when (val result = grafanaRepository.searchDashboards(query = query, type = "dash-db")) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(isLoading = false, dashboards = result.data, errorMessage = null)
                }
                is NetworkResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message ?: "Failed to load dashboards")
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadDashboards()
    }
}
