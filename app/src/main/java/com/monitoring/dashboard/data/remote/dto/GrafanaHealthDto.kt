package com.monitoring.dashboard.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Grafana `/api/health` endpoint response.
 * Used to verify connectivity and Grafana instance status.
 */
data class GrafanaHealthDto(
    @SerializedName("commit")
    val commit: String? = null,

    @SerializedName("database")
    val database: String? = null, // "ok" or "failing"

    @SerializedName("version")
    val version: String? = null,
)

