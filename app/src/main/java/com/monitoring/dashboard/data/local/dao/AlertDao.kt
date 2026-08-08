package com.monitoring.dashboard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.monitoring.dashboard.data.local.entity.AlertViolationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {

    @Query("SELECT * FROM alert_violations ORDER BY openedAt DESC")
    suspend fun getAll(): List<AlertViolationEntity>

    @Query("SELECT * FROM alert_violations ORDER BY openedAt DESC")
    fun observeAll(): Flow<List<AlertViolationEntity>>

    @Query("SELECT * FROM alert_violations WHERE isOpen = 1 ORDER BY openedAt DESC")
    suspend fun getOpen(): List<AlertViolationEntity>

    @Query("SELECT * FROM alert_violations WHERE isOpen = 1 ORDER BY openedAt DESC")
    fun observeOpen(): Flow<List<AlertViolationEntity>>

    @Query("SELECT * FROM alert_violations WHERE id = :id")
    suspend fun getById(id: Long): AlertViolationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(violations: List<AlertViolationEntity>)

    @Query("UPDATE alert_violations SET isOpen = 0, resolvedAt = :resolvedAt WHERE id IN (:ids)")
    suspend fun markResolved(ids: List<Long>, resolvedAt: Long)

    @Query("DELETE FROM alert_violations")
    suspend fun deleteAll()

    @Query("DELETE FROM alert_violations WHERE cachedAt < :timestamp AND isOpen = 0")
    suspend fun deleteOlderThan(timestamp: Long)
}
