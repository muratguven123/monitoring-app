package com.monitoring.dashboard.worker

import org.junit.Assert.assertEquals
import org.junit.Test

class AlertMonitorWorkerScheduleTest {

    @Test
    fun `normalizeIntervalMinutes coerces below WorkManager minimum to 15`() {
        assertEquals(15L, AlertMonitorWorker.normalizeIntervalMinutes(5))
        assertEquals(15L, AlertMonitorWorker.normalizeIntervalMinutes(14))
        assertEquals(15L, AlertMonitorWorker.normalizeIntervalMinutes(15))
    }

    @Test
    fun `normalizeIntervalMinutes keeps 30 and 60`() {
        assertEquals(30L, AlertMonitorWorker.normalizeIntervalMinutes(30))
        assertEquals(60L, AlertMonitorWorker.normalizeIntervalMinutes(60))
    }
}
