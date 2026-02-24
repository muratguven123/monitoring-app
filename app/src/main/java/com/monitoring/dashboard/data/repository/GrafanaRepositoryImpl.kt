package com.monitoring.dashboard.data.repository

}
    }
        }
            )
                exception = e,
                message = e.localizedMessage ?: "Unknown error",
            NetworkResult.Error(
            Timber.e(e, "Grafana API call failed")
        } catch (e: Exception) {
            }
                )
                    message = errorBody ?: response.message(),
                    code = response.code(),
                NetworkResult.Error(
                Timber.e("Grafana API error [${response.code()}]: $errorBody")
                val errorBody = response.errorBody()?.string()
            } else {
                }
                    )
                        message = "Response body is null",
                        code = response.code(),
                    NetworkResult.Error(
                } else {
                    NetworkResult.Success(body)
                if (body != null) {
                val body = response.body()
            if (response.isSuccessful) {
            val response = apiCall()
        try {
    ): NetworkResult<T> = withContext(ioDispatcher) {
        apiCall: suspend () -> Response<T>,
    private suspend fun <T> safeApiCall(
     */
     * to [NetworkResult.Success] or [NetworkResult.Error].
     * Wraps a Retrofit suspend call in a try/catch and maps the result
    /**

    // ── Internal helper ───────────────────────────────────────────────────

    }
        apiService.getHealth()
    override suspend fun getHealth(): NetworkResult<GrafanaHealthDto> = safeApiCall {

    // ── Health ─────────────────────────────────────────────────────────────

    }
        apiService.getDatasourceByUid(uid)
    ): NetworkResult<DatasourceDto> = safeApiCall {
        uid: String,
    override suspend fun getDatasourceByUid(

    }
        apiService.getDatasourceById(id)
    ): NetworkResult<DatasourceDto> = safeApiCall {
        id: Long,
    override suspend fun getDatasourceById(

    }
        apiService.getDatasources()
    override suspend fun getDatasources(): NetworkResult<List<DatasourceDto>> = safeApiCall {

    // ── Datasources ───────────────────────────────────────────────────────

    }
        apiService.getDashboardByUid(uid)
    ): NetworkResult<DashboardDetailResponseDto> = safeApiCall {
        uid: String,
    override suspend fun getDashboardByUid(

    }
        )
            page = page,
            limit = limit,
            starred = starred,
            tag = tag,
            type = type,
            query = query,
        apiService.searchDashboards(
    ): NetworkResult<List<DashboardSearchHitDto>> = safeApiCall {
        page: Int?,
        limit: Int?,
        starred: Boolean?,
        tag: String?,
        type: String?,
        query: String?,
    override suspend fun searchDashboards(

    // ── Dashboards ────────────────────────────────────────────────────────

) : GrafanaRepository {
    private val ioDispatcher: CoroutineDispatcher,
    private val apiService: GrafanaApiService,
class GrafanaRepositoryImpl @Inject constructor(
@Singleton
 */
 * Delegates to [GrafanaApiService] and maps Retrofit [Response] objects to [NetworkResult].
 * Concrete implementation of [GrafanaRepository].
/**

import javax.inject.Singleton
import javax.inject.Inject
import timber.log.Timber
import retrofit2.Response
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineDispatcher
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.data.remote.dto.GrafanaHealthDto
import com.monitoring.dashboard.data.remote.dto.DatasourceDto
import com.monitoring.dashboard.data.remote.dto.DashboardSearchHitDto
import com.monitoring.dashboard.data.remote.dto.DashboardDetailResponseDto
import com.monitoring.dashboard.data.remote.GrafanaApiService

