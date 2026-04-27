package com.monitoring.dashboard.ui.screens.newrelic

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.data.repository.NewRelicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

data class NrTimeRange(val label: String, val from: String)

val NR_TIME_RANGES = listOf(
    NrTimeRange("30 dk",  "now-30m"),
    NrTimeRange("1 saat", "now-1h"),
    NrTimeRange("3 saat", "now-3h"),
    NrTimeRange("6 saat", "now-6h"),
    NrTimeRange("12 saat","now-12h"),
    NrTimeRange("1 gün",  "now-1d"),
    NrTimeRange("3 gün",  "now-3d"),
    NrTimeRange("7 gün",  "now-7d"),
)

data class NewRelicMetricDetailUiState(
    val displayName: String  = "",
    val unit: String         = "",
    val points: List<Float>  = emptyList(),
    val isLoading: Boolean   = true,
    val errorMessage: String? = null,
    val selectedTimeRange: NrTimeRange = NR_TIME_RANGES[2], // 3 saat varsayılan
)

@HiltViewModel
class NewRelicMetricDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newRelicRepository: NewRelicRepository,
) : ViewModel() {

    private val appId: Long       = checkNotNull(savedStateHandle["appId"])
    private val metricName: String = URLDecoder.decode(checkNotNull(savedStateHandle["metricName"]), "UTF-8")
    private val valueKey: String   = URLDecoder.decode(savedStateHandle["valueKey"] ?: "", "UTF-8")
    private val displayName: String = URLDecoder.decode(savedStateHandle["displayName"] ?: metricName, "UTF-8")
    private val unit: String        = URLDecoder.decode(savedStateHandle["unit"] ?: "", "UTF-8")

    private val _uiState = MutableStateFlow(
        NewRelicMetricDetailUiState(displayName = displayName, unit = unit)
    )
    val uiState: StateFlow<NewRelicMetricDetailUiState> = _uiState.asStateFlow()

    init {
        loadMetric(_uiState.value.selectedTimeRange)
    }

    fun selectTimeRange(range: NrTimeRange) {
        _uiState.update { it.copy(selectedTimeRange = range) }
        loadMetric(range)
    }

    fun retry() = loadMetric(_uiState.value.selectedTimeRange)

    private fun loadMetric(range: NrTimeRange) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = newRelicRepository.getMetricData(
                applicationId = appId,
                names         = listOf(metricName),
                from          = range.from,
                summarize     = false,
            )
            when (result) {
                is NetworkResult.Success -> {
                    val slice = result.data.metrics.firstOrNull { it.name == metricName }
                    val points = slice?.timeslices?.mapNotNull { ts ->
                        ts.values[valueKey]?.toFloat()
                    } ?: emptyList()
                    _uiState.update { it.copy(points = points, isLoading = false) }
                }
                is NetworkResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message ?: "Veri yüklenemedi")
                }
                else -> {}
            }
        }
    }
}
