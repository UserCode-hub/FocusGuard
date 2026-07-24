package com.focusguard.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager
import com.focusguard.app.service.ScreenMonitorService

/**
 * 屏幕状态广播接收器
 * 监听 SCREEN_ON / SCREEN_OFF / USER_PRESENT 事件
 * 同时也监听电话状态
 */
class ScreenStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_ON -> {
                // 检查是否在通话中
                if (isPhoneInCall(context)) return
                ScreenMonitorService.notifyScreenOn(context)
            }
            Intent.ACTION_SCREEN_OFF -> {
                ScreenMonitorService.notifyScreenOff(context)
            }
            Intent.ACTION_USER_PRESENT -> {
                if (isPhoneInCall(context)) return
                ScreenMonitorService.notifyUserPresent(context)
            }
            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                handlePhoneState(context, intent)
            }
        }
    }

    private fun handlePhoneState(context: Context, intent: Intent) {
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        when (state) {
            TelephonyManager.EXTRA_STATE_IDLE -> {
                // 通话结束
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK, TelephonyManager.EXTRA_STATE_RINGING -> {
                // 通话中或来电
            }
        }
    }

    private fun isPhoneInCall(context: Context): Boolean {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                !telephonyManager.callStateForPackage.isNullOrEmpty()
            } else {
                @Suppress("DEPRECATION")
                telephonyManager.callState != TelephonyManager.CALL_STATE_IDLE
            }
        } catch (e: Exception) {
            false
        }
    }
}
