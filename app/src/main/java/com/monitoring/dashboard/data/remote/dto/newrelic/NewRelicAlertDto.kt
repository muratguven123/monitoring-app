package com.monitoring.dashboard.data.remote.dto.newrelic

import com.google.gson.annotations.SerializedName

/**
 * New Relic `/v2/alerts_violations.json` response wrapper.
 */
data class NewRelicAlertViolationsResponseDto(
    @SerializedName("violations")
    val violations: List<AlertViolationDto>,
)

data class AlertViolationDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("label")
    val label: String? = null,

    @SerializedName("duration")
    val duration: Long? = null,

    @SerializedName("policy_name")
    val policyName: String? = null,

    @SerializedName("condition_name")
    val conditionName: String? = null,

    @SerializedName("priority")
    val priority: String? = null, // "critical", "warning"

    @SerializedName("opened_at")
    val openedAt: Long? = null,

    @SerializedName("closed_at")
    val closedAt: Long? = null,

    @SerializedName("entity")
    val entity: AlertEntityDto? = null,

    @SerializedName("links")
    val links: AlertLinksDto? = null,
)

data class AlertEntityDto(
    @SerializedName("product")
    val product: String? = null,

    @SerializedName("type")
    val type: String? = null,

    @SerializedName("group_id")
    val groupId: Long? = null,

    @SerializedName("name")
    val name: String? = null,
)

data class AlertLinksDto(
    @SerializedName("policy_id")
    val policyId: Long? = null,

    @SerializedName("condition_id")
    val conditionId: Long? = null,
)
