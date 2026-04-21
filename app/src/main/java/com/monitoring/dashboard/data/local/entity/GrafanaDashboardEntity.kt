package com.monitoring.dashboard.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity that caches Grafana dashboard search results for offline access.
 */
@Entity(tableName = "grafana_dashboards")
data class GrafanaDashboardEntity(
    @PrimaryKey
    val id: Long,
    val uid: String,
    val title: String,
    val tags: String, // comma-separated
    val url: String,
    val folderTitle: String?,
    val cachedAt: Long = System.currentTimeMillis(),
)
