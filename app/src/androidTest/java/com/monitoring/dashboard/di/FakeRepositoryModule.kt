package com.monitoring.dashboard.di

import com.monitoring.dashboard.data.repository.GrafanaRepository
import com.monitoring.dashboard.data.repository.NewRelicRepository
import com.monitoring.dashboard.fake.FakeGrafanaRepository
import com.monitoring.dashboard.fake.FakeNewRelicRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RepositoryModule::class],
)
object FakeRepositoryModule {

    @Provides
    @Singleton
    fun provideGrafanaRepository(): GrafanaRepository = FakeGrafanaRepository()

    @Provides
    @Singleton
    fun provideNewRelicRepository(): NewRelicRepository = FakeNewRelicRepository()
}
