package com.focusguard.app.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 首次提醒横幅（v3.0 最终版）
 * 顶部横幅显示自定义阈值时间，30 秒后自动消失
 */
class FirstReminderActivity : ComponentActivity() {

    companion object {
        const val EXTRA_THRESHOLD = "first_threshold_minutes"
        const val EXTRA_ELAPSED = "elapsed_minutes"
    }

    private var closeReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 注册关闭广播：用户关闭监控时段时立即关闭提醒
        closeReceiver = registerReceiver(null, IntentFilter(Constants.ACTION_CLOSE_REMINDERS)).let {
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == Constants.ACTION_CLOSE_REMINDERS) {
                        finish()
                    }
                }
            }
        }
        registerReceiver(closeReceiver, IntentFilter(Constants.ACTION_CLOSE_REMINDERS))

        val elapsedMinutes = intent?.getIntExtra(EXTRA_ELAPSED, 60) ?: 60

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
            MaterialTheme {
                FirstReminderBanner(elapsedMinutes)
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({ finish() }, 30000)
    }

    override fun onDestroy() {
        super.onDestroy()
        closeReceiver?.let { unregisterReceiver(it) }
    }
}

@Composable
fun FirstReminderBanner(elapsedMinutes: Int) {
    val title = if (elapsedMinutes >= 60) {
        "😴 已连续使用 ${elapsedMinutes / 60} 小时${elapsedMinutes % 60} 分钟了，休息一下吧"
    } else {
        "😴 已连续使用 ${elapsedMinutes} 分钟了，休息一下吧"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .background(
                color = Color.White,
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
            Text("😴", fontSize = 22.sp, modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF212121), lineHeight = 18.sp
                )
                Text(
                    text = "注意休息，放松眼睛，保护颈椎！",
                    fontSize = 13.sp, color = Color(0xFF666666),
                    lineHeight = 17.sp, modifier = Modifier.padding(top = 2.dp)
                )
            }
            Text("现在", fontSize = 11.sp, color = Color(0xFF999999),
                modifier = Modifier.padding(start = 8.dp))
        }
    }
}
