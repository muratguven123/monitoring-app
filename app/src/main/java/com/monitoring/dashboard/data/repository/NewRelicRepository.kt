package com.monitoring.dashboard.data.repository

import com.monitoring.dashboard.data.remote.dto.newrelic.AlertViolationDto
import com.monitoring.dashboard.data.remote.dto.newrelic.MetricDataDto
import com.monitoring.dashboard.data.remote.dto.newrelic.MetricNameDto
import com.monitoring.dashboard.data.remote.dto.newrelic.NewRelicApplicationDto
import com.monitoring.dashboard.data.remote.util.NetworkResult

/**
 * Repository abstraction for New Relic API operations.
 * All methods return [NetworkResult] wrappers for unified error handling.
 */
interface NewRelicRepository {

    // ── Applications ────────────────────────────────────────────────────

    suspend fun getApplications(
        filterName: String? = null,
    ): NetworkResult<List<NewRelicApplicationDto>>

    suspend fun getApplicationById(
        id: Long,
    ): NetworkResult<NewRelicApplicationDto>

    // ── Metrics ─────────────────────────────────────────────────────────

    suspend fun getMetricNames(
        applicationId: Long,
        name: String? = null,
    ): NetworkResult<List<MetricNameDto>>

    suspend fun getMetricData(
        applicationId: Long,
        names: List<String>,
        from: String? = null,
        to: String? = null,
        period: Int? = null,
        summarize: Boolean? = null,
    ): NetworkResult<MetricDataDto>

    // ── Alerts ──────────────────────────────────────────────────────────

    suspend fun getAlertViolations(
        onlyOpen: Boolean? = null,
    ): NetworkResult<List<AlertViolationDto>>
}
