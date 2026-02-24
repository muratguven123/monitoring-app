package com.monitoring.dashboard.data.remote.interceptor

import com.monitoring.dashboard.data.local.SecurePreferencesManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor that attaches the Grafana Bearer token to every outgoing request.
 *
 * The API key is read at request-time from [SecurePreferencesManager] so that the user
 * can update it without restarting the app or recreating the OkHttp client.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val securePreferencesManager: SecurePreferencesManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val apiKey = securePreferencesManager.getGrafanaApiKey()

        val request = if (!apiKey.isNullOrBlank()) {
            originalRequest.newBuilder()
                .header(HEADER_AUTHORIZATION, "$BEARER_PREFIX$apiKey")
                .header(HEADER_ACCEPT, ACCEPT_JSON)
                .build()
        } else {
            originalRequest.newBuilder()
                .header(HEADER_ACCEPT, ACCEPT_JSON)
                .build()
        }

        return chain.proceed(request)
    }

    companion object {
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val HEADER_ACCEPT = "Accept"
        private const val BEARER_PREFIX = "Bearer "
        private const val ACCEPT_JSON = "application/json"
    }
}

