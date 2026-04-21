package com.monitoring.dashboard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.monitoring.dashboard.data.local.entity.GrafanaDashboardEntity

@Dao
interface GrafanaDao {

    @Query("SELECT * FROM grafana_dashboards ORDER BY title ASC")
    suspend fun getAll(): List<GrafanaDashboardEntity>

    @Query("SELECT * FROM grafana_dashboards WHERE id = :id")
    suspend fun getById(id: Long): GrafanaDashboardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dashboards: List<GrafanaDashboardEntity>)

    @Query("DELETE FROM grafana_dashboards")
    suspend fun deleteAll()

    @Query("DELETE FROM grafana_dashboards WHERE cachedAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}
