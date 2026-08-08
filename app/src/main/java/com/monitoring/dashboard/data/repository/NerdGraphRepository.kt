package com.monitoring.dashboard.data.repository

import com.google.gson.JsonObject
import com.monitoring.dashboard.BuildConfig
import com.monitoring.dashboard.data.local.SecurePreferencesManager
import com.monitoring.dashboard.data.remote.NerdGraphApiService
import com.monitoring.dashboard.data.remote.NerdGraphRequest
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class NrqlResult(
    val rawJson: String,
    val accountId: String,
    val query: String,
)

@Singleton
class NerdGraphRepository @Inject constructor(
    private val apiService: NerdGraphApiService,
    private val securePreferencesManager: SecurePreferencesManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun runNrql(nrql: String): NetworkResult<NrqlResult> = withContext(ioDispatcher) {
        val accountId = securePreferencesManager.getNewRelicAccountId()
        if (accountId.isNullOrBlank()) {
            return@withContext NetworkResult.Error(message = "New Relic Account ID is required for NRQL")
        }
        val graphql = """
            query(${'$'}accountId: Int!, ${'$'}nrql: Nrql!) {
              actor {
                account(id: ${'$'}accountId) {
                  nrql(query: ${'$'}nrql) {
                    results
                  }
                }
              }
            }
        """.trimIndent()
        try {
            val response = apiService.execute(
                NerdGraphRequest(
                    query = graphql,
                    variables = mapOf(
                        "accountId" to accountId.toIntOrNull(),
                        "nrql" to nrql,
                    ),
                ),
            )
            if (response.isSuccessful) {
                val body = response.body() ?: JsonObject()
                NetworkResult.Success(
                    NrqlResult(
                        rawJson = body.toString(),
                        accountId = accountId,
                        query = nrql,
                    ),
                )
            } else {
                NetworkResult.Error(code = response.code(), message = response.message())
            }
        } catch (e: Exception) {
            NetworkResult.Error(message = e.message, exception = e)
        }
    }

    fun nerdGraphBaseUrl(): String = BuildConfig.NEWRELIC_NERDGRAPH_URL
}
