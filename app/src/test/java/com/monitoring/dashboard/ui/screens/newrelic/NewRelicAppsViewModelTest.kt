package com.monitoring.dashboard.ui.screens.newrelic

import com.monitoring.dashboard.data.local.UserPreferencesRepository
import com.monitoring.dashboard.data.remote.dto.newrelic.NewRelicApplicationDto
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.domain.usecase.GetNewRelicApplicationsUseCase
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
class NewRelicAppsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getNewRelicApplicationsUseCase: GetNewRelicApplicationsUseCase
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    private fun app(id: Long, name: String) = NewRelicApplicationDto(id = id, name = name)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getNewRelicApplicationsUseCase = mockk()
        userPreferencesRepository = mockk(relaxed = true)
        every { userPreferencesRepository.favoriteAppIds } returns flowOf(emptySet())
        coEvery { getNewRelicApplicationsUseCase(any()) } returns
            NetworkResult.Success(listOf(app(1, "checkout"), app(2, "payments")))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() =
        NewRelicAppsViewModel(getNewRelicApplicationsUseCase, userPreferencesRepository)

    @Test
    fun `starts in loading state`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `successful load populates applications`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf("checkout", "payments"), state.applications.map { it.name })
        assertNull(state.errorMessage)
    }

    @Test
    fun `empty account is not an error`() = runTest(testDispatcher) {
        coEvery { getNewRelicApplicationsUseCase(any()) } returns NetworkResult.Success(emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.applications.isEmpty())
        assertNull(state.errorMessage)
    }

    @Test
    fun `failure surfaces the message and stops loading`() = runTest(testDispatcher) {
        coEvery { getNewRelicApplicationsUseCase(any()) } returns
            NetworkResult.Error(code = 401, message = "Invalid API key")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Invalid API key", state.errorMessage)
    }

    @Test
    fun `failure without a message falls back to a readable default`() = runTest(testDispatcher) {
        coEvery { getNewRelicApplicationsUseCase(any()) } returns NetworkResult.Error(message = null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals("Failed to load applications", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `a successful retry clears the previous error`() = runTest(testDispatcher) {
        coEvery { getNewRelicApplicationsUseCase(any()) } returns
            NetworkResult.Error(message = "offline")

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals("offline", viewModel.uiState.value.errorMessage)

        coEvery { getNewRelicApplicationsUseCase(any()) } returns
            NetworkResult.Success(listOf(app(1, "checkout")))

        viewModel.loadApplications()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `search query is forwarded as a filter`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("checkout")
        advanceUntilIdle()

        assertEquals("checkout", viewModel.uiState.value.searchQuery)
        coVerify { getNewRelicApplicationsUseCase("checkout") }
    }

    @Test
    fun `blank search query is sent as null rather than an empty string`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onSearchQueryChanged("  ")
            advanceUntilIdle()

            coVerify { getNewRelicApplicationsUseCase(null) }
        }

    @Test
    fun `favorites from preferences are reflected in state`() = runTest(testDispatcher) {
        every { userPreferencesRepository.favoriteAppIds } returns flowOf(setOf("1"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(setOf("1"), viewModel.uiState.value.favoriteAppIds)
    }

    @Test
    fun `toggling a favorite delegates to preferences`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleFavorite(1L)
        advanceUntilIdle()

        coVerify { userPreferencesRepository.toggleFavoriteApp(1L) }
    }
}
