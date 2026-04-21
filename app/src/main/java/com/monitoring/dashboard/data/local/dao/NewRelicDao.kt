package com.monitoring.dashboard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.monitoring.dashboard.data.local.entity.NewRelicAppEntity

@Dao
interface NewRelicDao {

    @Query("SELECT * FROM newrelic_apps ORDER BY name ASC")
    suspend fun getAll(): List<NewRelicAppEntity>

    @Query("SELECT * FROM newrelic_apps WHERE id = :id")
    suspend fun getById(id: Long): NewRelicAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<NewRelicAppEntity>)

    @Query("DELETE FROM newrelic_apps")
    suspend fun deleteAll()

    @Query("DELETE FROM newrelic_apps WHERE cachedAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}
