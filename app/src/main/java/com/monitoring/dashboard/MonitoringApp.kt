package com.monitoring.dashboard

import android.app.Application
import android.util.Log
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
 *  1. Plants Timber — DebugTree in debug builds, ProductionTree (W/E only) in release.
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
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.ERROR)
            .build()

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            // Full verbose logging in debug builds
            Timber.plant(Timber.DebugTree())
        } else {
            // Production: only warnings and errors, no sensitive data exposed
            Timber.plant(ProductionTree())
        }

        // Set up notification channels (no-op if already created)
        notificationHelper.createNotificationChannels()

        // Schedule background alert monitoring (keeps existing schedule if already queued)
        AlertMonitorWorker.schedule(WorkManager.getInstance(applicationContext))
    }

    /**
     * Timber tree for production builds.
     *
     * - Logs only WARN and ERROR priority messages.
     * - Strips any message that may contain credentials or tokens (basic guard).
     * - In a real project, replace the body with your crash-reporting SDK
     *   (e.g. Firebase Crashlytics: FirebaseCrashlytics.getInstance().recordException(t)).
     */
    private class ProductionTree : Timber.Tree() {
        override fun isLoggable(tag: String?, priority: Int): Boolean =
            priority >= Log.WARN

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (!isLoggable(tag, priority)) return
            // Sanitize: strip potential API key / token patterns before logging
            val sanitized = message
                .replace(Regex("Bearer [A-Za-z0-9\\-._~+/]+=*"), "Bearer [REDACTED]")
                .replace(Regex("Api-Key: [A-Za-z0-9\\-]+"), "Api-Key: [REDACTED]")
            // Forward to Android logcat (WARN/ERROR only)
            Log.println(priority, tag ?: "MonitoringApp", sanitized)
            // TODO: forward to your crash reporting SDK here, e.g.:
            // FirebaseCrashlytics.getInstance().log(sanitized)
            // t?.let { FirebaseCrashlytics.getInstance().recordException(it) }
        }
    }
}

