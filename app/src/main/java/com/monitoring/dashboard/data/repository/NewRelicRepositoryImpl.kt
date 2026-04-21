package com.monitoring.dashboard.data.repository

import com.monitoring.dashboard.data.local.dao.AlertDao
import com.monitoring.dashboard.data.local.dao.NewRelicDao
import com.monitoring.dashboard.data.local.entity.AlertViolationEntity
import com.monitoring.dashboard.data.local.entity.NewRelicAppEntity
import com.monitoring.dashboard.data.remote.NewRelicApiService
import com.monitoring.dashboard.data.remote.dto.newrelic.AlertViolationDto
import com.monitoring.dashboard.data.remote.dto.newrelic.MetricDataDto
import com.monitoring.dashboard.data.remote.dto.newrelic.MetricNameDto
import com.monitoring.dashboard.data.remote.dto.newrelic.NewRelicApplicationDto
import com.monitoring.dashboard.data.remote.util.NetworkResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Concrete implementation of [NewRelicRepository].
 *
 * Uses a **NetworkBoundResource** strategy for applications and alert violations:
 * 1. Evict stale cache entries beyond TTL.
 * 2. Fire network request.
 * 3. On success → update DB, return fresh data.
 * 4. On failure → return cached data with an error log.
 */
@Singleton
class NewRelicRepositoryImpl @Inject constructor(
    private val apiService: NewRelicApiService,
    private val newRelicDao: NewRelicDao,
    private val alertDao: AlertDao,
    private val ioDispatcher: CoroutineDispatcher,
    @Named("cacheTtlMs") private val cacheTtlMs: Long,
) : NewRelicRepository {

    // ── Applications (NetworkBoundResource) ─────────────────────────────

    override suspend fun getApplications(
        filterName: String?,
    ): NetworkResult<List<NewRelicApplicationDto>> = withContext(ioDispatcher) {
        newRelicDao.deleteOlderThan(System.currentTimeMillis() - cacheTtlMs)

        val networkResult = safeApiCall {
            apiService.getApplications(filterName = filterName)
        }.map { it.applications }

        when (networkResult) {
            is NetworkResult.Success -> {
                val entities = networkResult.data.map { it.toEntity() }
                newRelicDao.deleteAll()
                newRelicDao.insertAll(entities)
                networkResult
            }
            is NetworkResult.Error -> {
                val cached = newRelicDao.getAll()
                if (cached.isNotEmpty()) {
                    Timber.w("New Relic network error, serving ${cached.size} cached apps")
                    NetworkResult.Success(cached.map { it.toDto() })
                } else {
                    networkResult
                }
            }
            is NetworkResult.Loading -> networkResult
        }
    }

    override suspend fun getApplicationById(
        id: Long,
    ): NetworkResult<NewRelicApplicationDto> = safeApiCall {
        apiService.getApplicationById(id)
    }.map { it.application }

    // ── Metrics ─────────────────────────────────────────────────────────

    override suspend fun getMetricNames(
        applicationId: Long,
        name: String?,
    ): NetworkResult<List<MetricNameDto>> = safeApiCall {
        apiService.getMetricNames(applicationId, name)
    }.map { it.metrics }

    override suspend fun getMetricData(
        applicationId: Long,
        names: List<String>,
        from: String?,
        to: String?,
        period: Int?,
        summarize: Boolean?,
    ): NetworkResult<MetricDataDto> = safeApiCall {
        apiService.getMetricData(
            applicationId = applicationId,
            names = names,
            from = from,
            to = to,
            period = period,
            summarize = summarize,
        )
    }.map { it.metricData }

    // ── Alerts (NetworkBoundResource) ───────────────────────────────────

    override suspend fun getAlertViolations(
        onlyOpen: Boolean?,
    ): NetworkResult<List<AlertViolationDto>> = withContext(ioDispatcher) {
        alertDao.deleteOlderThan(System.currentTimeMillis() - cacheTtlMs)

        val networkResult = safeApiCall {
            apiService.getAlertViolations(onlyOpen = onlyOpen)
        }.map { it.violations }

        when (networkResult) {
            is NetworkResult.Success -> {
                val entities = networkResult.data.map { it.toEntity() }
                alertDao.deleteAll()
                alertDao.insertAll(entities)
                networkResult
            }
            is NetworkResult.Error -> {
                val cached = alertDao.getAll()
                if (cached.isNotEmpty()) {
                    Timber.w("New Relic network error, serving ${cached.size} cached violations")
                    NetworkResult.Success(cached.map { it.toDto() })
                } else {
                    networkResult
                }
            }
            is NetworkResult.Loading -> networkResult
        }
    }

    // ── Mapping helpers ────────────────────────────────────────────────

    private fun NewRelicApplicationDto.toEntity() = NewRelicAppEntity(
        id = id,
        name = name,
        language = language,
        healthStatus = healthStatus,
        reporting = reporting,
    )

    private fun NewRelicAppEntity.toDto() = NewRelicApplicationDto(
        id = id,
        name = name,
        language = language,
        healthStatus = healthStatus,
        reporting = reporting,
    )

    private fun AlertViolationDto.toEntity() = AlertViolationEntity(
        id = id,
        label = label,
        policyName = policyName,
        openedAt = openedAt,
        severity = priority,
    )

    private fun AlertViolationEntity.toDto() = AlertViolationDto(
        id = id,
        label = label,
        policyName = policyName,
        openedAt = openedAt,
        priority = severity,
    )

    // ── Internal helper ─────────────────────────────────────────────────

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
                Timber.e("New Relic API error [${response.code()}]: $errorBody")
                NetworkResult.Error(
                    code = response.code(),
                    message = errorBody ?: response.message(),
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "New Relic API call failed")
            NetworkResult.Error(
                message = e.localizedMessage ?: "Unknown error",
                exception = e,
            )
        }
    }
}
