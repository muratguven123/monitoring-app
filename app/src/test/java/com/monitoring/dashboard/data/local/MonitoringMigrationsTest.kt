package com.monitoring.dashboard.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class MonitoringMigrationsTest {

    @Test
    fun migration1To2_hasExpectedVersions() {
        val migration = MonitoringMigrations.MIGRATION_1_2
        assertEquals(1, migration.startVersion)
        assertEquals(2, migration.endVersion)
    }
}
