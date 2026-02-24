package com.monitoring.dashboard.di

import com.monitoring.dashboard.BuildConfig
import com.monitoring.dashboard.data.local.SecurePreferencesManager
import com.monitoring.dashboard.data.remote.GrafanaApiService
import com.monitoring.dashboard.data.remote.interceptor.AuthInterceptor
import com.monitoring.dashboard.data.remote.interceptor.DynamicBaseUrlInterceptor
import com.monitoring.dashboard.data.repository.GrafanaRepository
import com.monitoring.dashboard.data.repository.GrafanaRepositoryImpl
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

    // ── OkHttpClient (Grafana) ────────────────────────────────────────────

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

    // ── Retrofit (Grafana) ────────────────────────────────────────────────

    @Provides
    @Singleton
    @GrafanaClient
    fun provideGrafanaRetrofit(
        @GrafanaClient okHttpClient: OkHttpClient,
        securePreferencesManager: SecurePreferencesManager,
    ): Retrofit {
        // Use stored base URL if available, otherwise fall back to BuildConfig default.
        val baseUrl = securePreferencesManager.getGrafanaBaseUrl()
            ?.ensureTrailingSlash()
            ?: BuildConfig.GRAFANA_BASE_URL.ensureTrailingSlash()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // ── API Service ───────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideGrafanaApiService(
        @GrafanaClient retrofit: Retrofit,
    ): GrafanaApiService =
        retrofit.create(GrafanaApiService::class.java)

    // ─��� Repository ────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideGrafanaRepository(
        apiService: GrafanaApiService,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): GrafanaRepository =
        GrafanaRepositoryImpl(apiService, ioDispatcher)

    // ── Helpers ───────────────────────────────────────────────────────────

    private const val TIMEOUT_SECONDS = 30L

    private fun String.ensureTrailingSlash(): String =
        if (endsWith("/")) this else "$this/"
}

