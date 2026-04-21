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
    version = 1,
    exportSchema = true,
)
abstract class MonitoringDatabase : RoomDatabase() {
    abstract fun grafanaDao(): GrafanaDao
    abstract fun newRelicDao(): NewRelicDao
    abstract fun alertDao(): AlertDao
}
