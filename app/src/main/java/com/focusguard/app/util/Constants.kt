package com.focusguard.app.util

object Constants {
    const val APP_NAME = "专注卫士"

    // Timer threshold defaults
    const val DEFAULT_FIRST_REMINDER_MINUTES = 60L
    const val DEFAULT_LOCK_REMINDER_MINUTES = 90L
    const val ANTICHEAT_THRESHOLD_MS = 10_000L  // 10 seconds

    // Default monitoring period: 0:00-24:00 (fixed full day)
    const val DEFAULT_START_HOUR = 0
    const val DEFAULT_START_MINUTE = 0
    const val DEFAULT_END_HOUR = 24
    const val DEFAULT_END_MINUTE = 0

    // Notification IDs
    const val NOTIFICATION_ID_USAGE = 1001
    const val NOTIFICATION_ID_EXEMPTION = 1002

    // Broadcast actions
    const val ACTION_UPDATE_USAGE = "com.focusguard.app.UPDATE_USAGE"
    const val ACTION_FIRST_REMINDER = "com.focusguard.app.FIRST_REMINDER"
    const val ACTION_LOCK_REMINDER = "com.focusguard.app.LOCK_REMINDER"
    const val ACTION_TIMER_RESET = "com.focusguard.app.TIMER_RESET"
    const val ACTION_EXEMPTION_CHANGED = "com.focusguard.app.EXEMPTION_CHANGED"
    const val ACTION_CLOSE_REMINDERS = "com.focusguard.app.CLOSE_REMINDERS"

    // Exemption: Navigation app package names
    val NAVIGATION_APP_PACKAGES = setOf(
        "com.autonavi.minimap",           // 高德地图
        "com.baidu.BaiduMap",             // 百度地图
        "com.tencent.map",                // 腾讯地图
        "com.google.android.apps.maps",   // Google 地图
        "com.google.android.apps.navigation", // Google 导航
    )

    // SharedPreferences keys
    const val PREF_SETUP_COMPLETED = "setup_completed"
    const val PREF_MONITORING_START_HOUR = "monitoring_start_hour"
    const val PREF_MONITORING_START_MINUTE = "monitoring_start_minute"
    const val PREF_MONITORING_END_HOUR = "monitoring_end_hour"
    const val PREF_MONITORING_END_MINUTE = "monitoring_end_minute"
    const val PREF_MONITORING_ENABLED = "monitoring_enabled"
    const val PREF_SERVICE_RUNNING = "service_running"
    const val PREF_HIGH_PRECISION_NAV = "high_precision_nav"
    const val PREF_FIRST_REMINDER_MINUTES = "first_reminder_minutes"
    const val PREF_LOCK_REMINDER_MINUTES = "lock_reminder_minutes"
}
