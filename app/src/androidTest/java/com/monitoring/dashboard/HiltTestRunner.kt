package com.monitoring.dashboard

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.test.runner.AndroidJUnitRunner
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.testing.HiltTestApplication

class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?,
    ): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }

    override fun callApplicationOnCreate(app: Application) {
        try {
            WorkManager.initialize(
                app,
                Configuration.Builder()
                    .setMinimumLoggingLevel(Log.DEBUG)
                    .build(),
            )
        } catch (_: IllegalStateException) {
            // Already initialised in this process.
        }
        super.callApplicationOnCreate(app)
    }
}
