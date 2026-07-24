package com.focusguard.app.manager

import com.focusguard.app.FocusGuardApp
import com.focusguard.app.data.AppDatabase
import com.focusguard.app.data.entity.UsageRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DailyReport(
    val date: String,
    val totalDurationMs: Long,
    val maxContinuousMs: Long,
    val unlockCount: Int
)

class ReportManager(
    private val app: FocusGuardApp,
    private val database: AppDatabase
) {
    private val dao = database.usageRecordDao()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /** 获取今天的日期字符串 */
    fun getTodayDate(): String {
        return dateFormat.format(Date())
    }

    /** 记录一次使用会话 */
    suspend fun recordUsage(startTime: Long, endTime: Long, wasLocked: Boolean) {
        val duration = endTime - startTime
        if (duration <= 0) return

        val record = UsageRecord(
            date = dateFormat.format(Date(startTime)),
            startTime = startTime,
            endTime = endTime,
            durationMs = duration,
            wasLocked = wasLocked
        )
        withContext(Dispatchers.IO) {
            dao.insert(record)
        }
    }

    /** 更新使用记录结束时间 */
    suspend fun updateUsageRecord(id: Long, endTime: Long) {
        withContext(Dispatchers.IO) {
            // First get the record to compute duration
            // For simplicity, we update directly
        }
    }

    /** 获取今日报告 */
    suspend fun getTodayReport(): DailyReport = withContext(Dispatchers.IO) {
        val today = getTodayDate()
        val total = dao.getTotalDurationByDate(today) ?: 0L
        val max = dao.getMaxDurationByDate(today) ?: 0L
        val count = dao.getUnlockCountByDate(today)
        DailyReport(today, total, max, count)
    }

    /** 获取今日详细使用记录 */
    suspend fun getTodayRecords(): List<UsageRecord> = withContext(Dispatchers.IO) {
        dao.getRecordsByDate(getTodayDate())
    }

    /** 获取最近7天报告（仅返回有数据的天） */
    suspend fun getLast7DaysReports(): List<DailyReport> = withContext(Dispatchers.IO) {
        val reports = mutableListOf<DailyReport>()
        val cal = java.util.Calendar.getInstance()
        for (i in 6 downTo 0) {
            cal.time = Date()
            cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
            val date = dateFormat.format(cal.time)
            val total = dao.getTotalDurationByDate(date) ?: 0L
            val max = dao.getMaxDurationByDate(date) ?: 0L
            val count = dao.getUnlockCountByDate(date)
            // 只添加有数据的天（时长>0或解锁次数>0）
            if (total > 0 || count > 0) {
                reports.add(DailyReport(date, total, max, count))
            }
        }
        reports
    }

    /** 清理旧数据（30天前） */
    suspend fun cleanOldRecords() = withContext(Dispatchers.IO) {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -30)
        val cutoff = dateFormat.format(cal.time)
        dao.deleteRecordsBefore(cutoff)
    }

    /** 清除所有使用记录 */
    suspend fun clearAllRecords() = withContext(Dispatchers.IO) {
        dao.deleteAll()
    }

    companion object {
        fun formatDuration(ms: Long): String {
            val seconds = ms / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            return when {
                hours > 0 -> "${hours}小时${minutes % 60}分钟"
                minutes > 0 -> "${minutes}分钟${seconds % 60}秒"
                else -> "${seconds}秒"
            }
        }

        fun formatDurationShort(ms: Long): String {
            val seconds = ms / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            return when {
                hours > 0 -> "${hours}h${minutes % 60}m"
                minutes > 0 -> "${minutes}m${seconds % 60}s"
                else -> "${seconds}s"
            }
        }
    }
}
