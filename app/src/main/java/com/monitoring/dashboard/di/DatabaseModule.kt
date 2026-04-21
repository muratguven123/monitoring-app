package com.monitoring.dashboard.di

import android.content.Context
import androidx.room.Room
import com.monitoring.dashboard.data.local.MonitoringDatabase
import com.monitoring.dashboard.data.local.dao.AlertDao
import com.monitoring.dashboard.data.local.dao.GrafanaDao
import com.monitoring.dashboard.data.local.dao.NewRelicDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /** Cache time-to-live: 5 minutes in milliseconds. */
    const val CACHE_TTL_MS = 5 * 60 * 1000L

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): MonitoringDatabase =
        Room.databaseBuilder(
            context,
            MonitoringDatabase::class.java,
            "monitoring_database",
        ).build()

    @Provides
    fun provideGrafanaDao(database: MonitoringDatabase): GrafanaDao =
        database.grafanaDao()

    @Provides
    fun provideNewRelicDao(database: MonitoringDatabase): NewRelicDao =
        database.newRelicDao()

    @Provides
    fun provideAlertDao(database: MonitoringDatabase): AlertDao =
        database.alertDao()

    @Provides
    @Singleton
    @Named("cacheTtlMs")
    fun provideCacheTtlMs(): Long = CACHE_TTL_MS
}
