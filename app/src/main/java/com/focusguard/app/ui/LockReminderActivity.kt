package com.focusguard.app.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.focusguard.app.util.Constants
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.app.FocusGuardApp

/**
 * 锁屏提醒横幅（v3.0 最终版）
 * 顶部横幅 + 30 秒倒计时（CountDownTimer 作为唯一计时源）
 * 倒计时结束自动执行 lockNow()
 */
class LockReminderActivity : ComponentActivity() {

    companion object {
        const val EXTRA_THRESHOLD = "lock_threshold_minutes"
    }

    private var closeReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 注册关闭广播：用户关闭监控时段时立即关闭提醒
        closeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Constants.ACTION_CLOSE_REMINDERS) {
                    cancelCountdown()
                    finish()
                }
            }
        }
        registerReceiver(closeReceiver, IntentFilter(Constants.ACTION_CLOSE_REMINDERS))

        window.setGravity(Gravity.TOP)
        window.attributes = window.attributes.apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            y = 48
            dimAmount = 0f
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )

        setContent {
            LockReminderBanner()
        }

        startLockCountdown()
    }

    private fun startLockCountdown() {
        val app = applicationContext as FocusGuardApp
        countDownTimer = object : CountDownTimer(30000, 500) {  // 每 500ms 刷新 UI，更流畅
            override fun onTick(millisUntilFinished: Long) {
                _secondsRemaining.value = (millisUntilFinished / 1000).toInt()
            }
            override fun onFinish() {
                _secondsRemaining.value = 0
                app.lockManager.lockScreen()
                // 锁屏后通知 Service 归零计时器
                com.focusguard.app.service.ScreenMonitorService.resetTimer(this@LockReminderActivity)
                finish()
            }
        }.start()
    }

    private var countDownTimer: CountDownTimer? = null

    private fun cancelCountdown() {
        countDownTimer?.cancel()
        countDownTimer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelCountdown()
        closeReceiver?.let { unregisterReceiver(it) }
    }

    companion object {
        val _secondsRemaining = mutableIntStateOf(30)
    }
}

@Composable
fun LockReminderBanner() {
    val seconds = LockReminderActivity._secondsRemaining.intValue

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .background(
                color = Color(0xFFFFF3E0),
                shape = RoundedCornerShape(
                    bottomStart = 16.dp, bottomEnd = 16.dp,
                    topStart = 0.dp, topEnd = 0.dp
                )
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⏰", fontSize = 22.sp, modifier = Modifier.padding(end = 12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "亲爱的，你已经严重超时使用手机了呢",
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFBF360C), lineHeight = 18.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text("手机将在 ", fontSize = 13.sp, color = Color(0xFF666666))
                    // 倒计时圆圈 - 由 CountDownTimer 驱动
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFFD32F2F), shape = RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "$seconds",
                            fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White
                        )
                    }
                    Text(
                        " 秒后锁屏哦！",
                        fontSize = 13.sp, color = Color(0xFF666666),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            Text("现在", fontSize = 11.sp, color = Color(0xFF999999),
                modifier = Modifier.padding(start = 8.dp))
        }
    }
}
