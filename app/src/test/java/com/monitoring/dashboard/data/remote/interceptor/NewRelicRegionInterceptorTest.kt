package com.monitoring.dashboard.data.remote.interceptor

import com.monitoring.dashboard.data.local.SecurePreferencesManager
import com.monitoring.dashboard.domain.model.NewRelicRegion
import io.mockk.every
import io.mockk.mockk
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class NewRelicRegionInterceptorTest {

    private lateinit var prefs: SecurePreferencesManager
    private lateinit var interceptor: NewRelicRegionInterceptor

    @Before
    fun setup() {
        prefs = mockk()
        interceptor = NewRelicRegionInterceptor(prefs)
    }

    private fun resultingHost(requestUrl: String, region: NewRelicRegion): String {
        every { prefs.getNewRelicRegion() } returns region
        val request = Request.Builder().url(requestUrl).build()
        var seen: Request? = null
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.proceed(any()) } answers {
            seen = firstArg()
            Response.Builder()
                .request(seen!!)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("".toResponseBody(null))
                .build()
        }
        interceptor.intercept(chain)
        return seen!!.url.host
    }

    @Test
    fun `us host is rewritten to eu when EU is selected`() {
        assertEquals(
            "api.eu.newrelic.com",
            resultingHost("https://api.newrelic.com/v2/applications.json", NewRelicRegion.EU),
        )
    }

    @Test
    fun `nerdgraph path is kept when host changes`() {
        every { prefs.getNewRelicRegion() } returns NewRelicRegion.EU
        val request = Request.Builder().url("https://api.newrelic.com/graphql").build()
        var seen: Request? = null
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.proceed(any()) } answers {
            seen = firstArg()
            Response.Builder()
                .request(seen!!)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("".toResponseBody(null))
                .build()
        }
        interceptor.intercept(chain)
        assertEquals("https://api.eu.newrelic.com/graphql", seen!!.url.toString())
    }

    @Test
    fun `emulator mock host is not rewritten`() {
        assertEquals(
            "10.0.2.2",
            resultingHost("http://10.0.2.2:5000/v2/applications.json", NewRelicRegion.EU),
        )
    }
}
