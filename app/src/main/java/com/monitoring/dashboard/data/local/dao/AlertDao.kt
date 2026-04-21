package com.monitoring.dashboard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.monitoring.dashboard.data.local.entity.AlertViolationEntity

@Dao
interface AlertDao {

    @Query("SELECT * FROM alert_violations ORDER BY openedAt DESC")
    suspend fun getAll(): List<AlertViolationEntity>

    @Query("SELECT * FROM alert_violations WHERE id = :id")
    suspend fun getById(id: Long): AlertViolationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(violations: List<AlertViolationEntity>)

    @Query("DELETE FROM alert_violations")
    suspend fun deleteAll()

    @Query("DELETE FROM alert_violations WHERE cachedAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}
