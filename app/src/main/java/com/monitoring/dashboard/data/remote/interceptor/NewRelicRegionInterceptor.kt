package com.monitoring.dashboard.data.remote.interceptor

import com.monitoring.dashboard.data.local.SecurePreferencesManager
import com.monitoring.dashboard.domain.model.NewRelicRegion
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Points New Relic REST / NerdGraph calls at the data center the user selected.
 *
 * Retrofit is built against the compile-time US host (or the debug mock). This
 * interceptor rewrites known New Relic API hosts at request time so a Settings
 * change takes effect without rebuilding the client. Non-New Relic hosts
 * (emulator mock `10.0.2.2`) are left untouched.
 */
@Singleton
class NewRelicRegionInterceptor @Inject constructor(
    private val securePreferencesManager: SecurePreferencesManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val host = original.url.host
        if (host !in NewRelicRegion.rewritableHosts) {
            return chain.proceed(original)
        }

        val targetHost = securePreferencesManager.getNewRelicRegion().apiHost
        if (host == targetHost) {
            return chain.proceed(original)
        }

        val rewritten = original.url.newBuilder().host(targetHost).build()
        return chain.proceed(original.newBuilder().url(rewritten).build())
    }
}
