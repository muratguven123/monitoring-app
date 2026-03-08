package com.monitoring.dashboard.domain.model

import com.monitoring.dashboard.data.remote.dto.GrafanaHealthDto

data class GrafanaHealth(
    val version: String,
    val database: String,
    val isHealthy: Boolean,
)

fun GrafanaHealthDto.toDomain() = GrafanaHealth(
    version = version ?: "Unknown",
    database = database ?: "unknown",
    isHealthy = database?.lowercase() == "ok",
)
