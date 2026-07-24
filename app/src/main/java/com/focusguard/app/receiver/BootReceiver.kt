package com.focusguard.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.focusguard.app.service.ScreenMonitorService

/**
 * 开机自启广播接收器
 * 手机重启后自动启动监控服务
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                // 检查用户是否已完成初始设置
                val prefs = context.getSharedPreferences("setup_prefs", Context.MODE_PRIVATE)
                val setupCompleted = prefs.getBoolean("setup_completed", false)

                if (setupCompleted) {
                    // 启动监控服务
                    ScreenMonitorService.start(context)
                }
            }
        }
    }
}
