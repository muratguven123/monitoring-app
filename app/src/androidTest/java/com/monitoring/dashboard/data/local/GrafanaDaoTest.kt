package com.monitoring.dashboard.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.monitoring.dashboard.data.local.dao.GrafanaDao
import com.monitoring.dashboard.data.local.entity.GrafanaDashboardEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GrafanaDaoTest {

    private lateinit var database: MonitoringDatabase
    private lateinit var dao: GrafanaDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MonitoringDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.grafanaDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun entity(id: Long = 1L, title: String = "Dashboard", cachedAt: Long = System.currentTimeMillis()) =
        GrafanaDashboardEntity(
            id = id,
            uid = "uid-$id",
            title = title,
            tags = "tag1,tag2",
            url = "/d/uid-$id/$title",
            folderTitle = "General",
            cachedAt = cachedAt,
        )

    @Test
    fun insertAll_and_getAll_roundTrip() = runTest {
        val items = listOf(entity(1, "A"), entity(2, "B"))
        dao.insertAll(items)

        val result = dao.getAll()
        assertEquals(2, result.size)
    }

    @Test
    fun getById_returnsCorrectEntity() = runTest {
        dao.insertAll(listOf(entity(42, "Target")))

        val found = dao.getById(42)
        assertNotNull(found)
        assertEquals("Target", found!!.title)
    }

    @Test
    fun getById_returnsNullForMissingId() = runTest {
        val found = dao.getById(999)
        assertNull(found)
    }

    @Test
    fun deleteAll_clearsTable() = runTest {
        dao.insertAll(listOf(entity(1), entity(2)))
        dao.deleteAll()

        assertTrue(dao.getAll().isEmpty())
    }

    @Test
    fun deleteOlderThan_removesStaleEntries() = runTest {
        val now = System.currentTimeMillis()
        dao.insertAll(
            listOf(
                entity(1, "Old", cachedAt = now - 10_000),
                entity(2, "Fresh", cachedAt = now),
            ),
        )

        dao.deleteOlderThan(now - 5_000)

        val remaining = dao.getAll()
        assertEquals(1, remaining.size)
        assertEquals("Fresh", remaining[0].title)
    }

    @Test
    fun insertAll_replacesOnConflict() = runTest {
        dao.insertAll(listOf(entity(1, "Original")))
        dao.insertAll(listOf(entity(1, "Updated")))

        val result = dao.getAll()
        assertEquals(1, result.size)
        assertEquals("Updated", result[0].title)
    }
}
