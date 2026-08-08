package com.monitoring.dashboard.data.local

import com.monitoring.dashboard.data.local.dao.AlertDao
import com.monitoring.dashboard.data.local.dao.GrafanaDao
import com.monitoring.dashboard.data.local.dao.NewRelicDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CacheInvalidatorTest {

    private lateinit var grafanaDao: GrafanaDao
    private lateinit var newRelicDao: NewRelicDao
    private lateinit var alertDao: AlertDao
    private lateinit var cacheInvalidator: CacheInvalidator

    @Before
    fun setup() {
        grafanaDao = mockk()
        newRelicDao = mockk()
        alertDao = mockk()
        coEvery { grafanaDao.deleteAll() } just runs
        coEvery { newRelicDao.deleteAll() } just runs
        coEvery { alertDao.deleteAll() } just runs
        cacheInvalidator = CacheInvalidator(
            grafanaDao = grafanaDao,
            newRelicDao = newRelicDao,
            alertDao = alertDao,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun `clearAll deletes every Room cache table`() = runTest {
        cacheInvalidator.clearAll()

        coVerify(exactly = 1) { grafanaDao.deleteAll() }
        coVerify(exactly = 1) { newRelicDao.deleteAll() }
        coVerify(exactly = 1) { alertDao.deleteAll() }
    }
}
