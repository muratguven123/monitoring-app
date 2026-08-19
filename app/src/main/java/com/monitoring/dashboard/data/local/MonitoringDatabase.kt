package com.monitoring.dashboard.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.monitoring.dashboard.data.local.dao.AlertDao
import com.monitoring.dashboard.data.local.dao.GrafanaDao
import com.monitoring.dashboard.data.local.dao.NewRelicDao
import com.monitoring.dashboard.data.local.entity.AlertViolationEntity
import com.monitoring.dashboard.data.local.entity.GrafanaDashboardEntity
import com.monitoring.dashboard.data.local.entity.NewRelicAppEntity

@Database(
    entities = [
        GrafanaDashboardEntity::class,
        NewRelicAppEntity::class,
        AlertViolationEntity::class,
    ],
    // Schema version 1 — the app has never shipped, so there is no installed
    // base to migrate from. Every future schema change must bump this number
    // AND add a Migration in DatabaseModule; never use destructive fallback,
    // it would silently wipe cached alert history on upgrade.
    version = 1,
    exportSchema = true,
)
abstract class MonitoringDatabase : RoomDatabase() {
    abstract fun grafanaDao(): GrafanaDao
    abstract fun newRelicDao(): NewRelicDao
    abstract fun alertDao(): AlertDao
}
