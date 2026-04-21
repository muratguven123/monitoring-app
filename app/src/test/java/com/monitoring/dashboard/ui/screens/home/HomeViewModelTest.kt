package com.monitoring.dashboard.ui.screens.home

import app.cash.turbine.test
import com.monitoring.dashboard.data.remote.dto.DashboardSearchHitDto
import com.monitoring.dashboard.data.remote.dto.GrafanaHealthDto
import com.monitoring.dashboard.data.remote.dto.newrelic.NewRelicApplicationDto
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.data.repository.GrafanaRepository
import com.monitoring.dashboard.data.repository.NewRelicRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var grafanaRepository: GrafanaRepository
    private lateinit var newRelicRepository: NewRelicRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        grafanaRepository = mockk()
        newRelicRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun stubSuccessResponses() {
        coEvery { grafanaRepository.getHealth() } returns NetworkResult.Success(
            GrafanaHealthDto(commit = "abc", database = "ok", version = "10.0"),
        )
        coEvery { grafanaRepository.searchDashboards(any(), any(), any(), any(), any(), any()) } returns
            NetworkResult.Success(emptyList<DashboardSearchHitDto>())
        coEvery { newRelicRepository.getApplications(any()) } returns
            NetworkResult.Success(emptyList<NewRelicApplicationDto>())
        coEvery { newRelicRepository.getAlertViolations(any()) } returns
            NetworkResult.Success(emptyList())
    }

    @Test
    fun `initial state is Loading`() = runTest(testDispatcher) {
        stubSuccessResponses()

        val viewModel = HomeViewModel(grafanaRepository, newRelicRepository)
        val state = viewModel.uiState.value

        assertTrue("Initial state should be loading", state.isLoading)
    }

    @Test
    fun `successful load transitions to Success state`() = runTest(testDispatcher) {
        stubSuccessResponses()

        val viewModel = HomeViewModel(grafanaRepository, newRelicRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("Should not be loading after data loads", state.isLoading)
        assertEquals("ok", state.grafanaHealth?.database)
    }

    @Test
    fun `auto-refresh countdown counts down from 30 to 0`() = runTest(testDispatcher) {
        stubSuccessResponses()

        val viewModel = HomeViewModel(grafanaRepository, newRelicRepository)
        advanceUntilIdle()

        // After init + first data load, countdown starts at 30
        val initialSeconds = viewModel.uiState.value.secondsUntilRefresh
        assertEquals(HomeViewModel.AUTO_REFRESH_INTERVAL_SECONDS, initialSeconds)

        // Advance 5 seconds
        advanceTimeBy(5_000L)

        val afterFive = viewModel.uiState.value.secondsUntilRefresh
        assertTrue(
            "Countdown should decrease after 5s (was $afterFive)",
            afterFive < initialSeconds,
        )
    }
}
