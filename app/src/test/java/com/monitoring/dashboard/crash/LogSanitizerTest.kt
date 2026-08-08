package com.monitoring.dashboard.crash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LogSanitizerTest {

    @Test
    fun `redacts bearer tokens`() {
        val out = LogSanitizer.sanitize("Authorization Bearer abc.def.ghi")
        assertEquals("Authorization Bearer [REDACTED]", out)
    }

    @Test
    fun `redacts New Relic and GitHub token shapes`() {
        val out = LogSanitizer.sanitize("key=NRAK-abc123XYZ token=ghp_abcdefghijklmnop")
        assertFalse(out.contains("NRAK-abc"))
        assertFalse(out.contains("ghp_abc"))
        assert(out.contains("NRAK-[REDACTED]"))
        assert(out.contains("ghp_[REDACTED]"))
    }

    @Test
    fun `leaves benign messages unchanged`() {
        val msg = "HomeViewModel: auto-refresh triggered"
        assertEquals(msg, LogSanitizer.sanitize(msg))
    }
}
