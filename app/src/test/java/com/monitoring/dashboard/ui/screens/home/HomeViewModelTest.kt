package com.monitoring.dashboard.ui.screens.home

import com.monitoring.dashboard.data.DataRefreshBus
import com.monitoring.dashboard.data.local.SecurePreferencesManager
import com.monitoring.dashboard.data.local.UserPreferencesRepository
import com.monitoring.dashboard.data.remote.dto.newrelic.NewRelicApplicationDto
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.data.repository.NewRelicRepository
import com.monitoring.dashboard.domain.model.GrafanaHealth
import com.monitoring.dashboard.domain.usecase.CheckGrafanaHealthUseCase
import com.monitoring.dashboard.domain.usecase.GetDashboardsUseCase
import com.monitoring.dashboard.domain.usecase.GetNewRelicApplicationsUseCase
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
    private lateinit var checkGrafanaHealthUseCase: CheckGrafanaHealthUseCase
    private lateinit var getDashboardsUseCase: GetDashboardsUseCase
    private lateinit var getNewRelicApplicationsUseCase: GetNewRelicApplicationsUseCase
    private lateinit var newRelicRepository: NewRelicRepository
    private lateinit var securePreferencesManager: SecurePreferencesManager
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var dataRefreshBus: DataRefreshBus

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        checkGrafanaHealthUseCase = mockk()
        getDashboardsUseCase = mockk()
        getNewRelicApplicationsUseCase = mockk()
        newRelicRepository = mockk()
        securePreferencesManager = mockk(relaxed = true)
        userPreferencesRepository = mockk(relaxed = true)
        dataRefreshBus = DataRefreshBus()
        every { securePreferencesManager.isAnySourceConfigured() } returns true
        every { securePreferencesManager.needsCredentialReset() } returns false
        every { userPreferencesRepository.favoriteDashboardUids } returns flowOf(emptySet())
        every { userPreferencesRepository.favoriteAppIds } returns flowOf(emptySet())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun stubSuccessResponses() {
        coEvery { checkGrafanaHealthUseCase() } returns NetworkResult.Success(
            GrafanaHealth(version = "10.0", database = "ok", isHealthy = true),
        )
        coEvery { getDashboardsUseCase(any(), any(), any(), any()) } returns
            NetworkResult.Success(emptyList())
        coEvery { getNewRelicApplicationsUseCase(any()) } returns
            NetworkResult.Success(emptyList<NewRelicApplicationDto>())
        coEvery { newRelicRepository.getAlertViolations(any()) } returns
            NetworkResult.Success(emptyList())
    }

    private fun createViewModel() = HomeViewModel(
        checkGrafanaHealthUseCase,
        getDashboardsUseCase,
        getNewRelicApplicationsUseCase,
        newRelicRepository,
        securePreferencesManager,
        userPreferencesRepository,
        dataRefreshBus,
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
        assertTrue(state.grafanaHealth?.isHealthy == true)
    }

    @Test
    fun `unconfigured shows setup state`() = runTest(testDispatcher) {
        every { securePreferencesManager.isAnySourceConfigured() } returns false
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isConfigured)
    }

    @Test
    fun `cached success sets isShowingCachedData`() = runTest(testDispatcher) {
        coEvery { checkGrafanaHealthUseCase() } returns NetworkResult.Success(
            GrafanaHealth(version = "10.0", database = "ok", isHealthy = true),
        )
        coEvery { getDashboardsUseCase(any(), any(), any(), any()) } returns
            NetworkResult.Success(emptyList(), fromCache = true)
        coEvery { getNewRelicApplicationsUseCase(any()) } returns
            NetworkResult.Success(emptyList())
        coEvery { newRelicRepository.getAlertViolations(any()) } returns
            NetworkResult.Success(emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isShowingCachedData)
    }
}
