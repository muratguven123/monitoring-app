package com.monitoring.dashboard.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object MonitoringMigrations {
    /**
     * v1 → v2: alert history columns (conditionName, isOpen, resolvedAt).
     * Grafana / New Relic tables unchanged for practical purposes.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE alert_violations ADD COLUMN conditionName TEXT")
            db.execSQL(
                "ALTER TABLE alert_violations ADD COLUMN isOpen INTEGER NOT NULL DEFAULT 1",
            )
            db.execSQL("ALTER TABLE alert_violations ADD COLUMN resolvedAt INTEGER")
        }
    }
}
