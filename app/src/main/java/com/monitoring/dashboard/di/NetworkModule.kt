package com.monitoring.dashboard.di

import com.monitoring.dashboard.BuildConfig
import com.monitoring.dashboard.data.remote.GitHubApiService
import com.monitoring.dashboard.data.remote.GrafanaApiService
import com.monitoring.dashboard.data.remote.GrafanaBaseUrlProvider
import com.monitoring.dashboard.data.remote.NerdGraphApiService
import com.monitoring.dashboard.data.remote.NewRelicApiService
import com.monitoring.dashboard.data.remote.interceptor.AuthInterceptor
import com.monitoring.dashboard.data.remote.interceptor.DynamicBaseUrlInterceptor
import com.monitoring.dashboard.data.remote.interceptor.GitHubAuthInterceptor
import com.monitoring.dashboard.data.remote.interceptor.NewRelicAuthInterceptor
import com.monitoring.dashboard.data.remote.interceptor.NewRelicRegionInterceptor
import android.content.Context
import coil.ImageLoader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
    ): Retrofit =
        // Deliberately NOT the configured server address.
        //
        // Retrofit is a singleton built once per process, but the user can change
        // servers in Settings at any time. Baking the address in here would pin
        // the app to whatever was configured at startup. Instead every request
        // leaves addressed to the placeholder and DynamicBaseUrlInterceptor
        // applies the current server — which also gives that interceptor an exact
        // rule for which requests to rewrite.
        Retrofit.Builder()
            .baseUrl(GrafanaBaseUrlProvider.UNCONFIGURED_PLACEHOLDER_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideGrafanaApiService(
        @GrafanaClient retrofit: Retrofit,
    ): GrafanaApiService =
        retrofit.create(GrafanaApiService::class.java)

    // ══════════════════════════════════════════════════════════════════════
    // ██  NEW RELIC  ██████████████████████████████████████████████████████
    // ══════════════════════════════════════════════════════════════════════

    @Provides
    @Singleton
    @NewRelicClient
    fun provideNewRelicOkHttpClient(
        newRelicRegionInterceptor: NewRelicRegionInterceptor,
        newRelicAuthInterceptor: NewRelicAuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(newRelicRegionInterceptor)
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

    // ══════════════════════════════════════════════════════════════════════
    // ██  NERDGRAPH / GITHUB  ██████████████████████████████████████████████
    // ══════════════════════════════════════════════════════════════════════

    @Provides
    @Singleton
    @NerdGraphClient
    fun provideNerdGraphOkHttpClient(
        newRelicRegionInterceptor: NewRelicRegionInterceptor,
        newRelicAuthInterceptor: NewRelicAuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(newRelicRegionInterceptor)
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

    // Grafana URL parsing/normalisation lives in
    // com.monitoring.dashboard.domain.model.GrafanaServerUrl so it can be unit
    // tested and shared with the Settings screen.
}
