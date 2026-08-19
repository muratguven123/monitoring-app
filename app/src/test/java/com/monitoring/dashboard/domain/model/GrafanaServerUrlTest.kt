package com.monitoring.dashboard.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The app is pointed at whatever Grafana a given deployment runs, so parsing has
 * to cope with the range of things an operator will realistically type.
 */
class GrafanaServerUrlTest {

    private fun parsed(raw: String): GrafanaServerUrl =
        (GrafanaServerUrl.parse(raw) as GrafanaUrlResult.Valid).url

    // ── Accepted forms ────────────────────────────────────────────────────

    @Test
    fun `bare host defaults to https`() {
        assertEquals("https://grafana.example.com/", parsed("grafana.example.com").baseUrl)
    }

    @Test
    fun `explicit https is preserved`() {
        assertEquals("https://grafana.example.com/", parsed("https://grafana.example.com").baseUrl)
    }

    @Test
    fun `explicit http is preserved`() {
        val url = parsed("http://grafana.example.com")
        assertEquals("http://grafana.example.com/", url.baseUrl)
        assertTrue(url.isCleartext)
    }

    @Test
    fun `non default port is kept`() {
        assertEquals("https://grafana.example.com:3000/", parsed("grafana.example.com:3000").baseUrl)
    }

    @Test
    fun `default ports are omitted from the canonical form`() {
        assertEquals("https://grafana.example.com/", parsed("https://grafana.example.com:443").baseUrl)
        assertEquals("http://grafana.example.com/", parsed("http://grafana.example.com:80").baseUrl)
    }

    @Test
    fun `single label intranet host is accepted`() {
        assertEquals("https://grafana/", parsed("grafana").baseUrl)
    }

    @Test
    fun `ip address with port is accepted`() {
        assertEquals("http://10.0.2.2:3000/", parsed("http://10.0.2.2:3000").baseUrl)
    }

    // ── Reverse-proxy sub-paths ───────────────────────────────────────────
    // Corporate Grafana is often mounted under a path on a shared host. Losing
    // the prefix sends /api/health to the proxy root and 404s.

    @Test
    fun `sub path is preserved`() {
        val url = parsed("https://intranet.example.com/grafana")
        assertEquals("/grafana", url.pathPrefix)
        assertEquals("https://intranet.example.com/grafana/", url.baseUrl)
    }

    @Test
    fun `trailing slash on sub path is normalised away`() {
        assertEquals("/grafana", parsed("https://intranet.example.com/grafana/").pathPrefix)
    }

    @Test
    fun `nested sub path is preserved`() {
        assertEquals("/tools/grafana", parsed("https://example.com/tools/grafana").pathPrefix)
    }

    @Test
    fun `root path yields an empty prefix`() {
        assertEquals("", parsed("https://grafana.example.com/").pathPrefix)
    }

    // ── Normalisation ─────────────────────────────────────────────────────

    @Test
    fun `surrounding whitespace is trimmed`() {
        assertEquals("https://grafana.example.com/", parsed("  grafana.example.com  ").baseUrl)
    }

    @Test
    fun `scheme and host are lowercased`() {
        assertEquals("https://grafana.example.com/", parsed("HTTPS://Grafana.Example.COM").baseUrl)
    }

    @Test
    fun `query string and fragment are dropped`() {
        // Users paste dashboard links; only the origin and prefix are useful.
        assertEquals("https://grafana.example.com/", parsed("https://grafana.example.com/?orgId=1").baseUrl)
    }

    // ── Rejected / empty ──────────────────────────────────────────────────

    @Test
    fun `empty input is not configured rather than invalid`() {
        assertEquals(GrafanaUrlResult.NotConfigured, GrafanaServerUrl.parse(""))
        assertEquals(GrafanaUrlResult.NotConfigured, GrafanaServerUrl.parse("   "))
        assertEquals(GrafanaUrlResult.NotConfigured, GrafanaServerUrl.parse(null))
    }

    @Test
    fun `unsupported scheme is rejected`() {
        val result = GrafanaServerUrl.parse("ftp://grafana.example.com")
        assertEquals(GrafanaUrlResult.Invalid(GrafanaUrlError.UNSUPPORTED_SCHEME), result)
    }

    @Test
    fun `custom scheme is rejected`() {
        assertEquals(
            GrafanaUrlResult.Invalid(GrafanaUrlError.UNSUPPORTED_SCHEME),
            GrafanaServerUrl.parse("grafana://open"),
        )
    }

    @Test
    fun `missing host is rejected`() {
        assertTrue(GrafanaServerUrl.parse("https://") is GrafanaUrlResult.Invalid)
    }

    @Test
    fun `urlOrNull returns null for anything unusable`() {
        assertEquals(null, GrafanaServerUrl.parse("").urlOrNull())
        assertEquals(null, GrafanaServerUrl.parse("ftp://x.com").urlOrNull())
    }

    @Test
    fun `https on default port is not cleartext`() {
        assertFalse(parsed("https://grafana.example.com").isCleartext)
    }
}
