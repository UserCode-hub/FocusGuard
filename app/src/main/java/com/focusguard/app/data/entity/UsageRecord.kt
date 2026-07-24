package com.focusguard.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_records")
data class UsageRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 日期，格式 yyyy-MM-dd */
    @ColumnInfo(name = "date")
    val date: String,

    /** 本次使用开始时间戳 */
    @ColumnInfo(name = "start_time")
    val startTime: Long,

    /** 本次使用结束时间戳 */
    @ColumnInfo(name = "end_time")
    val endTime: Long = 0,

    /** 本次使用时长（毫秒） */
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long,

    /** 是否被强制锁定 */
    @ColumnInfo(name = "was_locked")
    val wasLocked: Boolean = false
)
