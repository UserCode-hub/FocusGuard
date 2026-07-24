package com.focusguard.app.receiver

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.app.usage.UsageStatsManager
import com.focusguard.app.FocusGuardApp
import com.focusguard.app.util.Constants

/**
 * 导航/车载状态广播接收器
 * 检测两种方式判断是否在开车导航中：
 *   B方案：车载蓝牙连接（默认）
 *   A方案：导航 App 前台运行（高精度模式，需 usage-stats 权限）
 */
class NavStateReceiver : BroadcastReceiver() {

    companion object {
        private const val PREF_NAME = "nav_prefs"
        private const val PREF_IS_NAVIGATING = "is_navigating"

        /** 获取车载蓝牙设备名称关键字 */
        private val CAR_BLUETOOTH_KEYWORDS = setOf(
            "car", "auto", "车载", "汽车", "蓝牙音箱", "蓝牙耳机",
            "driving", "vehicle", "ford", "toyota", "honda", "bmw",
            "audi", "benz", "volkswagen", "nissan", "hyundai", "volvo"
        )

        /** 检查给定蓝牙设备是否为车载设备 */
        fun isCarBluetoothDevice(device: BluetoothDevice?): Boolean {
            if (device == null) return false
            val name = device.name ?: return false
            return CAR_BLUETOOTH_KEYWORDS.any { keyword ->
                name.contains(keyword, ignoreCase = true)
            }
        }

        /** 检查前台是否存在导航应用（方案 A） */
        fun isNavAppForeground(context: Context): Boolean {
            try {
                val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val currentTime = System.currentTimeMillis()
                val stats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    currentTime - 2000,
                    currentTime
                )

                if (stats.isNullOrEmpty()) return false

                // 获取最近使用的 App
                val lastUsed = stats.maxByOrNull { it.lastTimeUsed } ?: return false
                val foregroundPackage = lastUsed.packageName ?: return false

                return foregroundPackage in Constants.NAVIGATION_APP_PACKAGES
            } catch (e: SecurityException) {
                // 没有 usage-stats 权限
                return false
            } catch (e: Exception) {
                return false
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                handleBluetoothConnected(context, intent)
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                handleBluetoothDisconnected(context, intent)
            }
            // 也可以监听前台应用变化（定时轮询，由 Service 端处理）
        }
    }

    private fun handleBluetoothConnected(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            if (isCarBluetoothDevice(device)) {
                setNavigating(context, true)
            }
        } else {
            @Suppress("DEPRECATION")
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            if (isCarBluetoothDevice(device)) {
                setNavigating(context, true)
            }
        }
    }

    private fun handleBluetoothDisconnected(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            if (isCarBluetoothDevice(device)) {
                setNavigating(context, false)
            }
        } else {
            @Suppress("DEPRECATION")
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            if (isCarBluetoothDevice(device)) {
                setNavigating(context, false)
            }
        }
    }

    /** 通过前台 App 检测（方案 A），由 Service 定时调用 */
    fun checkForegroundNavApp(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val highPrecisionMode = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getBoolean(Constants.PREF_HIGH_PRECISION_NAV, false)

        if (!highPrecisionMode) return

        val isNav = isNavAppForeground(context)
        val wasNavigating = prefs.getBoolean(PREF_IS_NAVIGATING, false)

        if (isNav != wasNavigating) {
            setNavigatingInternal(context, isNav)
        }
    }

    private fun setNavigating(context: Context, isNavigating: Boolean) {
        val app = context.applicationContext as FocusGuardApp
        setNavigatingInternal(context, isNavigating)

        // 通知豁免管理器
        val exempted = app.exemptionManager.isNavigating
        if (exempted != isNavigating) {
            app.exemptionManager.updateNavigationState(isNavigating)
        }
    }

    private fun setNavigatingInternal(context: Context, isNavigating: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PREF_IS_NAVIGATING, isNavigating)
            .apply()
    }
}
