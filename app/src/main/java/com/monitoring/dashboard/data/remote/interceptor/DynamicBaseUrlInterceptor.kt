package com.monitoring.dashboard.data.remote.interceptor

import com.monitoring.dashboard.data.remote.GrafanaBaseUrlProvider
import com.monitoring.dashboard.data.remote.GrafanaNotConfiguredException
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Points Grafana requests at the server the user configured, so the address can
 * change at runtime without rebuilding Retrofit.
 *
 * Retrofit is always constructed against [GrafanaBaseUrlProvider.PLACEHOLDER_HOST];
 * this interceptor swaps in the real scheme, host, port **and path prefix**.
 *
 * The path prefix matters: a Grafana mounted at
 * `https://intranet.example.com/grafana` needs `/api/health` sent to
 * `/grafana/api/health`, otherwise the reverse proxy answers with its own 404 and
 * a perfectly healthy server looks broken.
 *
 * Requests that are *not* addressed to the placeholder are passed through
 * untouched. Those are absolute URLs the app built itself — Coil panel render
 * URLs, which already contain the configured origin and prefix. Rewriting them
 * would apply the prefix a second time (`/grafana/grafana/render/...`).
 *
 * When nothing valid is configured the call is aborted with
 * [GrafanaNotConfiguredException] rather than being sent to the placeholder host.
 */
@Singleton
class DynamicBaseUrlInterceptor @Inject constructor(
    private val baseUrlProvider: GrafanaBaseUrlProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url

        if (originalUrl.host != GrafanaBaseUrlProvider.PLACEHOLDER_HOST) {
            return chain.proceed(originalRequest)
        }

        val server = baseUrlProvider.current() ?: throw GrafanaNotConfiguredException()

        val rewrittenUrl = originalUrl.newBuilder()
            .scheme(server.scheme)
            .host(server.host)
            .port(server.port)
            .encodedPath(server.pathPrefix + originalUrl.encodedPath)
            .build()

        return chain.proceed(
            originalRequest.newBuilder()
                .url(rewrittenUrl)
                .build(),
        )
    }
}
