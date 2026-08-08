package com.monitoring.dashboard.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.domain.usecase.ShouldNotifyViolationUseCase
import com.monitoring.dashboard.domain.usecase.SyncAlertSnapshotUseCase
import com.monitoring.dashboard.notification.AlertNotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

@HiltWorker
class AlertMonitorWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncAlertSnapshotUseCase: SyncAlertSnapshotUseCase,
    private val shouldNotifyViolationUseCase: ShouldNotifyViolationUseCase,
    private val notificationHelper: AlertNotificationHelper,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("AlertMonitorWorker: checking for new violations…")
        return try {
            when (val result = syncAlertSnapshotUseCase()) {
                is NetworkResult.Success -> {
                    val toNotify = shouldNotifyViolationUseCase(result.data.newlyOpened)
                    if (toNotify.isNotEmpty()) {
                        Timber.i("AlertMonitorWorker: ${toNotify.size} new violation(s) to notify")
                        val isCritical = toNotify.any { it.severity?.equals("critical", true) == true }
                        notificationHelper.showAlertNotification(
                            newViolationCount = toNotify.size,
                            policyName = toNotify.firstOrNull()?.policyName,
                            isCritical = isCritical,
                        )
                    } else {
                        Timber.d("AlertMonitorWorker: no notifiable new violations")
                    }
                    Result.success()
                }
                is NetworkResult.Error -> {
                    Timber.w("AlertMonitorWorker: API error – ${result.message}. Retrying later.")
                    Result.retry()
                }
                is NetworkResult.Loading -> Result.retry()
            }
        } catch (e: Exception) {
            Timber.e(e, "AlertMonitorWorker: unexpected error")
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "alert_monitor_periodic_work"
        private const val REPEAT_INTERVAL_MINUTES = 15L

        fun schedule(workManager: WorkManager, intervalMinutes: Long = REPEAT_INTERVAL_MINUTES) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<AlertMonitorWorker>(
                intervalMinutes.coerceAtLeast(15L),
                TimeUnit.MINUTES,
            )
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )

            Timber.i("AlertMonitorWorker scheduled (interval = ${intervalMinutes}min)")
        }
    }
}
