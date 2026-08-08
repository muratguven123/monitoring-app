package com.monitoring.dashboard.ui.screens.newrelic

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monitoring.dashboard.data.local.MetricThresholds
import com.monitoring.dashboard.data.local.UserPreferencesRepository
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

/** Tek bir metric'in zaman serisi veri noktaları */
data class MetricChartData(
    val displayName: String,
    val metricName: String,
    val valueKey: String,
    val unit: String,
    val points: List<Float> = emptyList(),
    val isLoading: Boolean = true,
)

data class NewRelicAppDetailUiState(
    val isLoading: Boolean = true,
    val application: NewRelicApplicationDto? = null,
    val metricData: MetricDataDto? = null,
    val violations: List<AlertViolationDto> = emptyList(),
    val thresholds: MetricThresholds = MetricThresholds(),
    val errorMessage: String? = null,
    // Grafik verileri
    val responseTimeChart: MetricChartData = MetricChartData(
        displayName = "Response Time",
        metricName = "HttpDispatcher",
        valueKey = "average_response_time",
        unit = "ms",
    ),
    val throughputChart: MetricChartData = MetricChartData(
        displayName = "Throughput",
        metricName = "HttpDispatcher",
        valueKey = "calls_per_minute",
        unit = "rpm",
    ),
    val errorRateChart: MetricChartData = MetricChartData(
        displayName = "Error Rate",
        metricName = "Errors/all",
        valueKey = "error_rate",
        unit = "%",
    ),
    val apdexChart: MetricChartData = MetricChartData(
        displayName = "Apdex Score",
        metricName = "Apdex",
        valueKey = "score",
        unit = "",
    ),
)

@HiltViewModel
class NewRelicAppDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newRelicRepository: NewRelicRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val appId: Long = checkNotNull(savedStateHandle["appId"])

    private val _uiState = MutableStateFlow(NewRelicAppDetailUiState())
    val uiState: StateFlow<NewRelicAppDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.metricThresholds.collect { thresholds ->
                _uiState.update { it.copy(thresholds = thresholds) }
            }
        }
        loadAppDetail()
    }

    fun loadAppDetail() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            launch { loadApplication() }
            launch { loadMetrics() }
            launch { loadViolations() }
            launch { loadChartData() }
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
            is NetworkResult.Success -> _uiState.update { it.copy(metricData = result.data) }
            else -> {}
        }
    }

    private suspend fun loadViolations() {
        when (val result = newRelicRepository.getAlertViolations(onlyOpen = true)) {
            is NetworkResult.Success -> _uiState.update { it.copy(violations = result.data) }
            else -> {}
        }
    }

    /** Son 3 saatlik zaman serisi verilerini yükler (grafik için). */
    private suspend fun loadChartData() {
        val metricGroups = listOf(
            Triple("HttpDispatcher", listOf("average_response_time", "calls_per_minute"), listOf("average_response_time", "calls_per_minute")),
            Triple("Errors/all",     listOf("error_rate"), listOf("error_rate")),
            Triple("Apdex",          listOf("score"),      listOf("score")),
        )

        metricGroups.forEach { (name, values, _) ->
            val result = newRelicRepository.getMetricData(
                applicationId = appId,
                names          = listOf(name),
                from           = "now-3h",
                summarize      = false,
            )
            if (result is NetworkResult.Success) {
                val metricSlice = result.data.metrics.firstOrNull { it.name == name }
                metricSlice?.let { slice ->
                    values.forEach { vKey ->
                        val points = slice.timeslices.mapNotNull { ts ->
                            ts.values[vKey]?.toFloat()
                        }
                        _uiState.update { state ->
                            when {
                                name == "HttpDispatcher" && vKey == "average_response_time" ->
                                    state.copy(responseTimeChart = state.responseTimeChart.copy(points = points, isLoading = false))
                                name == "HttpDispatcher" && vKey == "calls_per_minute" ->
                                    state.copy(throughputChart = state.throughputChart.copy(points = points, isLoading = false))
                                name == "Errors/all" ->
                                    state.copy(errorRateChart = state.errorRateChart.copy(points = points, isLoading = false))
                                name == "Apdex" ->
                                    state.copy(apdexChart = state.apdexChart.copy(points = points, isLoading = false))
                                else -> state
                            }
                        }
                    }
                } ?: run {
                    // Veri gelmedi — yükleme durumunu kapat
                    _uiState.update { state ->
                        when (name) {
                            "HttpDispatcher" -> state.copy(
                                responseTimeChart = state.responseTimeChart.copy(isLoading = false),
                                throughputChart   = state.throughputChart.copy(isLoading = false),
                            )
                            "Errors/all" -> state.copy(errorRateChart = state.errorRateChart.copy(isLoading = false))
                            "Apdex"      -> state.copy(apdexChart = state.apdexChart.copy(isLoading = false))
                            else -> state
                        }
                    }
                }
            } else {
                _uiState.update { state ->
                    when (name) {
                        "HttpDispatcher" -> state.copy(
                            responseTimeChart = state.responseTimeChart.copy(isLoading = false),
                            throughputChart   = state.throughputChart.copy(isLoading = false),
                        )
                        "Errors/all" -> state.copy(errorRateChart = state.errorRateChart.copy(isLoading = false))
                        "Apdex"      -> state.copy(apdexChart = state.apdexChart.copy(isLoading = false))
                        else -> state
                    }
                }
            }
        }
    }
}
