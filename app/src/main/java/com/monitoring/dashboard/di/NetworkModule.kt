package com.monitoring.dashboard.di

import com.monitoring.dashboard.BuildConfig
import com.monitoring.dashboard.data.local.SecurePreferencesManager
import com.monitoring.dashboard.data.remote.GitHubApiService
import com.monitoring.dashboard.data.remote.GrafanaApiService
import com.monitoring.dashboard.data.remote.NerdGraphApiService
import com.monitoring.dashboard.data.remote.NewRelicApiService
import com.monitoring.dashboard.data.remote.interceptor.AuthInterceptor
import com.monitoring.dashboard.data.remote.interceptor.DynamicBaseUrlInterceptor
import com.monitoring.dashboard.data.remote.interceptor.GitHubAuthInterceptor
import com.monitoring.dashboard.data.remote.interceptor.NewRelicAuthInterceptor
import com.monitoring.dashboard.data.local.dao.AlertDao
import com.monitoring.dashboard.data.local.dao.GrafanaDao
import com.monitoring.dashboard.data.local.dao.NewRelicDao
import com.monitoring.dashboard.data.repository.GrafanaRepository
import com.monitoring.dashboard.data.repository.GrafanaRepositoryImpl
import com.monitoring.dashboard.data.repository.NewRelicRepository
import com.monitoring.dashboard.data.repository.NewRelicRepositoryImpl
import android.content.Context
import coil.ImageLoader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
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

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NerdGraphClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GitHubClient

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
                // Debug: log full request/response bodies for easier debugging.
                // WARNING: this will log Authorization and Api-Key headers — never enable in release.
                HttpLoggingInterceptor.Level.BODY
            } else {
                // Release: no HTTP logging to prevent leaking credentials or PII.
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
        // Priority: runtime user setting > BuildConfig default > debug fallback (debug only)
        val baseUrl = securePreferencesManager
            .getGrafanaBaseUrl()
            ?.toRetrofitBaseUrlOrNull()
            ?: BuildConfig.GRAFANA_BASE_URL
                .takeIf { it.isNotBlank() }
                ?.toRetrofitBaseUrlOrNull()
            ?: if (BuildConfig.DEBUG) "http://10.0.2.2:3000/" else "https://localhost/"
        // In production the user MUST configure the Grafana URL via Settings.
        // The "https://localhost/" placeholder will simply fail requests until configured.

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
        grafanaDao: GrafanaDao,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @Named("cacheTtlMs") cacheTtlMs: Long,
    ): GrafanaRepository =
        GrafanaRepositoryImpl(apiService, grafanaDao, ioDispatcher, cacheTtlMs)

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
        newRelicDao: NewRelicDao,
        alertDao: AlertDao,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @Named("cacheTtlMs") cacheTtlMs: Long,
    ): NewRelicRepository =
        NewRelicRepositoryImpl(apiService, newRelicDao, alertDao, ioDispatcher, cacheTtlMs)

    // ══════════════════════════════════════════════════════════════════════
    // ██  NERDGRAPH / GITHUB  ██████████████████████████████████████████████
    // ══════════════════════════════════════════════════════════════════════

    @Provides
    @Singleton
    @NerdGraphClient
    fun provideNerdGraphOkHttpClient(
        newRelicAuthInterceptor: NewRelicAuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(newRelicAuthInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    @NerdGraphClient
    fun provideNerdGraphRetrofit(
        @NerdGraphClient okHttpClient: OkHttpClient,
    ): Retrofit {
        val url = BuildConfig.NEWRELIC_NERDGRAPH_URL
            .removeSuffix("/graphql")
            .ensureTrailingSlash()
        return Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideNerdGraphApiService(
        @NerdGraphClient retrofit: Retrofit,
    ): NerdGraphApiService = retrofit.create(NerdGraphApiService::class.java)

    @Provides
    @Singleton
    @GitHubClient
    fun provideGitHubOkHttpClient(
        gitHubAuthInterceptor: GitHubAuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(gitHubAuthInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    @GitHubClient
    fun provideGitHubRetrofit(
        @GitHubClient okHttpClient: OkHttpClient,
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.GITHUB_BASE_URL.ensureTrailingSlash())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideGitHubApiService(
        @GitHubClient retrofit: Retrofit,
    ): GitHubApiService = retrofit.create(GitHubApiService::class.java)

    // ── Grafana Image Loader (Coil) ───────────────────────────────────────
    //
    // Grafana panel render endpoint returns PNG images that require the same
    // Bearer token as the REST API. We wire Coil's ImageLoader to the same
    // OkHttpClient so auth headers are injected automatically.

    @Provides
    @Singleton
    fun provideGrafanaImageLoader(
        @ApplicationContext context: Context,
        @GrafanaClient okHttpClient: OkHttpClient,
    ): ImageLoader =
        ImageLoader.Builder(context)
            .okHttpClient(okHttpClient)
            .crossfade(true)
            .build()

    // ── Helpers ───────────────────────────────────────────────────────────

    private const val TIMEOUT_SECONDS = 30L

    private fun String.ensureTrailingSlash(): String =
        if (endsWith("/")) this else "$this/"

    /**
     * Accepts pasted Grafana links and normalizes them to Retrofit-safe origin URL.
     * Example: https://host/goto/abc?orgId=1  -> https://host/
     */
    private fun String.toRetrofitBaseUrlOrNull(): String? {
        val raw = trim()
        if (raw.isBlank()) return null

        val withScheme = if (raw.startsWith("http://") || raw.startsWith("https://")) {
            raw
        } else {
            "https://$raw"
        }

        val parsed = withScheme.toHttpUrlOrNull() ?: return null
        val defaultPort = (parsed.scheme == "http" && parsed.port == 80) ||
            (parsed.scheme == "https" && parsed.port == 443)
        val portPart = if (defaultPort) "" else ":${parsed.port}"
        return "${parsed.scheme}://${parsed.host}$portPart/"
    }
}
