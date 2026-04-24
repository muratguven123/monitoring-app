package com.monitoring.dashboard.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides a [WorkManager] instance for dependency injection.
 *
 * WorkManager is a singleton scoped to the application lifecycle.
 * Worker classes annotated with [@HiltWorker][androidx.hilt.work.HiltWorker]
 * receive their dependencies via [androidx.hilt.work.HiltWorkerFactory],
 * which is wired up in [com.monitoring.dashboard.MonitoringApp].
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkManagerModule {

    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context,
    ): WorkManager = WorkManager.getInstance(context)
}
