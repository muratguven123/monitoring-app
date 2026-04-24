package com.monitoring.dashboard.ui.screens.newrelic

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monitoring.dashboard.data.remote.dto.newrelic.AlertViolationDto
import com.monitoring.dashboard.data.remote.dto.newrelic.MetricDataDto
import com.monitoring.dashboard.data.remote.dto.newrelic.NewRelicApplicationDto
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.data.repository.NewRelicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewRelicAppDetailUiState(
    val isLoading: Boolean = true,
    val application: NewRelicApplicationDto? = null,
    val metricData: MetricDataDto? = null,
    val violations: List<AlertViolationDto> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class NewRelicAppDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newRelicRepository: NewRelicRepository,
) : ViewModel() {

    private val appId: Long = checkNotNull(savedStateHandle["appId"])

    private val _uiState = MutableStateFlow(NewRelicAppDetailUiState())
    val uiState: StateFlow<NewRelicAppDetailUiState> = _uiState.asStateFlow()

    init {
        loadAppDetail()
    }

    fun loadAppDetail() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            launch { loadApplication() }
            launch { loadMetrics() }
            launch { loadViolations() }
        }
    }

    private suspend fun loadApplication() {
        when (val result = newRelicRepository.getApplicationById(appId)) {
            is NetworkResult.Success -> _uiState.update {
                it.copy(isLoading = false, application = result.data, errorMessage = null)
            }
            is NetworkResult.Error -> _uiState.update {
                it.copy(isLoading = false, errorMessage = result.message ?: "Failed to load application")
            }
            is NetworkResult.Loading -> {}
        }
    }

    private suspend fun loadMetrics() {
        val result = newRelicRepository.getMetricData(
            applicationId = appId,
            names = listOf("HttpDispatcher", "Apdex", "EndUser/Apdex", "Errors/all"),
            summarize = true,
        )
        when (result) {
            is NetworkResult.Success -> _uiState.update {
                it.copy(metricData = result.data)
            }
            is NetworkResult.Error -> {}
            is NetworkResult.Loading -> {}
        }
    }

    private suspend fun loadViolations() {
        when (val result = newRelicRepository.getAlertViolations(onlyOpen = true)) {
            is NetworkResult.Success -> _uiState.update {
                it.copy(violations = result.data)
            }
            is NetworkResult.Error -> {}
            is NetworkResult.Loading -> {}
        }
    }
}
