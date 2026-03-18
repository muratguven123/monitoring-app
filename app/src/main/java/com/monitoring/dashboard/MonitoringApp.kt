package com.monitoring.dashboard

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.monitoring.dashboard.notification.AlertNotificationHelper
import com.monitoring.dashboard.worker.AlertMonitorWorker
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Application class.
 *
 * Implements [Configuration.Provider] so WorkManager uses [HiltWorkerFactory]
 * to inject dependencies into [@HiltWorker][androidx.hilt.work.HiltWorker] classes.
 *
 * On startup:
 *  1. Plants Timber for debug logging.
 *  2. Creates notification channels (idempotent – safe to call every launch).
 *  3. Schedules the periodic [AlertMonitorWorker] (also idempotent).
 */
@HiltAndroidApp
class MonitoringApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notificationHelper: AlertNotificationHelper

    // WorkManager configuration – must be provided before WorkManager is used
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Set up notification channels (no-op if already created)
        notificationHelper.createNotificationChannels()

        // Schedule background alert monitoring (keeps existing schedule if already queued)
        // Avoid injecting WorkManager into Application itself; initialize after app startup.
        AlertMonitorWorker.schedule(WorkManager.getInstance(applicationContext))
    }
}

