package com.monitoring.dashboard.ui.screens.newrelic

import androidx.lifecycle.SavedStateHandle
import com.monitoring.dashboard.data.local.MetricThresholds
import com.monitoring.dashboard.data.local.UserPreferencesRepository
import com.monitoring.dashboard.data.remote.dto.newrelic.MetricDataDto
import com.monitoring.dashboard.data.remote.dto.newrelic.NewRelicApplicationDto
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.data.repository.NewRelicRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NewRelicAppDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var newRelicRepository: NewRelicRepository
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    private val application = NewRelicApplicationDto(id = 7L, name = "checkout")

    /** A well-formed response that simply contains no timeseries yet. */
    private val emptyMetrics = MetricDataDto(metrics = emptyList())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        newRelicRepository = mockk()
        userPreferencesRepository = mockk(relaxed = true)

        every { userPreferencesRepository.metricThresholds } returns flowOf(MetricThresholds())

        coEvery { newRelicRepository.getApplicationById(any()) } returns
            NetworkResult.Success(application)
        coEvery {
            newRelicRepository.getMetricData(any(), any(), any(), any(), any(), any())
        } returns NetworkResult.Success(emptyMetrics)
        coEvery { newRelicRepository.getAlertViolations(any()) } returns
            NetworkResult.Success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(appId: Long = 7L) = NewRelicAppDetailViewModel(
        SavedStateHandle(mapOf("appId" to appId)),
        newRelicRepository,
        userPreferencesRepository,
    )

    @Test
    fun `starts in loading state`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `app id comes from saved state and is used for the lookup`() = runTest(testDispatcher) {
        val viewModel = createViewModel(appId = 99L)
        advanceUntilIdle()

        assertEquals(99L, viewModel.appId)
        coVerify { newRelicRepository.getApplicationById(99L) }
    }

    @Test
    fun `successful load populates the application`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("checkout", state.application?.name)
        assertNull(state.errorMessage)
    }

    @Test
    fun `failure surfaces the message and stops loading`() = runTest(testDispatcher) {
        coEvery { newRelicRepository.getApplicationById(any()) } returns
            NetworkResult.Error(code = 404, message = "Application not found")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Application not found", state.errorMessage)
    }

    @Test
    fun `failure without a message falls back to a readable default`() = runTest(testDispatcher) {
        coEvery { newRelicRepository.getApplicationById(any()) } returns
            NetworkResult.Error(message = null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals("Failed to load application", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `charts stop loading even when no timeseries data comes back`() = runTest(testDispatcher) {
        // Empty metrics are common for a freshly instrumented app. The charts
        // must settle into "no data" instead of spinning forever.
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.responseTimeChart.isLoading)
        assertFalse(state.throughputChart.isLoading)
        assertFalse(state.errorRateChart.isLoading)
        assertFalse(state.apdexChart.isLoading)
        assertTrue(state.responseTimeChart.points.isEmpty())
    }

    @Test
    fun `charts stop loading when the metrics request fails`() = runTest(testDispatcher) {
        coEvery {
            newRelicRepository.getMetricData(any(), any(), any(), any(), any(), any())
        } returns NetworkResult.Error(message = "metrics unavailable")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.responseTimeChart.isLoading)
        assertFalse(state.apdexChart.isLoading)
    }

    @Test
    fun `a metrics failure does not hide the application itself`() = runTest(testDispatcher) {
        coEvery {
            newRelicRepository.getMetricData(any(), any(), any(), any(), any(), any())
        } returns NetworkResult.Error(message = "metrics unavailable")

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Metrics are supplementary; losing them should not blank the screen.
        assertEquals("checkout", viewModel.uiState.value.application?.name)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `threshold preferences flow into state`() = runTest(testDispatcher) {
        val custom = MetricThresholds(apdexGreen = 0.95)
        every { userPreferencesRepository.metricThresholds } returns flowOf(custom)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(0.95, viewModel.uiState.value.thresholds.apdexGreen, 0.0001)
    }

    @Test
    fun `open violations are loaded alongside the application`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        coVerify { newRelicRepository.getAlertViolations(true) }
        assertTrue(viewModel.uiState.value.violations.isEmpty())
    }
}
