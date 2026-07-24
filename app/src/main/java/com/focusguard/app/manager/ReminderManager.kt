package com.focusguard.app.manager

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.focusguard.app.ui.FirstReminderActivity
import com.focusguard.app.ui.LockReminderActivity

/**
 * 分级提醒管理器（v3.0 最终版）
 * - 首次提醒：连续使用 60 分钟，顶部横幅，30 秒后自动消失
 * - 锁屏提醒：连续使用 90 分钟，顶部横幅 + 30 秒倒计时，结束后 lockNow()
 */
class ReminderManager(private val context: Context) {

    /** 触发首次提醒：顶部横幅，30秒后自动消失 */
    fun triggerFirstReminder(timerManager: TimerManager, thresholdMinutes: Int = 60, elapsedMinutes: Int = 60) {
        timerManager.markFirstReminderTriggered()
        vibratePhone()
        val intent = Intent(context, FirstReminderActivity::class.java).apply {
            putExtra(FirstReminderActivity.EXTRA_ELAPSED, elapsedMinutes)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(intent)
    }

    /** 触发锁屏提醒：顶部横幅 + 30 秒倒计时 → lockNow() */
    fun triggerLockReminder(thresholdMinutes: Int = 90) {
        vibratePhoneStrong()
        val intent = Intent(context, LockReminderActivity::class.java).apply {
            putExtra(LockReminderActivity.EXTRA_THRESHOLD, thresholdMinutes)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(intent)
    }

    private fun vibratePhone() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(500)
            }
        } catch (e: Exception) { }
    }

    private fun vibratePhoneStrong() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = VibrationEffect.createWaveform(
                    longArrayOf(0, 300, 200, 300, 200, 500),
                    intArrayOf(0, 255, 0, 255, 0, 255), -1
                )
                vibrator.vibrate(pattern)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 300, 200, 300, 200, 500), -1)
            }
        } catch (e: Exception) { }
    }
}
