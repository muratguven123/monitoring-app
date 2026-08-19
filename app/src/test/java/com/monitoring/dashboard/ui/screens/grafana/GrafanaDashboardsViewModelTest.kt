package com.monitoring.dashboard.ui.screens.grafana

import com.monitoring.dashboard.data.local.UserPreferencesRepository
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.domain.model.Dashboard
import com.monitoring.dashboard.domain.usecase.GetDashboardsUseCase
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
class GrafanaDashboardsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getDashboardsUseCase: GetDashboardsUseCase
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    private fun dashboard(uid: String, title: String = "Dashboard $uid") = Dashboard(
        id = uid.hashCode().toLong(),
        uid = uid,
        title = title,
        url = "/d/$uid",
        type = "dash-db",
        tags = emptyList(),
        isStarred = false,
        folderTitle = null,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getDashboardsUseCase = mockk()
        userPreferencesRepository = mockk(relaxed = true)
        every { userPreferencesRepository.favoriteDashboardUids } returns flowOf(emptySet())
        coEvery { getDashboardsUseCase(any(), any(), any(), any()) } returns
            NetworkResult.Success(listOf(dashboard("a"), dashboard("b")))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() =
        GrafanaDashboardsViewModel(getDashboardsUseCase, userPreferencesRepository)

    @Test
    fun `starts in loading state`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `successful load populates dashboards`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf("a", "b"), state.dashboards.map { it.uid })
        assertNull(state.errorMessage)
    }

    @Test
    fun `empty result is not an error`() = runTest(testDispatcher) {
        coEvery { getDashboardsUseCase(any(), any(), any(), any()) } returns
            NetworkResult.Success(emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.dashboards.isEmpty())
        assertNull(state.errorMessage)
        assertFalse(state.canLoadMore)
    }

    @Test
    fun `failure surfaces the message and stops loading`() = runTest(testDispatcher) {
        coEvery { getDashboardsUseCase(any(), any(), any(), any()) } returns
            NetworkResult.Error(code = 403, message = "Forbidden")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Forbidden", state.errorMessage)
    }

    @Test
    fun `failure without a message falls back to a readable default`() = runTest(testDispatcher) {
        coEvery { getDashboardsUseCase(any(), any(), any(), any()) } returns
            NetworkResult.Error(message = null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals("Failed to load dashboards", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `full page means more results may exist`() = runTest(testDispatcher) {
        val fullPage = (1..50).map { dashboard("uid-$it") }
        coEvery { getDashboardsUseCase(any(), any(), any(), any()) } returns
            NetworkResult.Success(fullPage)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.canLoadMore)
    }

    @Test
    fun `search query is passed through and resets the list`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        coEvery { getDashboardsUseCase("cpu", any(), any(), any()) } returns
            NetworkResult.Success(listOf(dashboard("cpu-1")))

        viewModel.onSearchQueryChanged("cpu")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("cpu", state.searchQuery)
        assertEquals(listOf("cpu-1"), state.dashboards.map { it.uid })
    }

    @Test
    fun `blank search query is sent as null rather than an empty string`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onSearchQueryChanged("   ")
            advanceUntilIdle()

            // Grafana treats query="" and no query differently; blank input must
            // mean "no filter".
            coVerify { getDashboardsUseCase(null, any(), any(), any()) }
        }

    @Test
    fun `loadMore appends without duplicating existing uids`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Page 2 overlaps page 1 — a dashboard created between requests shifts
        // the window, which is exactly when duplicates appear.
        coEvery { getDashboardsUseCase(any(), any(), any(), any()) } returns
            NetworkResult.Success(listOf(dashboard("b"), dashboard("c")))

        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(listOf("a", "b", "c"), viewModel.uiState.value.dashboards.map { it.uid })
    }

    @Test
    fun `favorites from preferences are reflected in state`() = runTest(testDispatcher) {
        every { userPreferencesRepository.favoriteDashboardUids } returns flowOf(setOf("a"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(setOf("a"), viewModel.uiState.value.favoriteUids)
    }

    @Test
    fun `toggling a favorite delegates to preferences`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleFavorite("a")
        advanceUntilIdle()

        coVerify { userPreferencesRepository.toggleFavoriteDashboard("a") }
    }
}
