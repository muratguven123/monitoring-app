package com.monitoring.dashboard.data.remote

import com.monitoring.dashboard.BuildConfig
import com.monitoring.dashboard.data.local.SecurePreferencesManager
import com.monitoring.dashboard.domain.model.GrafanaServerUrl
import com.monitoring.dashboard.domain.model.GrafanaUrlResult
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for which Grafana server the app talks to.
 *
 * Resolution order:
 *  1. The address the user saved in Settings.
 *  2. `BuildConfig.GRAFANA_BASE_URL` — a build-time default. Empty in release
 *     builds (every deployment points at a different server), set to the local
 *     emulator in debug builds.
 *
 * If neither yields a valid address the app is *unconfigured*, which is a
 * first-class state rather than a network failure: requests are refused before
 * they leave the device so the user gets "set your Grafana URL" instead of an
 * opaque DNS error.
 */
@Singleton
class GrafanaBaseUrlProvider @Inject constructor(
    private val securePreferencesManager: SecurePreferencesManager,
) {

    /** The currently effective server, or `null` when not configured. */
    fun current(): GrafanaServerUrl? = resolve().urlOrNull()

    fun isConfigured(): Boolean = current() != null

    /** Full resolution result, including *why* an address was rejected. */
    fun resolve(): GrafanaUrlResult {
        val stored = GrafanaServerUrl.parse(securePreferencesManager.getGrafanaBaseUrl())
        if (stored is GrafanaUrlResult.Valid) return stored

        val fallback = GrafanaServerUrl.parse(BuildConfig.GRAFANA_BASE_URL)
        if (fallback is GrafanaUrlResult.Valid) return fallback

        // Surface the user's invalid input rather than the empty fallback —
        // "this URL is malformed" is more useful than "nothing is configured".
        return stored
    }

    companion object {
        /**
         * Host used as Retrofit's compile-time base URL.
         *
         * Retrofit needs a base URL when it is constructed, but the app is a
         * singleton and the user can change servers at any time, so the real
         * address is applied per-request by [DynamicBaseUrlInterceptor] instead.
         *
         * Retrofit is therefore *always* pointed at this placeholder — never at
         * the configured server. That gives the interceptor an exact, idempotent
         * rule: rewrite requests addressed to the placeholder, leave everything
         * else alone. Absolute URLs the app builds itself (Coil panel renders)
         * already carry the right origin and path and must not be rewritten.
         *
         * `.invalid` is reserved by RFC 2606 and can never resolve, so a request
         * that escapes the interceptor fails loudly instead of reaching a real
         * host.
         */
        const val PLACEHOLDER_HOST = "grafana-not-configured.invalid"

        const val UNCONFIGURED_PLACEHOLDER_URL = "https://$PLACEHOLDER_HOST/"
    }
}

/**
 * Thrown by the Grafana interceptor when no valid server address is configured.
 *
 * Extends [IOException] so Retrofit/OkHttp treat it as a normal call failure and
 * repositories can map it to a dedicated UI state instead of a generic error.
 */
class GrafanaNotConfiguredException(
    message: String = "Grafana server address is not configured",
) : IOException(message)
