package com.monitoring.dashboard.di

import com.monitoring.dashboard.data.local.dao.AlertDao
import com.monitoring.dashboard.data.local.dao.GrafanaDao
import com.monitoring.dashboard.data.local.dao.NewRelicDao
import com.monitoring.dashboard.data.remote.GrafanaApiService
import com.monitoring.dashboard.data.remote.NewRelicApiService
import com.monitoring.dashboard.data.repository.GrafanaRepository
import com.monitoring.dashboard.data.repository.GrafanaRepositoryImpl
import com.monitoring.dashboard.data.repository.NewRelicRepository
import com.monitoring.dashboard.data.repository.NewRelicRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Named
import javax.inject.Singleton

/**
 * Repository bindings live in their own module so instrumented tests can replace
 * them with fakes via `@TestInstallIn(replaces = [RepositoryModule::class])`
 * without rebuilding Retrofit / OkHttp.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideGrafanaRepository(
        apiService: GrafanaApiService,
        grafanaDao: GrafanaDao,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @Named("cacheTtlMs") cacheTtlMs: Long,
    ): GrafanaRepository =
        GrafanaRepositoryImpl(apiService, grafanaDao, ioDispatcher, cacheTtlMs)

    @Provides
    @Singleton
    fun provideNewRelicRepository(
        apiService: NewRelicApiService,
        newRelicDao: NewRelicDao,
        alertDao: AlertDao,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @Named("cacheTtlMs") cacheTtlMs: Long,
    ): NewRelicRepository =
        NewRelicRepositoryImpl(apiService, newRelicDao, alertDao, ioDispatcher, cacheTtlMs)
}
