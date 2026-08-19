package com.monitoring.dashboard.ui.screens.alerts

import com.monitoring.dashboard.data.DataRefreshBus
import com.monitoring.dashboard.data.local.UserPreferencesRepository
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.domain.model.AlertViolation
import com.monitoring.dashboard.domain.usecase.AlertSyncResult
import com.monitoring.dashboard.domain.usecase.GetOpenViolationsUseCase
import com.monitoring.dashboard.domain.usecase.SyncAlertSnapshotUseCase
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
class AlertsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getOpenViolationsUseCase: GetOpenViolationsUseCase
    private lateinit var syncAlertSnapshotUseCase: SyncAlertSnapshotUseCase
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var dataRefreshBus: DataRefreshBus

    private val criticalOpen = violation(id = 1, severity = "CRITICAL", isOpen = true)
    private val warningOpen = violation(id = 2, severity = "WARNING", isOpen = true)
    private val resolved = violation(id = 3, severity = "CRITICAL", isOpen = false)
    private val allViolations = listOf(criticalOpen, warningOpen, resolved)

    private fun violation(id: Long, severity: String, isOpen: Boolean) = AlertViolation(
        id = id,
        label = "violation-$id",
        policyName = "policy",
        conditionName = "condition",
        severity = severity,
        openedAt = 1_000L,
        isOpen = isOpen,
        resolvedAt = if (isOpen) null else 2_000L,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getOpenViolationsUseCase = mockk()
        syncAlertSnapshotUseCase = mockk()
        userPreferencesRepository = mockk(relaxed = true)
        dataRefreshBus = DataRefreshBus()

        every { getOpenViolationsUseCase.observe() } returns flowOf(allViolations)
        coEvery { syncAlertSnapshotUseCase() } returns NetworkResult.Success(
            AlertSyncResult(
                openViolations = listOf(criticalOpen, warningOpen),
                newlyOpened = emptyList(),
                newlyResolvedIds = emptyList(),
            ),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = AlertsViewModel(
        getOpenViolationsUseCase,
        syncAlertSnapshotUseCase,
        userPreferencesRepository,
        dataRefreshBus,
    )

    @Test
    fun `starts in loading state`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `emitted violations end loading and populate state`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(allViolations, state.violations)
        assertNull(state.error)
    }

    @Test
    fun `default filter shows only open violations`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AlertFilter.OPEN, state.filter)
        assertEquals(listOf(criticalOpen, warningOpen), state.filtered)
    }

    @Test
    fun `critical filter excludes warnings and resolved`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setFilter(AlertFilter.CRITICAL)

        // Severity comparison must be case-insensitive: the API returns
        // "CRITICAL", "critical" and "Critical" depending on the endpoint.
        assertEquals(listOf(criticalOpen), viewModel.uiState.value.filtered)
    }

    @Test
    fun `resolved filter shows only closed violations`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setFilter(AlertFilter.RESOLVED)

        assertEquals(listOf(resolved), viewModel.uiState.value.filtered)
    }

    @Test
    fun `all filter shows everything`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setFilter(AlertFilter.ALL)

        assertEquals(allViolations, viewModel.uiState.value.filtered)
    }

    @Test
    fun `empty violation list yields an empty filtered list without error`() =
        runTest(testDispatcher) {
            every { getOpenViolationsUseCase.observe() } returns flowOf(emptyList())

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.violations.isEmpty())
            assertTrue(state.filtered.isEmpty())
            assertNull(state.error)
        }

    @Test
    fun `sync failure surfaces the error and stops loading`() = runTest(testDispatcher) {
        coEvery { syncAlertSnapshotUseCase() } returns NetworkResult.Error(
            code = 401,
            message = "Invalid API key",
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Invalid API key", state.error)
    }

    @Test
    fun `cached violations remain visible when the sync fails`() = runTest(testDispatcher) {
        coEvery { syncAlertSnapshotUseCase() } returns NetworkResult.Error(message = "offline")

        val viewModel = createViewModel()
        advanceUntilIdle()

        // A failed refresh must not blank out what the user was already looking at.
        assertEquals(allViolations, viewModel.uiState.value.violations)
    }

    @Test
    fun `refresh bus event triggers a resync`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        dataRefreshBus.requestRefresh()
        advanceUntilIdle()

        // Once on init, once for the bus event.
        coVerify(atLeast = 2) { syncAlertSnapshotUseCase() }
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `muting a violation persists an expiry in the future`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val before = System.currentTimeMillis()

        viewModel.muteForHours(id = 42L, hours = 2)
        advanceUntilIdle()

        coVerify {
            userPreferencesRepository.muteViolation(
                42L,
                match { it >= before + 2 * 60 * 60 * 1000L },
            )
        }
    }
}
