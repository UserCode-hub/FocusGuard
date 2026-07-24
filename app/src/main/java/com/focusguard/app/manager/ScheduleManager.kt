package com.focusguard.app.manager

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.focusguard.app.ui.FirstReminderActivity
import com.focusguard.app.ui.LockReminderActivity
import com.focusguard.app.util.Constants

/**
 * 监控时段管理器（v3.0 最终版）
 * 固定 0:00-24:00（全天），用户仅通过开关控制是否启用
 */
class ScheduleManager(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        context.getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE)

    /** 获取监控时段是否启用 */
    fun isEnabled(): Boolean {
        return prefs.getBoolean(Constants.PREF_MONITORING_ENABLED, true)
    }

    /** 设置监控时段启用状态 */
    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(Constants.PREF_MONITORING_ENABLED, enabled).apply()
        if (!enabled) {
            // 关闭监控时段 → 立即关闭所有提醒横幅，保留提醒标记不变
            val closeIntent = Intent(Constants.ACTION_CLOSE_REMINDERS).apply {
                addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            }
            appContext.sendBroadcast(closeIntent)
        }
    }

    /**
     * 检查当前是否在监控时段内
     * 固定 0:00-24:00（全天），仅受开关控制
     * isEnabled()=true → 全天都是监控时段
     * isEnabled()=false → 监控时段不生效
     */
    fun isInMonitoringPeriod(): Boolean {
        return isEnabled()
    }

    /** 获取格式化的时段字符串 */
    fun getPeriodString(): String {
        return "00:00 - 24:00"
    }

    /** 恢复默认设置并清空 */
    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }
}
