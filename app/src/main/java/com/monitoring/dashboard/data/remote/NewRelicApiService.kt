package com.monitoring.dashboard.data.remote

import com.monitoring.dashboard.data.remote.dto.newrelic.NewRelicAlertViolationsResponseDto
import com.monitoring.dashboard.data.remote.dto.newrelic.NewRelicApplicationResponseDto
import com.monitoring.dashboard.data.remote.dto.newrelic.NewRelicApplicationsResponseDto
import com.monitoring.dashboard.data.remote.dto.newrelic.NewRelicMetricDataResponseDto
import com.monitoring.dashboard.data.remote.dto.newrelic.NewRelicMetricNamesResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit service interface for the New Relic REST API v2.
 *
 * Base URL: https://api.newrelic.com
 * Authentication is handled by [com.monitoring.dashboard.data.remote.interceptor.NewRelicAuthInterceptor].
 */
interface NewRelicApiService {

    // ── Applications ────────────────────────────────────────────────────

    @GET("v2/applications.json")
    suspend fun getApplications(
        @Query("filter[name]") filterName: String? = null,
        @Query("filter[ids]") filterIds: String? = null,
        @Query("filter[language]") filterLanguage: String? = null,
        @Query("page") page: Int? = null,
    ): Response<NewRelicApplicationsResponseDto>

    @GET("v2/applications/{id}.json")
    suspend fun getApplicationById(
        @Path("id") id: Long,
    ): Response<NewRelicApplicationResponseDto>

    // ── Metrics ─────────────────────────────────────────────────────────

    @GET("v2/applications/{id}/metrics.json")
    suspend fun getMetricNames(
        @Path("id") applicationId: Long,
        @Query("name") name: String? = null,
        @Query("page") page: Int? = null,
    ): Response<NewRelicMetricNamesResponseDto>

    @GET("v2/applications/{id}/metrics/data.json")
    suspend fun getMetricData(
        @Path("id") applicationId: Long,
        @Query("names[]") names: List<String>,
        @Query("values[]") values: List<String>? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("period") period: Int? = null,
        @Query("summarize") summarize: Boolean? = null,
        @Query("raw") raw: Boolean? = null,
    ): Response<NewRelicMetricDataResponseDto>

    // ── Alert Violations ────────────────────────────────────────────────

    @GET("v2/alerts_violations.json")
    suspend fun getAlertViolations(
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("only_open") onlyOpen: Boolean? = null,
        @Query("page") page: Int? = null,
    ): Response<NewRelicAlertViolationsResponseDto>
}
