package com.monitoring.dashboard.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.domain.model.AlertViolation
import com.monitoring.dashboard.domain.usecase.AlertSyncResult
import com.monitoring.dashboard.domain.usecase.ShouldNotifyViolationUseCase
import com.monitoring.dashboard.domain.usecase.SyncAlertSnapshotUseCase
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
    private lateinit var syncAlertSnapshotUseCase: SyncAlertSnapshotUseCase
    private lateinit var shouldNotifyViolationUseCase: ShouldNotifyViolationUseCase
    private lateinit var notificationHelper: AlertNotificationHelper

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        workerParams = mockk(relaxed = true)
        syncAlertSnapshotUseCase = mockk()
        shouldNotifyViolationUseCase = mockk()
        notificationHelper = mockk(relaxed = true)
    }

    private fun createWorker() = AlertMonitorWorker(
        context = context,
        workerParams = workerParams,
        syncAlertSnapshotUseCase = syncAlertSnapshotUseCase,
        shouldNotifyViolationUseCase = shouldNotifyViolationUseCase,
        notificationHelper = notificationHelper,
    )

    @Test
    fun `new violation triggers notification`() = runTest {
        val violation = AlertViolation(
            id = 1L,
            label = "High CPU",
            policyName = "Infra Policy",
            conditionName = null,
            severity = "critical",
            openedAt = System.currentTimeMillis(),
            isOpen = true,
            resolvedAt = null,
        )
        coEvery { syncAlertSnapshotUseCase() } returns NetworkResult.Success(
            AlertSyncResult(
                openViolations = listOf(violation),
                newlyOpened = listOf(violation),
                newlyResolvedIds = emptyList(),
            ),
        )
        coEvery { shouldNotifyViolationUseCase(listOf(violation)) } returns listOf(violation)

        val result = createWorker().doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) {
            notificationHelper.showAlertNotification(
                newViolationCount = 1,
                policyName = "Infra Policy",
                isCritical = true,
            )
        }
    }

    @Test
    fun `same violation again does not trigger notification (dedup)`() = runTest {
        coEvery { syncAlertSnapshotUseCase() } returns NetworkResult.Success(
            AlertSyncResult(
                openViolations = emptyList(),
                newlyOpened = emptyList(),
                newlyResolvedIds = emptyList(),
            ),
        )
        coEvery { shouldNotifyViolationUseCase(emptyList()) } returns emptyList()

        val result = createWorker().doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) {
            notificationHelper.showAlertNotification(any(), any(), any())
        }
    }
}
