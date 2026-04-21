package com.monitoring.dashboard.data.repository

import com.monitoring.dashboard.data.local.dao.AlertDao
import com.monitoring.dashboard.data.local.dao.NewRelicDao
import com.monitoring.dashboard.data.local.entity.NewRelicAppEntity
import com.monitoring.dashboard.data.remote.NewRelicApiService
import com.monitoring.dashboard.data.remote.dto.newrelic.NewRelicApplicationDto
import com.monitoring.dashboard.data.remote.dto.newrelic.NewRelicApplicationsResponseDto
import com.monitoring.dashboard.data.remote.util.NetworkResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class NewRelicRepositoryImplTest {

    private lateinit var apiService: NewRelicApiService
    private lateinit var newRelicDao: NewRelicDao
    private lateinit var alertDao: AlertDao
    private lateinit var repository: NewRelicRepositoryImpl
    private val testDispatcher = StandardTestDispatcher()

    private val sampleAppDto = NewRelicApplicationDto(
        id = 100L,
        name = "my-service",
        language = "java",
        healthStatus = "green",
        reporting = true,
    )

    @Before
    fun setup() {
        apiService = mockk()
        newRelicDao = mockk(relaxed = true)
        alertDao = mockk(relaxed = true)
        repository = NewRelicRepositoryImpl(
            apiService = apiService,
            newRelicDao = newRelicDao,
            alertDao = alertDao,
            ioDispatcher = testDispatcher,
            cacheTtlMs = 5 * 60 * 1000L,
        )
    }

    @Test
    fun `successful fetch updates DB and returns fresh data`() = runTest(testDispatcher) {
        // Given
        val response = Response.success(NewRelicApplicationsResponseDto(listOf(sampleAppDto)))
        coEvery { apiService.getApplications(any()) } returns response
        coEvery { newRelicDao.getAll() } returns emptyList()

        // When
        val result = repository.getApplications()

        // Then
        assertTrue(result is NetworkResult.Success)
        assertEquals(1, (result as NetworkResult.Success).data.size)
        assertEquals("my-service", result.data[0].name)

        coVerify { newRelicDao.deleteAll() }
        coVerify { newRelicDao.insertAll(any()) }
    }

    @Test
    fun `network error returns cached data`() = runTest(testDispatcher) {
        // Given
        coEvery { apiService.getApplications(any()) } throws IOException("No network")
        coEvery { newRelicDao.getAll() } returns listOf(
            NewRelicAppEntity(
                id = 100L,
                name = "cached-service",
                language = "java",
                healthStatus = "green",
                reporting = true,
                cachedAt = System.currentTimeMillis(),
            ),
        )

        // When
        val result = repository.getApplications()

        // Then
        assertTrue(result is NetworkResult.Success)
        assertEquals("cached-service", (result as NetworkResult.Success).data[0].name)
    }

    @Test
    fun `empty cache plus network error returns NetworkResult Error`() = runTest(testDispatcher) {
        // Given
        coEvery { apiService.getApplications(any()) } throws IOException("No network")
        coEvery { newRelicDao.getAll() } returns emptyList()

        // When
        val result = repository.getApplications()

        // Then
        assertTrue(result is NetworkResult.Error)
    }
}
