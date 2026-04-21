package com.monitoring.dashboard.data.repository

import com.monitoring.dashboard.data.local.dao.GrafanaDao
import com.monitoring.dashboard.data.local.entity.GrafanaDashboardEntity
import com.monitoring.dashboard.data.remote.GrafanaApiService
import com.monitoring.dashboard.data.remote.dto.DashboardSearchHitDto
import com.monitoring.dashboard.data.remote.util.NetworkResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class GrafanaRepositoryImplTest {

    private lateinit var apiService: GrafanaApiService
    private lateinit var grafanaDao: GrafanaDao
    private lateinit var repository: GrafanaRepositoryImpl
    private val testDispatcher = StandardTestDispatcher()

    private val sampleDto = DashboardSearchHitDto(
        id = 1L,
        uid = "abc123",
        title = "Test Dashboard",
        uri = "db/test",
        url = "/d/abc123/test",
        slug = "test",
        type = "dash-db",
        tags = listOf("prod", "infra"),
        isStarred = false,
        folderTitle = "General",
    )

    @Before
    fun setup() {
        apiService = mockk()
        grafanaDao = mockk(relaxed = true)
        repository = GrafanaRepositoryImpl(
            apiService = apiService,
            grafanaDao = grafanaDao,
            ioDispatcher = testDispatcher,
            cacheTtlMs = 5 * 60 * 1000L,
        )
    }

    @Test
    fun `successful fetch updates DB and returns fresh data`() = runTest(testDispatcher) {
        // Given
        val response = Response.success(listOf(sampleDto))
        coEvery { apiService.searchDashboards(any(), any(), any(), any(), any(), any()) } returns response
        coEvery { grafanaDao.getAll() } returns emptyList()

        // When
        val result = repository.searchDashboards()

        // Then
        assertTrue(result is NetworkResult.Success)
        assertEquals(1, (result as NetworkResult.Success).data.size)
        assertEquals("Test Dashboard", result.data[0].title)

        // Verify DB was updated
        coVerify { grafanaDao.deleteAll() }
        coVerify { grafanaDao.insertAll(any()) }
    }

    @Test
    fun `network error returns cached data`() = runTest(testDispatcher) {
        // Given
        coEvery { apiService.searchDashboards(any(), any(), any(), any(), any(), any()) } throws IOException("No network")
        coEvery { grafanaDao.getAll() } returns listOf(
            GrafanaDashboardEntity(
                id = 1L,
                uid = "abc123",
                title = "Cached Dashboard",
                tags = "prod,infra",
                url = "/d/abc123/test",
                folderTitle = "General",
                cachedAt = System.currentTimeMillis(),
            ),
        )

        // When
        val result = repository.searchDashboards()

        // Then
        assertTrue(result is NetworkResult.Success)
        assertEquals("Cached Dashboard", (result as NetworkResult.Success).data[0].title)
    }

    @Test
    fun `empty cache plus network error returns NetworkResult Error`() = runTest(testDispatcher) {
        // Given
        coEvery { apiService.searchDashboards(any(), any(), any(), any(), any(), any()) } throws IOException("No network")
        coEvery { grafanaDao.getAll() } returns emptyList()

        // When
        val result = repository.searchDashboards()

        // Then
        assertTrue(result is NetworkResult.Error)
    }
}
