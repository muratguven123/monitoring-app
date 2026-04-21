package com.monitoring.dashboard.data.repository

import com.monitoring.dashboard.data.local.dao.GrafanaDao
import com.monitoring.dashboard.data.local.entity.GrafanaDashboardEntity
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
import javax.inject.Named
import javax.inject.Singleton

/**
 * Concrete implementation of [GrafanaRepository].
 *
 * Uses a **NetworkBoundResource** strategy for dashboard searches:
 * 1. Emit cached data from Room immediately.
 * 2. Fire network request in parallel.
 * 3. On success → update DB, return fresh data.
 * 4. On failure → return cached data with an error log.
 */
@Singleton
class GrafanaRepositoryImpl @Inject constructor(
    private val apiService: GrafanaApiService,
    private val grafanaDao: GrafanaDao,
    private val ioDispatcher: CoroutineDispatcher,
    @Named("cacheTtlMs") private val cacheTtlMs: Long,
) : GrafanaRepository {

    // ── Dashboards (NetworkBoundResource) ─────────────────────────────────

    override suspend fun searchDashboards(
        query: String?,
        type: String?,
        tag: String?,
        starred: Boolean?,
        limit: Int?,
        page: Int?,
    ): NetworkResult<List<DashboardSearchHitDto>> = withContext(ioDispatcher) {
        // Evict stale cache entries
        grafanaDao.deleteOlderThan(System.currentTimeMillis() - cacheTtlMs)

        // Try network first
        val networkResult = safeApiCall {
            apiService.searchDashboards(
                query = query,
                type = type,
                tag = tag,
                starred = starred,
                limit = limit,
                page = page,
            )
        }

        when (networkResult) {
            is NetworkResult.Success -> {
                // Update cache
                val entities = networkResult.data.map { it.toEntity() }
                grafanaDao.deleteAll()
                grafanaDao.insertAll(entities)
                networkResult
            }
            is NetworkResult.Error -> {
                // Serve from cache
                val cached = grafanaDao.getAll()
                if (cached.isNotEmpty()) {
                    Timber.w("Grafana network error, serving ${cached.size} cached dashboards")
                    NetworkResult.Success(cached.map { it.toDto() })
                } else {
                    networkResult
                }
            }
            is NetworkResult.Loading -> networkResult
        }
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

    // ── Mapping helpers ──────────────────────────────────────────────────

    private fun DashboardSearchHitDto.toEntity() = GrafanaDashboardEntity(
        id = id,
        uid = uid,
        title = title,
        tags = tags.joinToString(","),
        url = url,
        folderTitle = folderTitle,
    )

    private fun GrafanaDashboardEntity.toDto() = DashboardSearchHitDto(
        id = id,
        uid = uid,
        title = title,
        uri = "",
        url = url,
        slug = "",
        type = "dash-db",
        tags = if (tags.isBlank()) emptyList() else tags.split(","),
        isStarred = false,
        folderTitle = folderTitle,
    )

    // ── Internal helper ───────────────────────────────────────────────────

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
