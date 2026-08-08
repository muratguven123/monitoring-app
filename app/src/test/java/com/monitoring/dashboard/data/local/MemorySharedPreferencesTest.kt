package com.monitoring.dashboard.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MemorySharedPreferencesTest {

    @Test
    fun `stores values only in memory`() {
        val prefs = MemorySharedPreferences()
        prefs.edit().putString("grafana_api_key", "secret").putBoolean("flag", true).apply()

        assertEquals("secret", prefs.getString("grafana_api_key", null))
        assertTrue(prefs.getBoolean("flag", false))
    }

    @Test
    fun `clear removes all keys`() {
        val prefs = MemorySharedPreferences()
        prefs.edit().putString("a", "1").apply()
        prefs.edit().clear().apply()
        assertNull(prefs.getString("a", null))
        assertFalse(prefs.contains("a"))
    }
}
