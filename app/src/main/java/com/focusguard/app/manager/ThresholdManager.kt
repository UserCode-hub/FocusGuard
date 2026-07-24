package com.focusguard.app.manager

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.focusguard.app.service.ScreenMonitorService
import com.focusguard.app.util.Constants

/**
 * 提醒阈值管理器
 * 用户可自定义首次提醒时间和锁屏提醒时间（分钟）
 */
class ThresholdManager(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        context.getSharedPreferences("threshold_prefs", Context.MODE_PRIVATE)

    /** 获取首次提醒时间（分钟），默认 60 */
    fun getFirstReminderMinutes(): Long {
        return prefs.getLong(
            Constants.PREF_FIRST_REMINDER_MINUTES,
            Constants.DEFAULT_FIRST_REMINDER_MINUTES
        )
    }

    /** 获取锁屏提醒时间（分钟），默认 90 */
    fun getLockReminderMinutes(): Long {
        return prefs.getLong(
            Constants.PREF_LOCK_REMINDER_MINUTES,
            Constants.DEFAULT_LOCK_REMINDER_MINUTES
        )
    }

    /** 设置首次提醒时间（分钟），不超过 360 — 重置提醒标记 */
    fun setFirstReminderMinutes(minutes: Long) {
        val clamped = minutes.coerceIn(1, 360)
        prefs.edit().putLong(Constants.PREF_FIRST_REMINDER_MINUTES, clamped).apply()
        ScreenMonitorService.resetReminderFlag(appContext)
    }

    /** 设置锁屏提醒时间（分钟），不超过 180 — 重置提醒标记 */
    fun setLockReminderMinutes(minutes: Long) {
        val clamped = minutes.coerceIn(1, 180)
        prefs.edit().putLong(Constants.PREF_LOCK_REMINDER_MINUTES, clamped).apply()
        ScreenMonitorService.resetReminderFlag(appContext)
    }

    /** 恢复默认值并清空所有设置 */
    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }
}
