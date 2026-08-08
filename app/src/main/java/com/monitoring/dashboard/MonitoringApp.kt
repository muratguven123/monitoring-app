package com.monitoring.dashboard

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.monitoring.dashboard.crash.CrashReporting
import com.monitoring.dashboard.crash.LogSanitizer
import com.monitoring.dashboard.data.local.UserPreferencesRepository
import com.monitoring.dashboard.notification.AlertNotificationHelper
import com.monitoring.dashboard.ui.AppLockController
import com.monitoring.dashboard.worker.AlertMonitorWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class MonitoringApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notificationHelper: AlertNotificationHelper
    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository
    @Inject lateinit var appLockController: AppLockController

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.ERROR)
            .build()

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = false
        } else {
            CrashReporting.install(CrashReporting.CrashlyticsSink())
            Timber.plant(ProductionTree())
            FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = true
        }

        notificationHelper.createNotificationChannels()
        appLockController.start()

        AlertMonitorWorker.schedule(WorkManager.getInstance(applicationContext))
        appScope.launch {
            val minutes = userPreferencesRepository.notificationPreferences.first().pollIntervalMinutes
            AlertMonitorWorker.schedule(
                WorkManager.getInstance(applicationContext),
                minutes.toLong(),
            )
        }
    }

    /**
     * Release logging tree: writes to Logcat and CrashReporting without calling Timber
     * again (avoids recursion if a Sink used Timber).
     */
    private class ProductionTree : Timber.Tree() {
        override fun isLoggable(tag: String?, priority: Int): Boolean =
            priority >= Log.WARN

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (!isLoggable(tag, priority)) return
            val sanitized = LogSanitizer.sanitize(message)
            Log.println(priority, tag ?: "MonitoringApp", sanitized)
            CrashReporting.log(sanitized)
            t?.let { CrashReporting.record(it) }
        }
    }
}
