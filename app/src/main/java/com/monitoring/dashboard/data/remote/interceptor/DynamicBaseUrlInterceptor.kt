package com.monitoring.dashboard.data.remote.interceptor

import com.monitoring.dashboard.data.local.SecurePreferencesManager
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor that dynamically rewrites the request host/scheme/port
 * to the Base URL stored in [SecurePreferencesManager].
 *
 * This allows the user to change the Grafana server URL at runtime without
 * rebuilding the Retrofit instance.
 */
@Singleton
class DynamicBaseUrlInterceptor @Inject constructor(
    private val securePreferencesManager: SecurePreferencesManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val storedBaseUrl = securePreferencesManager.getGrafanaBaseUrl()

        val request = if (!storedBaseUrl.isNullOrBlank()) {
            val newBaseUrl = storedBaseUrl.toHttpUrlOrNull()
            if (newBaseUrl != null) {
                val newUrl = originalRequest.url.newBuilder()
                    .scheme(newBaseUrl.scheme)
                    .host(newBaseUrl.host)
                    .port(newBaseUrl.port)
                    .build()

                originalRequest.newBuilder()
                    .url(newUrl)
                    .build()
            } else {
                originalRequest
            }
        } else {
            originalRequest
        }

        return chain.proceed(request)
    }
}

