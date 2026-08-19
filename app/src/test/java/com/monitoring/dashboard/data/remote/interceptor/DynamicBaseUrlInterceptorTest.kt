package com.monitoring.dashboard.data.remote.interceptor

import com.monitoring.dashboard.data.remote.GrafanaBaseUrlProvider
import com.monitoring.dashboard.data.remote.GrafanaNotConfiguredException
import com.monitoring.dashboard.domain.model.GrafanaServerUrl
import com.monitoring.dashboard.domain.model.GrafanaUrlResult
import io.mockk.every
import io.mockk.mockk
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

/**
 * These tests exist because URL rewriting is easy to get subtly wrong in ways
 * that only show up against a particular deployment topology — a reverse-proxied
 * Grafana, or an absolute URL the app built itself.
 */
class DynamicBaseUrlInterceptorTest {

    private lateinit var baseUrlProvider: GrafanaBaseUrlProvider
    private lateinit var interceptor: DynamicBaseUrlInterceptor

    private val placeholder = GrafanaBaseUrlProvider.UNCONFIGURED_PLACEHOLDER_URL

    @Before
    fun setup() {
        baseUrlProvider = mockk()
        interceptor = DynamicBaseUrlInterceptor(baseUrlProvider)
    }

    private fun configureServer(raw: String) {
        val server = (GrafanaServerUrl.parse(raw) as GrafanaUrlResult.Valid).url
        every { baseUrlProvider.current() } returns server
    }

    /** Runs the interceptor and returns the URL the request would actually go to. */
    private fun resultingUrl(requestUrl: String): String {
        val request = Request.Builder().url(requestUrl).build()
        var seen: Request? = null

        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.proceed(any()) } answers {
            seen = firstArg<Request>()
            Response.Builder()
                .request(seen!!)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("".toResponseBody(null))
                .build()
        }

        interceptor.intercept(chain)
        return seen!!.url.toString()
    }

    // ── Origin rewriting ──────────────────────────────────────────────────

    @Test
    fun `placeholder host is replaced with the configured server`() {
        configureServer("https://grafana.example.com")

        assertEquals(
            "https://grafana.example.com/api/health",
            resultingUrl("${placeholder}api/health"),
        )
    }

    @Test
    fun `port is carried over`() {
        configureServer("http://grafana.example.com:3000")

        assertEquals(
            "http://grafana.example.com:3000/api/search",
            resultingUrl("${placeholder}api/search"),
        )
    }

    @Test
    fun `query parameters survive the rewrite`() {
        configureServer("https://grafana.example.com")

        assertEquals(
            "https://grafana.example.com/api/search?limit=50&type=dash-db",
            resultingUrl("${placeholder}api/search?limit=50&type=dash-db"),
        )
    }

    // ── Reverse-proxy sub-path ────────────────────────────────────────────

    @Test
    fun `sub path prefix is applied to api calls`() {
        configureServer("https://intranet.example.com/grafana")

        assertEquals(
            "https://intranet.example.com/grafana/api/health",
            resultingUrl("${placeholder}api/health"),
        )
    }

    @Test
    fun `nested sub path prefix is applied`() {
        configureServer("https://intranet.example.com/tools/grafana")

        assertEquals(
            "https://intranet.example.com/tools/grafana/api/datasources",
            resultingUrl("${placeholder}api/datasources"),
        )
    }

    // ── Idempotence: absolute URLs the app built itself ───────────────────

    @Test
    fun `absolute panel render url is left untouched`() {
        // Coil render URLs are built from the stored base URL and already carry
        // the origin and prefix. Prefixing again would give /grafana/grafana/...
        configureServer("https://intranet.example.com/grafana")

        val renderUrl =
            "https://intranet.example.com/grafana/render/d-solo/abc/cpu?panelId=4&from=now-3h"

        assertEquals(renderUrl, resultingUrl(renderUrl))
    }

    @Test
    fun `absolute url on a plain host is left untouched`() {
        configureServer("https://grafana.example.com")

        val renderUrl = "https://grafana.example.com/render/d-solo/abc/cpu?panelId=4"

        assertEquals(renderUrl, resultingUrl(renderUrl))
    }

    @Test
    fun `a request to an unrelated host is not redirected`() {
        // Defensive: the Grafana client must never silently hijack another host.
        configureServer("https://grafana.example.com")

        val other = "https://images.example.org/logo.png"

        assertEquals(other, resultingUrl(other))
    }

    // ── Unconfigured ──────────────────────────────────────────────────────

    @Test
    fun `an unconfigured server aborts the call instead of hitting the placeholder`() {
        every { baseUrlProvider.current() } returns null

        assertThrows(GrafanaNotConfiguredException::class.java) {
            resultingUrl("${placeholder}api/health")
        }
    }
}
