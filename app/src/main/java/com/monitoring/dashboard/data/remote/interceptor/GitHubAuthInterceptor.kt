package com.monitoring.dashboard.data.remote.interceptor

import com.monitoring.dashboard.data.local.SecurePreferencesManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubAuthInterceptor @Inject constructor(
    private val securePreferencesManager: SecurePreferencesManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = securePreferencesManager.getGithubToken()
        val request = if (!token.isNullOrBlank()) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .build()
        } else {
            chain.request().newBuilder()
                .header("Accept", "application/vnd.github+json")
                .build()
        }
        return chain.proceed(request)
    }
}
