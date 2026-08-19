package com.monitoring.dashboard.ui.screens.home

import androidx.lifecycle.viewModelScope
import com.monitoring.dashboard.data.DataRefreshBus
import com.monitoring.dashboard.data.local.SecurePreferencesManager
import com.monitoring.dashboard.data.local.UserPreferencesRepository
import com.monitoring.dashboard.data.remote.dto.newrelic.NewRelicApplicationDto
import com.monitoring.dashboard.data.remote.GrafanaBaseUrlProvider
import com.monitoring.dashboard.data.remote.GrafanaNotConfiguredException
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * [HomeViewModel] starts a countdown in `init` that never ends:
 * `while (isActive) { ...; delay(1_000) }`, launched on `viewModelScope`.
 *
 * That makes this class hostile to `runTest`, in two separate ways, and both had
 * to be handled or the whole suite hangs instead of failing:
 *
 *  1. `advanceUntilIdle()` advances virtual time until the scheduler runs dry.
 *     Against an unbounded delay loop it never returns. Hence [runCurrent], which
 *     executes what is scheduled at the current instant and stops — the mocked
 *     loads have no delay so they finish, and the countdown parks at its first
 *     `delay`.
 *
 *  2. `runTest` *also* drains the scheduler after the test body returns, to let
 *     stragglers finish. `viewModelScope` is not a child of the test scope, so it
 *     is never cancelled automatically and that drain spins on the countdown
 *     forever. Every test therefore cancels the scope in a `finally`.
 *
 * The explicit [TEST_TIMEOUT] is a backstop: if a future change reintroduces an
 * unbounded coroutine, the test fails in seconds instead of pinning a CPU until
 * CI's job timeout.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var checkGrafanaHealthUseCase: CheckGrafanaHealthUseCase
    private lateinit var getDashboardsUseCase: GetDashboardsUseCase
    private lateinit var getNewRelicApplicationsUseCase: GetNewRelicApplicationsUseCase
    private lateinit var newRelicRepository: NewRelicRepository
    private lateinit var grafanaBaseUrlProvider: GrafanaBaseUrlProvider
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
        grafanaBaseUrlProvider = mockk()
        every { grafanaBaseUrlProvider.isConfigured() } returns true
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
        grafanaBaseUrlProvider,
        securePreferencesManager,
        userPreferencesRepository,
        dataRefreshBus,
    )

    /**
     * Creates the ViewModel and guarantees its scope is cancelled afterwards,
     * including when an assertion fails — otherwise a failing test would hang
     * rather than report the failure.
     */
    private fun withViewModel(block: (HomeViewModel) -> Unit) {
        val viewModel = createViewModel()
        try {
            block(viewModel)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    private companion object {
        val TEST_TIMEOUT = 15.seconds
    }

    @Test
    fun `initial state is Loading`() = runTest(testDispatcher, timeout = TEST_TIMEOUT) {
        stubSuccessResponses()
        withViewModel { viewModel ->
            assertTrue(viewModel.uiState.value.isLoading)
        }
    }

    @Test
    fun `successful load transitions to Success state`() =
        runTest(testDispatcher, timeout = TEST_TIMEOUT) {
            stubSuccessResponses()
            withViewModel { viewModel ->
                runCurrent()
                val state = viewModel.uiState.value
                assertFalse(state.isLoading)
                assertEquals("ok", state.grafanaHealth?.database)
                assertTrue(state.grafanaHealth?.isHealthy == true)
            }
        }

    @Test
    fun `unconfigured shows setup state`() = runTest(testDispatcher, timeout = TEST_TIMEOUT) {
        every { securePreferencesManager.isAnySourceConfigured() } returns false
        withViewModel { viewModel ->
            runCurrent()
            assertFalse(viewModel.uiState.value.isConfigured)
        }
    }

    @Test
    fun `cached success sets isShowingCachedData`() =
        runTest(testDispatcher, timeout = TEST_TIMEOUT) {
            coEvery { checkGrafanaHealthUseCase() } returns NetworkResult.Success(
                GrafanaHealth(version = "10.0", database = "ok", isHealthy = true),
            )
            coEvery { getDashboardsUseCase(any(), any(), any(), any()) } returns
                NetworkResult.Success(emptyList(), fromCache = true)
            coEvery { getNewRelicApplicationsUseCase(any()) } returns
                NetworkResult.Success(emptyList())
            coEvery { newRelicRepository.getAlertViolations(any()) } returns
                NetworkResult.Success(emptyList())

            withViewModel { viewModel ->
                runCurrent()
                assertTrue(viewModel.uiState.value.isShowingCachedData)
            }
        }

    @Test
    fun `missing grafana url is reported as unconfigured, not as an error`() =
        runTest(testDispatcher, timeout = TEST_TIMEOUT) {
            every { grafanaBaseUrlProvider.isConfigured() } returns false
            coEvery { checkGrafanaHealthUseCase() } returns NetworkResult.Error(
                message = "Grafana server address is not configured",
                exception = GrafanaNotConfiguredException(),
            )
            coEvery { getDashboardsUseCase(any(), any(), any(), any()) } returns
                NetworkResult.Success(emptyList())
            coEvery { getNewRelicApplicationsUseCase(any()) } returns
                NetworkResult.Success(emptyList())
            coEvery { newRelicRepository.getAlertViolations(any()) } returns
                NetworkResult.Success(emptyList())

            withViewModel { viewModel ->
                runCurrent()

                val state = viewModel.uiState.value
                assertTrue(state.grafanaNotConfigured)
                // The setup prompt replaces the error, it does not appear alongside it.
                assertNull(state.grafanaHealthError)
            }
        }

    @Test
    fun `real grafana failure is reported as an error, not as unconfigured`() =
        runTest(testDispatcher, timeout = TEST_TIMEOUT) {
            coEvery { checkGrafanaHealthUseCase() } returns NetworkResult.Error(
                code = 502,
                message = "Bad gateway",
                exception = java.io.IOException("Bad gateway"),
            )
            coEvery { getDashboardsUseCase(any(), any(), any(), any()) } returns
                NetworkResult.Success(emptyList())
            coEvery { getNewRelicApplicationsUseCase(any()) } returns
                NetworkResult.Success(emptyList())
            coEvery { newRelicRepository.getAlertViolations(any()) } returns
                NetworkResult.Success(emptyList())

            withViewModel { viewModel ->
                runCurrent()

                val state = viewModel.uiState.value
                assertFalse(state.grafanaNotConfigured)
                assertEquals("Bad gateway", state.grafanaHealthError)
            }
        }
}
