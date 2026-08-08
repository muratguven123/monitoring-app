package com.monitoring.dashboard.crash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportingTest {

    @Test
    fun `install routes log and record to sink`() {
        val logs = mutableListOf<String>()
        val errors = mutableListOf<Throwable>()
        CrashReporting.install(
            object : CrashReporting.Sink {
                override fun log(message: String) {
                    logs += message
                }

                override fun record(throwable: Throwable) {
                    errors += throwable
                }
            },
        )

        CrashReporting.log("hello")
        val boom = IllegalStateException("boom")
        CrashReporting.record(boom)

        assertEquals(listOf("hello"), logs)
        assertEquals(1, errors.size)
        assertTrue(errors[0] === boom)

        // Reset to no-op so other tests are unaffected
        CrashReporting.install(
            object : CrashReporting.Sink {
                override fun log(message: String) = Unit
                override fun record(throwable: Throwable) = Unit
            },
        )
    }
}
