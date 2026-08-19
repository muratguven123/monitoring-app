package com.monitoring.dashboard.ui.screens.settings

import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.monitoring.dashboard.data.DataRefreshBus
import com.monitoring.dashboard.data.local.CacheInvalidator
import com.monitoring.dashboard.data.local.MetricThresholds
import com.monitoring.dashboard.data.local.NotificationPreferences
import com.monitoring.dashboard.data.local.SecurePreferencesManager
import com.monitoring.dashboard.data.local.UserPreferencesRepository
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.domain.model.GrafanaHealth
import com.monitoring.dashboard.domain.model.GrafanaUrlError
import com.monitoring.dashboard.domain.model.NewRelicRegion
import com.monitoring.dashboard.domain.usecase.CheckGrafanaHealthUseCase
import com.monitoring.dashboard.domain.usecase.TestNewRelicConnectionUseCase
import com.monitoring.dashboard.ui.AppLockController
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var workManager: WorkManager
    private lateinit var securePreferencesManager: SecurePreferencesManager
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var checkGrafanaHealthUseCase: CheckGrafanaHealthUseCase
    private lateinit var testNewRelicConnectionUseCase: TestNewRelicConnectionUseCase
    private lateinit var cacheInvalidator: CacheInvalidator
    private lateinit var dataRefreshBus: DataRefreshBus
    private lateinit var appLockController: AppLockController

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        workManager = mockk(relaxed = true)
        securePreferencesManager = mockk(relaxed = true)
        userPreferencesRepository = mockk(relaxed = true)
        checkGrafanaHealthUseCase = mockk()
        testNewRelicConnectionUseCase = mockk()
        cacheInvalidator = mockk(relaxed = true)
        dataRefreshBus = DataRefreshBus()
        appLockController = mockk(relaxed = true)

        every { userPreferencesRepository.notificationPreferences } returns
            flowOf(NotificationPreferences())
        every { userPreferencesRepository.metricThresholds } returns flowOf(MetricThresholds())
        every { securePreferencesManager.getGrafanaBaseUrl() } returns null
        every { securePreferencesManager.getActiveProfileId() } returns
            SecurePreferencesManager.PROFILE_DEFAULT
        every { securePreferencesManager.getProfileIds() } returns
            setOf(SecurePreferencesManager.PROFILE_DEFAULT)
        every { securePreferencesManager.getNewRelicRegion() } returns NewRelicRegion.US

        coEvery { checkGrafanaHealthUseCase() } returns NetworkResult.Success(
            GrafanaHealth(version = "10.0", database = "ok", isHealthy = true),
        )
        coEvery { testNewRelicConnectionUseCase() } returns NetworkResult.Success(3)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = SettingsViewModel(
        workManager,
        securePreferencesManager,
        userPreferencesRepository,
        checkGrafanaHealthUseCase,
        testNewRelicConnectionUseCase,
        cacheInvalidator,
        dataRefreshBus,
        appLockController,
    )

    // ── Grafana URL validation ────────────────────────────────────────────

    @Test
    fun `empty url reports the empty state, not an error`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(GrafanaUrlStatus.Empty, viewModel.uiState.value.grafanaUrlStatus)
    }

    @Test
    fun `bare host is accepted and normalised to https`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onGrafanaBaseUrlChanged("grafana.example.com")

        val status = viewModel.uiState.value.grafanaUrlStatus
        assertTrue(status is GrafanaUrlStatus.Valid)
        assertEquals("https://grafana.example.com/", (status as GrafanaUrlStatus.Valid).normalized)
        assertFalse(status.isCleartext)
    }

    @Test
    fun `sub path deployment is accepted and preserved`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onGrafanaBaseUrlChanged("https://intranet.example.com/grafana")

        val status = viewModel.uiState.value.grafanaUrlStatus as GrafanaUrlStatus.Valid
        assertEquals("https://intranet.example.com/grafana/", status.normalized)
    }

    @Test
    fun `http url is valid but flagged as cleartext`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onGrafanaBaseUrlChanged("http://grafana.example.com")

        val status = viewModel.uiState.value.grafanaUrlStatus as GrafanaUrlStatus.Valid
        assertTrue(status.isCleartext)
    }

    @Test
    fun `unsupported scheme is reported as invalid`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onGrafanaBaseUrlChanged("ftp://grafana.example.com")

        assertEquals(
            GrafanaUrlStatus.Invalid(GrafanaUrlError.UNSUPPORTED_SCHEME),
            viewModel.uiState.value.grafanaUrlStatus,
        )
    }

    @Test
    fun `saving normalises the url before persisting it`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onGrafanaBaseUrlChanged("  GRAFANA.example.com  ")
        viewModel.saveSettings()
        advanceUntilIdle()

        // Downstream consumers should never have to re-parse user input.
        verify { securePreferencesManager.saveGrafanaBaseUrl("https://grafana.example.com/") }
    }

    // ── Connection test ───────────────────────────────────────────────────

    @Test
    fun `an invalid url is rejected before any network call`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onGrafanaBaseUrlChanged("ftp://grafana.example.com")
        viewModel.onGrafanaApiKeyChanged("key")
        viewModel.connectAndSave()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.connectionSuccess)
        assertFalse(state.isConnecting)
        coVerify(exactly = 0) { checkGrafanaHealthUseCase() }
    }

    @Test
    fun `connecting with no source configured reports it without calling out`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.connectAndSave()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(false, state.connectionSuccess)
            coVerify(exactly = 0) { checkGrafanaHealthUseCase() }
            coVerify(exactly = 0) { testNewRelicConnectionUseCase() }
        }

    @Test
    fun `a successful grafana connection marks onboarding complete`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onGrafanaBaseUrlChanged("grafana.example.com")
        viewModel.onGrafanaApiKeyChanged("key")
        viewModel.connectAndSave()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(true, state.connectionSuccess)
        assertFalse(state.isConnecting)
        verify { securePreferencesManager.setOnboardingComplete(true) }
    }

    @Test
    fun `a failing grafana connection reports the failure and does not save`() =
        runTest(testDispatcher) {
            coEvery { checkGrafanaHealthUseCase() } returns
                NetworkResult.Error(code = 401, message = "Unauthorized")

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onGrafanaBaseUrlChanged("grafana.example.com")
            viewModel.onGrafanaApiKeyChanged("bad-key")
            viewModel.connectAndSave()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(false, state.connectionSuccess)
            assertTrue(state.saveError)
            assertTrue(state.connectionMessage.orEmpty().contains("Unauthorized"))
            verify(exactly = 0) { securePreferencesManager.setOnboardingComplete(true) }
        }

    @Test
    fun `new relic alone is enough to connect`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onNewRelicApiKeyChanged("NRAK-123")
        viewModel.connectAndSave()
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.connectionSuccess)
        coVerify(exactly = 0) { checkGrafanaHealthUseCase() }
        coVerify { testNewRelicConnectionUseCase() }
    }

    @Test
    fun `saving persists the selected New Relic region`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onNewRelicRegionChanged(NewRelicRegion.EU)
        viewModel.saveSettings()

        verify { securePreferencesManager.saveNewRelicRegion(NewRelicRegion.EU) }
        assertEquals(NewRelicRegion.EU, viewModel.uiState.value.newRelicRegion)
    }

    // ── Profiles and preferences ──────────────────────────────────────────

    @Test
    fun `switching profile clears the cache and asks screens to refresh`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.switchProfile(SecurePreferencesManager.PROFILE_PROD)
            advanceUntilIdle()

            // Stale data from the previous environment would be actively
            // misleading, so the cache must go.
            coVerify { cacheInvalidator.clearAll() }
            verify { securePreferencesManager.loadProfileIntoActive(SecurePreferencesManager.PROFILE_PROD) }
        }

    @Test
    fun `switching to the already active profile is a no-op`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.switchProfile(SecurePreferencesManager.PROFILE_DEFAULT)
        advanceUntilIdle()

        coVerify(exactly = 0) { cacheInvalidator.clearAll() }
    }

    @Test
    fun `toggling app lock updates preferences and the lock controller`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setAppLockEnabled(true)

            verify { securePreferencesManager.setAppLockEnabled(true) }
            verify { appLockController.onAppLockSettingChanged(true) }
            assertTrue(viewModel.uiState.value.appLockEnabled)
        }

    @Test
    fun `changing the poll interval persists it`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setPollIntervalMinutes(60)
        advanceUntilIdle()

        coVerify { userPreferencesRepository.setPollIntervalMinutes(60) }
        // The background poll must actually be rescheduled, not just persisted —
        // otherwise the setting silently has no effect until the next app start.
        verify { workManager.enqueueUniquePeriodicWork(any(), any(), any<PeriodicWorkRequest>()) }
    }

    @Test
    fun `clearing all settings resets the ui state`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onGrafanaBaseUrlChanged("grafana.example.com")

        viewModel.clearAllSettings()

        val state = viewModel.uiState.value
        assertEquals("", state.grafanaBaseUrl)
        assertEquals(GrafanaUrlStatus.Empty, state.grafanaUrlStatus)
        verify { securePreferencesManager.clearAll() }
    }

    @Test
    fun `a failure while saving is surfaced instead of silently swallowed`() =
        runTest(testDispatcher) {
            every { securePreferencesManager.saveGrafanaBaseUrl(any()) } throws
                IllegalStateException("keystore unavailable")

            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onGrafanaBaseUrlChanged("grafana.example.com")

            viewModel.saveSettings()

            val state = viewModel.uiState.value
            assertTrue(state.saveError)
            assertFalse(state.isSaved)
        }
}
