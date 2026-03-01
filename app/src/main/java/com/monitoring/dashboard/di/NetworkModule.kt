package com.monitoring.dashboard.di

import com.monitoring.dashboard.BuildConfig
import com.monitoring.dashboard.data.local.SecurePreferencesManager
import com.monitoring.dashboard.data.remote.GrafanaApiService
import com.monitoring.dashboard.data.remote.NewRelicApiService
import com.monitoring.dashboard.data.remote.interceptor.AuthInterceptor
import com.monitoring.dashboard.data.remote.interceptor.DynamicBaseUrlInterceptor
import com.monitoring.dashboard.data.remote.interceptor.NewRelicAuthInterceptor
import com.monitoring.dashboard.data.repository.GrafanaRepository
import com.monitoring.dashboard.data.repository.GrafanaRepositoryImpl
import com.monitoring.dashboard.data.repository.NewRelicRepository
import com.monitoring.dashboard.data.repository.NewRelicRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/** Qualifier to distinguish the Grafana-specific OkHttpClient & Retrofit instances. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GrafanaClient

/** Qualifier to distinguish the New Relic-specific OkHttpClient & Retrofit instances. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NewRelicClient

/** Qualifier for the IO dispatcher. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // ── Dispatcher ────────────────────────────────────────────────────────

    @Provides
    @Singleton
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    // ── Logging Interceptor ───────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

    // ══════════════════════════════════════════════════════════════════════
    // ██  GRAFANA  ████████████████████████████████████████████████████████
    // ══════════════════════════════════════════════════════════════════════

    @Provides
    @Singleton
    @GrafanaClient
    fun provideGrafanaOkHttpClient(
        authInterceptor: AuthInterceptor,
        dynamicBaseUrlInterceptor: DynamicBaseUrlInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(dynamicBaseUrlInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    @GrafanaClient
    fun provideGrafanaRetrofit(
        @GrafanaClient okHttpClient: OkHttpClient,
        securePreferencesManager: SecurePreferencesManager,
    ): Retrofit {
        val baseUrl = securePreferencesManager.getGrafanaBaseUrl()
            ?.ensureTrailingSlash()
            ?: BuildConfig.GRAFANA_BASE_URL.ensureTrailingSlash()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideGrafanaApiService(
        @GrafanaClient retrofit: Retrofit,
    ): GrafanaApiService =
        retrofit.create(GrafanaApiService::class.java)

    @Provides
    @Singleton
    fun provideGrafanaRepository(
        apiService: GrafanaApiService,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): GrafanaRepository =
        GrafanaRepositoryImpl(apiService, ioDispatcher)

    // ══════════════════════════════════════════════════════════════════════
    // ██  NEW RELIC  ██████████████████████████████████████████████████████
    // ══════════════════════════════════════════════════════════════════════

    @Provides
    @Singleton
    @NewRelicClient
    fun provideNewRelicOkHttpClient(
        newRelicAuthInterceptor: NewRelicAuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(newRelicAuthInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    @NewRelicClient
    fun provideNewRelicRetrofit(
        @NewRelicClient okHttpClient: OkHttpClient,
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.NEWRELIC_BASE_URL.ensureTrailingSlash())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideNewRelicApiService(
        @NewRelicClient retrofit: Retrofit,
    ): NewRelicApiService =
        retrofit.create(NewRelicApiService::class.java)

    @Provides
    @Singleton
    fun provideNewRelicRepository(
        apiService: NewRelicApiService,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): NewRelicRepository =
        NewRelicRepositoryImpl(apiService, ioDispatcher)

    // ── Helpers ───────────────────────────────────────────────────────────

    private const val TIMEOUT_SECONDS = 30L

    private fun String.ensureTrailingSlash(): String =
        if (endsWith("/")) this else "$this/"
}
