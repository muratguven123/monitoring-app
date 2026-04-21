package com.monitoring.dashboard.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.monitoring.dashboard.data.local.dao.AlertDao
import com.monitoring.dashboard.data.local.entity.AlertViolationEntity
import com.monitoring.dashboard.data.remote.dto.newrelic.AlertViolationDto
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.data.repository.NewRelicRepository
import com.monitoring.dashboard.notification.AlertNotificationHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AlertMonitorWorkerTest {

    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters
    private lateinit var newRelicRepository: NewRelicRepository
    private lateinit var notificationHelper: AlertNotificationHelper
    private lateinit var alertDao: AlertDao

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        workerParams = mockk(relaxed = true)
        newRelicRepository = mockk()
        notificationHelper = mockk(relaxed = true)
        alertDao = mockk(relaxed = true)
    }

    private fun createWorker() = AlertMonitorWorker(
        context = context,
        workerParams = workerParams,
        newRelicRepository = newRelicRepository,
        notificationHelper = notificationHelper,
        alertDao = alertDao,
    )

    @Test
    fun `new violation triggers notification`() = runTest {
        // Given: one new violation, nothing in cache
        val violation = AlertViolationDto(
            id = 1L,
            label = "High CPU",
            policyName = "Infra Policy",
            priority = "critical",
            openedAt = System.currentTimeMillis(),
        )
        coEvery { newRelicRepository.getAlertViolations(onlyOpen = true) } returns
            NetworkResult.Success(listOf(violation))
        coEvery { alertDao.getAll() } returns emptyList()

        // When
        val worker = createWorker()
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) {
            notificationHelper.showAlertNotification(
                newViolationCount = 1,
                policyName = "Infra Policy",
                isCritical = true,
            )
        }
        coVerify { alertDao.insertAll(any()) }
    }

    @Test
    fun `same violation again does not trigger notification (dedup)`() = runTest {
        // Given: violation already in DB cache
        val violation = AlertViolationDto(
            id = 1L,
            label = "High CPU",
            policyName = "Infra Policy",
            priority = "critical",
            openedAt = System.currentTimeMillis(),
        )
        coEvery { newRelicRepository.getAlertViolations(onlyOpen = true) } returns
            NetworkResult.Success(listOf(violation))
        coEvery { alertDao.getAll() } returns listOf(
            AlertViolationEntity(
                id = 1L,
                label = "High CPU",
                policyName = "Infra Policy",
                openedAt = System.currentTimeMillis(),
                severity = "critical",
            ),
        )

        // When
        val worker = createWorker()
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) {
            notificationHelper.showAlertNotification(any(), any(), any())
        }
    }
}
