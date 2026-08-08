package com.monitoring.dashboard.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity that caches New Relic alert violation data for offline access,
 * history, and deduplication in [com.monitoring.dashboard.worker.AlertMonitorWorker].
 */
@Entity(tableName = "alert_violations")
data class AlertViolationEntity(
    @PrimaryKey
    val id: Long,
    val label: String?,
    val policyName: String?,
    val conditionName: String? = null,
    val openedAt: Long?,
    val severity: String?,
    val isOpen: Boolean = true,
    val resolvedAt: Long? = null,
    val cachedAt: Long = System.currentTimeMillis(),
)
