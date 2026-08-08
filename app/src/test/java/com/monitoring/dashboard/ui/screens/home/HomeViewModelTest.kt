package com.monitoring.dashboard.ui.screens.home

import com.monitoring.dashboard.data.local.SecurePreferencesManager
import com.monitoring.dashboard.data.local.UserPreferencesRepository
import com.monitoring.dashboard.data.remote.dto.DashboardSearchHitDto
import com.monitoring.dashboard.data.remote.dto.GrafanaHealthDto
import com.monitoring.dashboard.data.remote.dto.newrelic.NewRelicApplicationDto
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.data.repository.GrafanaRepository
import com.monitoring.dashboard.data.repository.NewRelicRepository
import io.mockk.coEvery
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var grafanaRepository: GrafanaRepository
    private lateinit var newRelicRepository: NewRelicRepository
    private lateinit var securePreferencesManager: SecurePreferencesManager
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        grafanaRepository = mockk()
        newRelicRepository = mockk()
        securePreferencesManager = mockk(relaxed = true)
        userPreferencesRepository = mockk(relaxed = true)
        every { securePreferencesManager.isAnySourceConfigured() } returns true
        every { userPreferencesRepository.favoriteDashboardUids } returns flowOf(emptySet())
        every { userPreferencesRepository.favoriteAppIds } returns flowOf(emptySet())
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

    private fun createViewModel() = HomeViewModel(
        grafanaRepository,
        newRelicRepository,
        securePreferencesManager,
        userPreferencesRepository,
    )

    @Test
    fun `initial state is Loading`() = runTest(testDispatcher) {
        stubSuccessResponses()
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `successful load transitions to Success state`() = runTest(testDispatcher) {
        stubSuccessResponses()
        val viewModel = createViewModel()
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("ok", state.grafanaHealth?.database)
    }

    @Test
    fun `unconfigured shows setup state`() = runTest(testDispatcher) {
        every { securePreferencesManager.isAnySourceConfigured() } returns false
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isConfigured)
    }
}
