package com.monitoring.dashboard.data.repository

import com.monitoring.dashboard.data.remote.GrafanaApiService
import com.monitoring.dashboard.data.remote.dto.DashboardDetailResponseDto
import com.monitoring.dashboard.data.remote.dto.DashboardSearchHitDto
import com.monitoring.dashboard.data.remote.dto.DatasourceDto
import com.monitoring.dashboard.data.remote.dto.GrafanaHealthDto
import com.monitoring.dashboard.data.remote.util.NetworkResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [GrafanaRepository].
 * Delegates to [GrafanaApiService] and maps Retrofit [Response] objects to [NetworkResult].
 */
@Singleton
class GrafanaRepositoryImpl @Inject constructor(
    private val apiService: GrafanaApiService,
    private val ioDispatcher: CoroutineDispatcher,
) : GrafanaRepository {

    // ── Dashboards ────────────────────────────────────────────────────────

    override suspend fun searchDashboards(
        query: String?,
        type: String?,
        tag: String?,
        starred: Boolean?,
        limit: Int?,
        page: Int?,
    ): NetworkResult<List<DashboardSearchHitDto>> = safeApiCall {
        apiService.searchDashboards(
            query = query,
            type = type,
            tag = tag,
            starred = starred,
            limit = limit,
            page = page,
        )
    }

    override suspend fun getDashboardByUid(
        uid: String,
    ): NetworkResult<DashboardDetailResponseDto> = safeApiCall {
        apiService.getDashboardByUid(uid)
    }

    // ── Datasources ───────────────────────────────────────────────────────

    override suspend fun getDatasources(): NetworkResult<List<DatasourceDto>> = safeApiCall {
        apiService.getDatasources()
    }

    override suspend fun getDatasourceById(
        id: Long,
    ): NetworkResult<DatasourceDto> = safeApiCall {
        apiService.getDatasourceById(id)
    }

    override suspend fun getDatasourceByUid(
        uid: String,
    ): NetworkResult<DatasourceDto> = safeApiCall {
        apiService.getDatasourceByUid(uid)
    }

    // ── Health ─────────────────────────────────────────────────────────────

    override suspend fun getHealth(): NetworkResult<GrafanaHealthDto> = safeApiCall {
        apiService.getHealth()
    }

    // ── Internal helper ───────────────────────────────────────────────────

    /**
     * Wraps a Retrofit suspend call in a try/catch and maps the result
     * to [NetworkResult.Success] or [NetworkResult.Error].
     */
    private suspend fun <T> safeApiCall(
        apiCall: suspend () -> Response<T>,
    ): NetworkResult<T> = withContext(ioDispatcher) {
        try {
            val response = apiCall()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    NetworkResult.Success(body)
                } else {
                    NetworkResult.Error(
                        code = response.code(),
                        message = "Response body is null",
                    )
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Timber.e("Grafana API error [${response.code()}]: $errorBody")
                NetworkResult.Error(
                    code = response.code(),
                    message = errorBody ?: response.message(),
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Grafana API call failed")
            NetworkResult.Error(
                message = e.localizedMessage ?: "Unknown error",
                exception = e,
            )
        }
    }
}
