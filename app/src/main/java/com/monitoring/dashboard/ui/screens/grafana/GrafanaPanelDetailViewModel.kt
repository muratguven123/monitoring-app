package com.monitoring.dashboard.ui.screens.grafana

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import coil.ImageLoader
import com.monitoring.dashboard.data.remote.GrafanaBaseUrlProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.net.URLDecoder
import javax.inject.Inject

data class TimeRange(val label: String, val from: String, val to: String = "now")

val TIME_RANGES = listOf(
    TimeRange("30 dk",  "now-30m"),
    TimeRange("1 saat", "now-1h"),
    TimeRange("3 saat", "now-3h"),
    TimeRange("6 saat", "now-6h"),
    TimeRange("12 saat","now-12h"),
    TimeRange("1 gün",  "now-1d"),
    TimeRange("3 gün",  "now-3d"),
    TimeRange("7 gün",  "now-7d"),
)

data class GrafanaPanelDetailUiState(
    val panelTitle: String = "",
    val renderUrl: String = "",
    val selectedTimeRange: TimeRange = TIME_RANGES[2], // 3 saat varsayılan
)

@HiltViewModel
class GrafanaPanelDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    grafanaBaseUrlProvider: GrafanaBaseUrlProvider,
    val imageLoader: ImageLoader,
) : ViewModel() {

    private val uid: String      = checkNotNull(savedStateHandle["uid"])
    private val panelId: Long    = checkNotNull(savedStateHandle["panelId"])
    private val slug: String     = URLDecoder.decode(savedStateHandle["slug"] ?: "", "UTF-8")
    private val panelTitle: String = URLDecoder.decode(savedStateHandle["panelTitle"] ?: "", "UTF-8")

    // Canonical origin + reverse-proxy prefix, no trailing slash (the render path
    // is concatenated onto it). Empty when no server is configured, which the
    // screen renders as a placeholder instead of firing a doomed image request.
    private val baseUrl: String =
        grafanaBaseUrlProvider.current()?.baseUrl?.trimEnd('/') ?: ""

    private val _uiState = MutableStateFlow(
        GrafanaPanelDetailUiState(panelTitle = panelTitle)
    )
    val uiState: StateFlow<GrafanaPanelDetailUiState> = _uiState.asStateFlow()

    init {
        updateRenderUrl(_uiState.value.selectedTimeRange)
    }

    fun selectTimeRange(range: TimeRange) {
        _uiState.update { it.copy(selectedTimeRange = range) }
        updateRenderUrl(range)
    }

    private fun updateRenderUrl(range: TimeRange) {
        if (baseUrl.isBlank() || uid.isBlank()) return
        val effectiveSlug = slug.ifBlank { panelTitle.lowercase().replace(" ", "-") }
        val url = "$baseUrl/render/d-solo/$uid/$effectiveSlug" +
            "?orgId=1" +
            "&panelId=$panelId" +
            "&width=800" +
            "&height=500" +
            "&from=${range.from}" +
            "&to=${range.to}" +
            "&theme=dark"
        _uiState.update { it.copy(renderUrl = url) }
    }
}
