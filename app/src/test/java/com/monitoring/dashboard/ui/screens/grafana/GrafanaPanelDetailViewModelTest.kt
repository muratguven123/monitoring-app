package com.monitoring.dashboard.ui.screens.grafana

import androidx.lifecycle.SavedStateHandle
import coil.ImageLoader
import com.monitoring.dashboard.data.remote.GrafanaBaseUrlProvider
import com.monitoring.dashboard.domain.model.GrafanaServerUrl
import com.monitoring.dashboard.domain.model.GrafanaUrlResult
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The panel screen renders a server-side PNG, so everything here hinges on
 * building a correct `/render/d-solo/...` URL.
 */
class GrafanaPanelDetailViewModelTest {

    private lateinit var grafanaBaseUrlProvider: GrafanaBaseUrlProvider
    private lateinit var imageLoader: ImageLoader

    @Before
    fun setup() {
        grafanaBaseUrlProvider = mockk()
        imageLoader = mockk(relaxed = true)
        configureServer("https://grafana.example.com")
    }

    private fun configureServer(raw: String?) {
        val server = raw?.let {
            (GrafanaServerUrl.parse(it) as GrafanaUrlResult.Valid).url
        }
        every { grafanaBaseUrlProvider.current() } returns server
    }

    private fun createViewModel(
        uid: String = "abc123",
        panelId: Long = 4L,
        slug: String = "cpu-overview",
        panelTitle: String = "CPU%20Usage",
    ) = GrafanaPanelDetailViewModel(
        SavedStateHandle(
            mapOf(
                "uid" to uid,
                "panelId" to panelId,
                "slug" to slug,
                "panelTitle" to panelTitle,
            ),
        ),
        grafanaBaseUrlProvider,
        imageLoader,
    )

    @Test
    fun `panel title is url decoded`() {
        assertEquals("CPU Usage", createViewModel().uiState.value.panelTitle)
    }

    @Test
    fun `render url is built from base url uid slug and panel id`() {
        val url = createViewModel().uiState.value.renderUrl

        assertTrue(url.startsWith("https://grafana.example.com/render/d-solo/abc123/cpu-overview"))
        assertTrue(url.contains("panelId=4"))
    }

    @Test
    fun `trailing slash on the base url does not produce a double slash`() {
        val url = createViewModel().uiState.value.renderUrl

        assertTrue(url.contains("https://grafana.example.com/render/"))
        assertTrue(!url.contains("com//render"))
    }

    @Test
    fun `default time range is three hours`() {
        val state = createViewModel().uiState.value

        assertEquals("now-3h", state.selectedTimeRange.from)
        assertTrue(state.renderUrl.contains("from=now-3h"))
        assertTrue(state.renderUrl.contains("to=now"))
    }

    @Test
    fun `selecting a time range rebuilds the render url`() {
        val viewModel = createViewModel()

        viewModel.selectTimeRange(TIME_RANGES.first { it.from == "now-7d" })

        val state = viewModel.uiState.value
        assertEquals("now-7d", state.selectedTimeRange.from)
        assertTrue(state.renderUrl.contains("from=now-7d"))
    }

    @Test
    fun `missing slug falls back to a slugified panel title`() {
        val url = createViewModel(slug = "", panelTitle = "CPU%20Usage").uiState.value.renderUrl

        assertTrue(url.contains("/abc123/cpu-usage"))
    }

    @Test
    fun `no render url is built when the grafana base url is not configured`() {
        configureServer(null)

        // An empty URL lets the screen show a placeholder instead of firing an
        // image request at a nonsensical address.
        assertEquals("", createViewModel().uiState.value.renderUrl)
    }

    @Test
    fun `reverse proxy sub path is kept in the render url`() {
        // The render endpoint sits under the same prefix as the API; dropping it
        // would request the image from the proxy root.
        configureServer("https://intranet.example.com/grafana")

        val url = createViewModel().uiState.value.renderUrl

        assertTrue(url.startsWith("https://intranet.example.com/grafana/render/d-solo/abc123/"))
    }

    @Test
    fun `a scheme-less stored address is normalised before building the url`() {
        configureServer("grafana.example.com")

        assertTrue(
            createViewModel().uiState.value.renderUrl
                .startsWith("https://grafana.example.com/render/"),
        )
    }
}
