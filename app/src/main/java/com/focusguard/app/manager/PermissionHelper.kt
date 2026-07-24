package com.focusguard.app.manager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

class PermissionHelper(private val context: Context) {

    fun isDeviceAdminGranted(): Boolean = LockManager(context).isAdminActive()

    fun isNotificationGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else true
    }

    fun isBatteryOptimizationExempted(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun getBatteryOptimizationIntent(): Intent {
        return Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}")
        )
    }

    fun getAutoStartIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    fun getNotificationSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    }

    /** 获取设置中 Usage Stats 权限的 Intent */
    fun getUsageStatsIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    fun getDeviceAdminSettingsIntent(): Intent {
        return Intent(Settings.ACTION_SECURITY_SETTINGS)
    }

    fun getMissingPermissions(): List<String> {
        val missing = mutableListOf<String>()
        if (!isDeviceAdminGranted()) missing.add("device_admin")
        if (!isBatteryOptimizationExempted()) missing.add("battery")
        return missing
    }

    fun isHighPrecisionNavEnabled(): Boolean {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        return prefs.getBoolean("high_precision_nav", false)
    }

    fun setHighPrecisionNav(enabled: Boolean) {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("high_precision_nav", enabled)
            .apply()
    }

    fun getRomGuideText(): String {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("huawei") || manufacturer.contains("honor") ->
                """华为 EMUI / 荣耀 MagicOS 引导路径：

                |设置 → 电池 → 应用启动管理
                |找到"专注卫士"，关闭"自动管理"，设为"手动"管理
                |→ 开启"自启动" + "允许后台活动"""".trimMargin()
            manufacturer.contains("vivo") || manufacturer.contains("iqoo") ->
                """vivo OriginOS 引导路径：

                |设置 → 电池 → 后台耗电管理
                |找到"专注卫士"，设为"允许后台运行"""".trimMargin()
            else ->
                """通用引导路径：

                |设置 → 应用 → 应用管理 → 专注卫士
                |→ 开启"自启动"权限
                |设置 → 电池 → 忽略电池优化
                |→ 找到"专注卫士"，设为"允许"""".trimMargin()
        }
    }

    fun isFirstLaunch(): Boolean {
        val prefs = context.getSharedPreferences("setup_prefs", Context.MODE_PRIVATE)
        return !prefs.getBoolean("setup_completed", false)
    }

    fun markSetupCompleted() {
        context.getSharedPreferences("setup_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("setup_completed", true)
            .apply()
    }
}
