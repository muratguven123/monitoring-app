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
import com.monitoring.dashboard.data.local.SecurePreferencesManager
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.data.repository.NewRelicRepository
import com.monitoring.dashboard.notification.AlertNotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Background worker that periodically checks New Relic for open alert violations
 * and fires a push notification when new ones are detected since the last run.
 *
 * Scheduled via [schedule] as a unique periodic task so it survives app restarts.
 *
 * Requires network connectivity (CONNECTED constraint) before executing.
 */
@HiltWorker
class AlertMonitorWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val newRelicRepository: NewRelicRepository,
    private val notificationHelper: AlertNotificationHelper,
    private val securePrefsManager: SecurePreferencesManager,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("AlertMonitorWorker: checking for new violations…")
        return try {
            when (val result = newRelicRepository.getAlertViolations(onlyOpen = true)) {
                is NetworkResult.Success -> {
                    val violations = result.data
                    val currentIds  = violations.map { it.id }.toSet()
                    val previousIds = securePrefsManager.getLastKnownViolationIds()

                    // Detect violations that weren't in the last snapshot
                    val newViolations = violations.filter { it.id !in previousIds }

                    if (newViolations.isNotEmpty()) {
                        Timber.i("AlertMonitorWorker: ${newViolations.size} new violation(s) detected")
                        val isCritical = newViolations.any { it.priority?.lowercase() == "critical" }
                        notificationHelper.showAlertNotification(
                            newViolationCount = newViolations.size,
                            policyName        = newViolations.firstOrNull()?.policyName,
                            isCritical        = isCritical,
                        )
                    } else {
                        Timber.d("AlertMonitorWorker: no new violations")
                    }

                    // Persist current snapshot for next run comparison
                    securePrefsManager.saveLastKnownViolationIds(currentIds)
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
        /** Unique name used to avoid duplicate enqueues. */
        const val WORK_NAME = "alert_monitor_periodic_work"

        /** How often the worker runs. WorkManager enforces a minimum of 15 minutes. */
        private const val REPEAT_INTERVAL_MINUTES = 15L

        /**
         * Enqueues (or keeps) the periodic alert-monitoring task.
         * Call once from Application.onCreate(); safe to call multiple times.
         */
        fun schedule(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<AlertMonitorWorker>(
                REPEAT_INTERVAL_MINUTES,
                TimeUnit.MINUTES,
            )
                .setConstraints(constraints)
                // Flex period: worker may run up to 5 min before the deadline
                // .setInitialDelay(1, TimeUnit.MINUTES) // optional: skip first immediate run
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // don't reset timer if already scheduled
                request,
            )

            Timber.i("AlertMonitorWorker scheduled (interval = ${REPEAT_INTERVAL_MINUTES}min)")
        }
    }
}
