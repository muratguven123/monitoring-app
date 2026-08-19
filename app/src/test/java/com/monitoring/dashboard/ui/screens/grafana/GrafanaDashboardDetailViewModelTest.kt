package com.monitoring.dashboard.ui.screens.grafana

import androidx.lifecycle.SavedStateHandle
import coil.ImageLoader
import com.monitoring.dashboard.data.remote.GrafanaBaseUrlProvider
import com.monitoring.dashboard.data.remote.dto.DashboardDetailResponseDto
import com.monitoring.dashboard.data.remote.dto.DashboardDto
import com.monitoring.dashboard.data.remote.dto.DashboardMetaDto
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.data.repository.GrafanaRepository
import com.monitoring.dashboard.domain.model.GrafanaServerUrl
import com.monitoring.dashboard.domain.model.GrafanaUrlResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GrafanaDashboardDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var grafanaRepository: GrafanaRepository
    private lateinit var grafanaBaseUrlProvider: GrafanaBaseUrlProvider
    private lateinit var imageLoader: ImageLoader

    private val detail = DashboardDetailResponseDto(
        meta = DashboardMetaDto(slug = "cpu-overview", url = "/d/abc123/cpu-overview"),
        dashboard = DashboardDto(uid = "abc123", title = "CPU Overview"),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        grafanaRepository = mockk()
        grafanaBaseUrlProvider = mockk()
        imageLoader = mockk(relaxed = true)

        configureServer("https://grafana.example.com")
        coEvery { grafanaRepository.getDashboardByUid(any()) } returns NetworkResult.Success(detail)
    }

    private fun configureServer(raw: String?) {
        val server = raw?.let {
            (GrafanaServerUrl.parse(it) as GrafanaUrlResult.Valid).url
        }
        every { grafanaBaseUrlProvider.current() } returns server
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(uid: String = "abc123") = GrafanaDashboardDetailViewModel(
        SavedStateHandle(mapOf("uid" to uid)),
        grafanaRepository,
        grafanaBaseUrlProvider,
        imageLoader,
    )

    @Test
    fun `starts in loading state`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `loads the dashboard for the uid from saved state`() = runTest(testDispatcher) {
        val viewModel = createViewModel(uid = "cpu-overview")
        advanceUntilIdle()

        coVerify { grafanaRepository.getDashboardByUid("cpu-overview") }
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.dashboard)
        assertNull(state.errorMessage)
    }

    @Test
    fun `base url trailing slash is stripped for panel render urls`() = runTest(testDispatcher) {
        // Panel URLs are built by concatenation, so a trailing slash would
        // produce "https://host//render/..." and break image loading.
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals("https://grafana.example.com", viewModel.uiState.value.grafanaBaseUrl)
    }

    @Test
    fun `missing base url yields an empty string rather than null`() = runTest(testDispatcher) {
        configureServer(null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.grafanaBaseUrl)
    }

    @Test
    fun `reverse proxy sub path is kept in the panel render base url`() =
        runTest(testDispatcher) {
            configureServer("https://intranet.example.com/grafana")

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(
                "https://intranet.example.com/grafana",
                viewModel.uiState.value.grafanaBaseUrl,
            )
        }

    @Test
    fun `failure surfaces the message and stops loading`() = runTest(testDispatcher) {
        coEvery { grafanaRepository.getDashboardByUid(any()) } returns
            NetworkResult.Error(code = 404, message = "Dashboard not found")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Dashboard not found", state.errorMessage)
        assertNull(state.dashboard)
    }

    @Test
    fun `failure without a message falls back to a readable default`() = runTest(testDispatcher) {
        coEvery { grafanaRepository.getDashboardByUid(any()) } returns
            NetworkResult.Error(message = null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals("Failed to load dashboard", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `a successful retry clears the previous error`() = runTest(testDispatcher) {
        coEvery { grafanaRepository.getDashboardByUid(any()) } returns
            NetworkResult.Error(message = "offline")

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals("offline", viewModel.uiState.value.errorMessage)

        coEvery { grafanaRepository.getDashboardByUid(any()) } returns NetworkResult.Success(detail)

        viewModel.loadDashboard()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertNotNull(state.dashboard)
    }
}
