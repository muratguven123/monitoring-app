package com.monitoring.dashboard.data.remote.interceptor

import com.monitoring.dashboard.data.local.SecurePreferencesManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor that attaches the New Relic API key to every outgoing request.
 *
 * Uses the `Api-Key` header as recommended by New Relic REST API v2.
 */
@Singleton
class NewRelicAuthInterceptor @Inject constructor(
    private val securePreferencesManager: SecurePreferencesManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val apiKey = securePreferencesManager.getNewRelicApiKey()

        val request = if (!apiKey.isNullOrBlank()) {
            originalRequest.newBuilder()
                .header(HEADER_API_KEY, apiKey)
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
        private const val HEADER_API_KEY = "Api-Key"
        private const val HEADER_ACCEPT = "Accept"
        private const val ACCEPT_JSON = "application/json"
    }
}
