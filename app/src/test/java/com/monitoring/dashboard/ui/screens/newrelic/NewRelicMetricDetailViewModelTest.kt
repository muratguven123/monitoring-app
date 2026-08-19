package com.monitoring.dashboard.ui.screens.newrelic

import androidx.lifecycle.SavedStateHandle
import com.monitoring.dashboard.data.remote.dto.newrelic.MetricDataDto
import com.monitoring.dashboard.data.remote.dto.newrelic.MetricTimeSliceDto
import com.monitoring.dashboard.data.remote.dto.newrelic.TimeSliceDto
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.data.repository.NewRelicRepository
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NewRelicMetricDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var newRelicRepository: NewRelicRepository

    private fun timeslice(vararg values: Pair<String, Double>) = TimeSliceDto(
        from = "2026-01-01T00:00:00+00:00",
        to = "2026-01-01T00:01:00+00:00",
        values = values.toMap(),
    )

    private fun metricData(
        name: String = "HttpDispatcher",
        slices: List<TimeSliceDto> = emptyList(),
    ) = MetricDataDto(
        metrics = listOf(MetricTimeSliceDto(name = name, timeslices = slices)),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        newRelicRepository = mockk()
        coEvery {
            newRelicRepository.getMetricData(any(), any(), any(), any(), any(), any())
        } returns NetworkResult.Success(
            metricData(
                slices = listOf(
                    timeslice("average_response_time" to 120.0),
                    timeslice("average_response_time" to 140.5),
                ),
            ),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        appId: Long = 7L,
        metricName: String = "HttpDispatcher",
        valueKey: String = "average_response_time",
        displayName: String = "Response%20Time",
        unit: String = "ms",
    ) = NewRelicMetricDetailViewModel(
        SavedStateHandle(
            mapOf(
                "appId" to appId,
                "metricName" to metricName,
                "valueKey" to valueKey,
                "displayName" to displayName,
                "unit" to unit,
            ),
        ),
        newRelicRepository,
    )

    @Test
    fun `starts in loading state`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `nav arguments are url decoded`() = runTest(testDispatcher) {
        // Metric display names contain spaces and slashes, so they arrive
        // percent-encoded from the nav route.
        val viewModel = createViewModel(displayName = "Response%20Time")
        advanceUntilIdle()

        assertEquals("Response Time", viewModel.uiState.value.displayName)
        assertEquals("ms", viewModel.uiState.value.unit)
    }

    @Test
    fun `timeslice values are extracted in order for the selected key`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(listOf(120.0f, 140.5f), state.points)
            assertNull(state.errorMessage)
        }

    @Test
    fun `timeslices missing the value key are skipped rather than zeroed`() =
        runTest(testDispatcher) {
            coEvery {
                newRelicRepository.getMetricData(any(), any(), any(), any(), any(), any())
            } returns NetworkResult.Success(
                metricData(
                    slices = listOf(
                        timeslice("average_response_time" to 120.0),
                        timeslice("calls_per_minute" to 5.0), // no response time in this slice
                        timeslice("average_response_time" to 130.0),
                    ),
                ),
            )

            val viewModel = createViewModel()
            advanceUntilIdle()

            // Substituting 0 would draw a misleading dip in the chart.
            assertEquals(listOf(120.0f, 130.0f), viewModel.uiState.value.points)
        }

    @Test
    fun `a metric name that does not match yields no points`() = runTest(testDispatcher) {
        coEvery {
            newRelicRepository.getMetricData(any(), any(), any(), any(), any(), any())
        } returns NetworkResult.Success(metricData(name = "SomethingElse"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.points.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `default time range is three hours`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals("now-3h", viewModel.uiState.value.selectedTimeRange.from)
        coVerify { newRelicRepository.getMetricData(7L, listOf("HttpDispatcher"), "now-3h", any(), any(), any()) }
    }

    @Test
    fun `selecting a time range reloads with the new window`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectTimeRange(NR_TIME_RANGES.first { it.from == "now-7d" })
        advanceUntilIdle()

        assertEquals("now-7d", viewModel.uiState.value.selectedTimeRange.from)
        coVerify { newRelicRepository.getMetricData(7L, any(), "now-7d", any(), any(), any()) }
    }

    @Test
    fun `failure surfaces the message and stops loading`() = runTest(testDispatcher) {
        coEvery {
            newRelicRepository.getMetricData(any(), any(), any(), any(), any(), any())
        } returns NetworkResult.Error(code = 500, message = "Internal error")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Internal error", state.errorMessage)
    }

    @Test
    fun `retry clears the error and reloads`() = runTest(testDispatcher) {
        coEvery {
            newRelicRepository.getMetricData(any(), any(), any(), any(), any(), any())
        } returns NetworkResult.Error(message = "offline")

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals("offline", viewModel.uiState.value.errorMessage)

        coEvery {
            newRelicRepository.getMetricData(any(), any(), any(), any(), any(), any())
        } returns NetworkResult.Success(
            metricData(slices = listOf(timeslice("average_response_time" to 99.0))),
        )

        viewModel.retry()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertEquals(listOf(99.0f), state.points)
    }
}
