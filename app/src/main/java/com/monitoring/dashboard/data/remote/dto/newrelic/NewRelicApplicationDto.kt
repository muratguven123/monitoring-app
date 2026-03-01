package com.monitoring.dashboard.data.remote.dto.newrelic

import com.google.gson.annotations.SerializedName

/**
 * New Relic REST API v2 `/v2/applications.json` response wrapper.
 */
data class NewRelicApplicationsResponseDto(
    @SerializedName("applications")
    val applications: List<NewRelicApplicationDto>,
)

/**
 * New Relic REST API v2 `/v2/applications/{id}.json` response wrapper.
 */
data class NewRelicApplicationResponseDto(
    @SerializedName("application")
    val application: NewRelicApplicationDto,
)

/**
 * Represents a single New Relic APM application.
 */
data class NewRelicApplicationDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("name")
    val name: String,

    @SerializedName("language")
    val language: String? = null,

    @SerializedName("health_status")
    val healthStatus: String? = null, // "green", "orange", "red", "gray"

    @SerializedName("reporting")
    val reporting: Boolean = false,

    @SerializedName("last_reported_at")
    val lastReportedAt: String? = null,

    @SerializedName("application_summary")
    val applicationSummary: ApplicationSummaryDto? = null,

    @SerializedName("end_user_summary")
    val endUserSummary: EndUserSummaryDto? = null,

    @SerializedName("settings")
    val settings: ApplicationSettingsDto? = null,

    @SerializedName("links")
    val links: ApplicationLinksDto? = null,
)

data class ApplicationSummaryDto(
    @SerializedName("response_time")
    val responseTime: Double? = null,

    @SerializedName("throughput")
    val throughput: Double? = null,

    @SerializedName("error_rate")
    val errorRate: Double? = null,

    @SerializedName("apdex_target")
    val apdexTarget: Double? = null,

    @SerializedName("apdex_score")
    val apdexScore: Double? = null,

    @SerializedName("host_count")
    val hostCount: Int? = null,

    @SerializedName("instance_count")
    val instanceCount: Int? = null,

    @SerializedName("concurrent_instance_count")
    val concurrentInstanceCount: Int? = null,
)

data class EndUserSummaryDto(
    @SerializedName("response_time")
    val responseTime: Double? = null,

    @SerializedName("throughput")
    val throughput: Double? = null,

    @SerializedName("apdex_target")
    val apdexTarget: Double? = null,

    @SerializedName("apdex_score")
    val apdexScore: Double? = null,
)

data class ApplicationSettingsDto(
    @SerializedName("app_apdex_threshold")
    val appApdexThreshold: Double? = null,

    @SerializedName("end_user_apdex_threshold")
    val endUserApdexThreshold: Double? = null,

    @SerializedName("enable_real_user_monitoring")
    val enableRealUserMonitoring: Boolean = false,

    @SerializedName("use_server_side_config")
    val useServerSideConfig: Boolean = false,
)

data class ApplicationLinksDto(
    @SerializedName("servers")
    val servers: List<Long> = emptyList(),

    @SerializedName("application_hosts")
    val applicationHosts: List<Long> = emptyList(),

    @SerializedName("application_instances")
    val applicationInstances: List<Long> = emptyList(),

    @SerializedName("alert_policy")
    val alertPolicy: Long? = null,
)
