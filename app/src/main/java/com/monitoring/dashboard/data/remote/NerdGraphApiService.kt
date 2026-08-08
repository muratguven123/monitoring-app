package com.monitoring.dashboard.data.remote

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class NerdGraphRequest(
    val query: String,
    val variables: Map<String, Any?> = emptyMap(),
)

interface NerdGraphApiService {
    @POST("graphql")
    suspend fun execute(@Body body: NerdGraphRequest): Response<JsonObject>
}
