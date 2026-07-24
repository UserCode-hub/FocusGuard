package com.focusguard.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.focusguard.app.data.entity.UsageRecord

@Dao
interface UsageRecordDao {

    @Insert
    suspend fun insert(record: UsageRecord)

    @Query("SELECT * FROM usage_records WHERE date = :date ORDER BY start_time DESC")
    suspend fun getRecordsByDate(date: String): List<UsageRecord>

    @Query("SELECT SUM(duration_ms) FROM usage_records WHERE date = :date")
    suspend fun getTotalDurationByDate(date: String): Long?

    @Query("SELECT MAX(duration_ms) FROM usage_records WHERE date = :date")
    suspend fun getMaxDurationByDate(date: String): Long?

    @Query("SELECT COUNT(*) FROM usage_records WHERE date = :date")
    suspend fun getUnlockCountByDate(date: String): Int

    @Query("SELECT * FROM usage_records WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC, start_time ASC")
    suspend fun getRecordsBetweenDates(startDate: String, endDate: String): List<UsageRecord>

    @Query("UPDATE usage_records SET end_time = :endTime, duration_ms = :durationMs WHERE id = :id")
    suspend fun updateEndTime(id: Long, endTime: Long, durationMs: Long)

    @Query("DELETE FROM usage_records WHERE date < :date")
    suspend fun deleteRecordsBefore(date: String)

    @Query("DELETE FROM usage_records")
    suspend fun deleteAll()
}
