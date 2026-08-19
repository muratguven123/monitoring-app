package com.monitoring.dashboard.data.repository

import com.google.gson.JsonObject
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
    val rows: List<String> = emptyList(),
    val missingAccountId: Boolean = false,
)

@Singleton
class NerdGraphRepository @Inject constructor(
    private val apiService: NerdGraphApiService,
    private val securePreferencesManager: SecurePreferencesManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    fun hasAccountId(): Boolean = !securePreferencesManager.getNewRelicAccountId().isNullOrBlank()

    suspend fun runNrql(nrql: String): NetworkResult<NrqlResult> = withContext(ioDispatcher) {
        val accountId = securePreferencesManager.getNewRelicAccountId()
        if (accountId.isNullOrBlank()) {
            return@withContext NetworkResult.Error(message = "MISSING_ACCOUNT_ID")
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
                        rows = parseResultRows(body),
                    ),
                )
            } else {
                NetworkResult.Error(code = response.code(), message = response.message())
            }
        } catch (e: Exception) {
            NetworkResult.Error(message = e.message, exception = e)
        }
    }

    private fun parseResultRows(body: JsonObject): List<String> {
        return try {
            val results = body
                .getAsJsonObject("data")
                ?.getAsJsonObject("actor")
                ?.getAsJsonObject("account")
                ?.getAsJsonObject("nrql")
                ?.getAsJsonArray("results")
                ?: return emptyList()
            results.map { element ->
                when {
                    element.isJsonObject -> element.asJsonObject.entrySet()
                        .joinToString(" · ") { (k, v) -> "$k=$v" }
                    else -> element.toString()
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun nerdGraphBaseUrl(): String =
        "https://${securePreferencesManager.getNewRelicRegion().apiHost}/graphql"
}
