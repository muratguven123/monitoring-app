package com.monitoring.dashboard.data.repository

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
import javax.inject.Singleton

@Singleton
class NewRelicRepositoryImpl @Inject constructor(
    private val apiService: NewRelicApiService,
    private val ioDispatcher: CoroutineDispatcher,
) : NewRelicRepository {

    // ── Applications ────────────────────────────────────────────────────

    override suspend fun getApplications(
        filterName: String?,
    ): NetworkResult<List<NewRelicApplicationDto>> = safeApiCall {
        apiService.getApplications(filterName = filterName)
    }.map { it.applications }

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

    // ── Alerts ──────────────────────────────────────────────────────────

    override suspend fun getAlertViolations(
        onlyOpen: Boolean?,
    ): NetworkResult<List<AlertViolationDto>> = safeApiCall {
        apiService.getAlertViolations(onlyOpen = onlyOpen)
    }.map { it.violations }

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
