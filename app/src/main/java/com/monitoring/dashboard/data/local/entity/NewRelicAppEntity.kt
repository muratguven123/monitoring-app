package com.monitoring.dashboard.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity that caches New Relic APM application data for offline access.
 */
@Entity(tableName = "newrelic_apps")
data class NewRelicAppEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val language: String?,
    val healthStatus: String?,
    val reporting: Boolean,
    val cachedAt: Long = System.currentTimeMillis(),
)
