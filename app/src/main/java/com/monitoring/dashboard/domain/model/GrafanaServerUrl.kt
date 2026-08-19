package com.monitoring.dashboard.domain.model

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * A Grafana server address that has been validated and normalised.
 *
 * The app is deployed against arbitrary Grafana installations, so the value the
 * user types is deliberately forgiving. All of these resolve to a usable server:
 *
 * ```
 * grafana.example.com              -> https://grafana.example.com/
 * https://grafana.example.com      -> https://grafana.example.com/
 * grafana.example.com:3000         -> https://grafana.example.com:3000/
 * https://example.com/grafana      -> https://example.com/grafana/    (reverse-proxy sub-path)
 * https://example.com/grafana/     -> https://example.com/grafana/
 * HTTPS://Example.COM/Grafana?x=1  -> https://example.com/grafana/    (query/fragment dropped)
 * ```
 *
 * [pathPrefix] matters: corporate Grafana is frequently mounted under a path on
 * a shared host. Rewriting only scheme/host/port would send `/api/health` to the
 * proxy root and 404.
 */
data class GrafanaServerUrl(
    val scheme: String,
    val host: String,
    val port: Int,
    /** Normalised path prefix: either empty or `/segment[/segment...]` with no trailing slash. */
    val pathPrefix: String,
) {

    /** Canonical form with a trailing slash, suitable as a Retrofit base URL. */
    val baseUrl: String
        get() = buildString {
            append(scheme).append("://").append(host)
            if (!isDefaultPort) append(':').append(port)
            append(pathPrefix)
            append('/')
        }

    val isDefaultPort: Boolean
        get() = (scheme == "https" && port == 443) || (scheme == "http" && port == 80)

    /** True when traffic to this server is unencrypted. */
    val isCleartext: Boolean
        get() = scheme == "http"

    override fun toString(): String = baseUrl

    companion object {
        /**
         * Parses user input into a [GrafanaServerUrl].
         *
         * A bare host is assumed to be HTTPS — the safe default, and what almost
         * every real deployment uses.
         */
        fun parse(raw: String?): GrafanaUrlResult {
            val trimmed = raw?.trim().orEmpty()
            if (trimmed.isEmpty()) return GrafanaUrlResult.NotConfigured

            // Reject schemes we cannot speak before okhttp silently rejects them.
            val schemeSeparator = trimmed.indexOf("://")
            if (schemeSeparator > 0) {
                val scheme = trimmed.substring(0, schemeSeparator).lowercase()
                if (scheme != "http" && scheme != "https") {
                    return GrafanaUrlResult.Invalid(GrafanaUrlError.UNSUPPORTED_SCHEME)
                }
            }

            val withScheme = if (schemeSeparator > 0) trimmed else "https://$trimmed"
            val parsed: HttpUrl = withScheme.toHttpUrlOrNull()
                ?: return GrafanaUrlResult.Invalid(GrafanaUrlError.MALFORMED)

            // Single-label hosts ("grafana") are legal on intranets, so only a
            // completely empty host is rejected.
            if (parsed.host.isBlank()) {
                return GrafanaUrlResult.Invalid(GrafanaUrlError.MISSING_HOST)
            }

            val pathPrefix = parsed.encodedPath
                .trimEnd('/')
                .takeIf { it.isNotEmpty() && it != "/" }
                .orEmpty()

            return GrafanaUrlResult.Valid(
                GrafanaServerUrl(
                    scheme = parsed.scheme,
                    host = parsed.host,
                    port = parsed.port,
                    pathPrefix = pathPrefix,
                ),
            )
        }
    }
}

/** Why a user-entered Grafana address could not be used. */
enum class GrafanaUrlError {
    /** Scheme other than http/https, e.g. `ftp://` or `grafana://`. */
    UNSUPPORTED_SCHEME,

    /** Not parseable as a URL at all. */
    MALFORMED,

    /** Parsed, but there is no host to connect to. */
    MISSING_HOST,
}

sealed interface GrafanaUrlResult {
    /** No address has been entered yet. Distinct from [Invalid] — nothing is wrong, just unset. */
    data object NotConfigured : GrafanaUrlResult

    data class Invalid(val error: GrafanaUrlError) : GrafanaUrlResult

    data class Valid(val url: GrafanaServerUrl) : GrafanaUrlResult

    fun urlOrNull(): GrafanaServerUrl? = (this as? Valid)?.url
}
