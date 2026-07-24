package com.focusguard.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.app.FocusGuardApp
import com.focusguard.app.manager.ReportManager
import com.focusguard.app.util.Constants

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as FocusGuardApp
    val firstThreshold = app.thresholdManager.getFirstReminderMinutes()
    val lockThreshold = firstThreshold + app.thresholdManager.getLockReminderMinutes()

    val todayReport by viewModel.todayReport.collectAsState()
    val elapsedMs by viewModel.elapsedMs.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshReport()
        // 页面可见期间每 10 秒刷新一次报告（确保锁屏归零后数据更新）
        while (true) {
            kotlinx.coroutines.delay(10_000)
            viewModel.refreshReport()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 状态卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 状态指示器
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                    Box(
                        modifier = Modifier.size(12.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    viewModel.isExempted -> Color(0xFFFFA000)
                                    viewModel.isInMonitoringPeriod -> Color(0xFF4CAF50)
                                    else -> Color(0xFF9E9E9E)
                                }
                            )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when {
                            viewModel.isExempted -> "豁免中"
                            viewModel.isInMonitoringPeriod -> "监控中"
                            else -> "监控待命中"
                        },
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 豁免状态提示
                if (viewModel.isExempted) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "📞 通话中 / 🚗 导航中 — 已暂停提醒",
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Color(0xFFE65100)
                        )
                    }
                }

                // 计时器（HH:MM:SS 格式）
                val totalSecs = elapsedMs / 1000
                val hours = totalSecs / 3600
                val minutes = (totalSecs % 3600) / 60
                val seconds = totalSecs % 60

                Text(
                    text = "%02d:%02d:%02d".format(hours, minutes, seconds),
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (viewModel.isExempted) Color(0xFF9E9E9E) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = if (viewModel.isExempted) "豁免中，计时已暂停"
                    else "连续使用时间",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // 监控时段
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("监控时段", fontWeight = FontWeight.Medium)
                    Text(viewModel.monitoringPeriodString, fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 今日统计
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "今日使用",
                value = ReportManager.formatDurationShort(todayReport.totalDurationMs),
                icon = Icons.Default.Timer
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "解锁次数",
                value = "${todayReport.unlockCount} 次",
                icon = Icons.Default.Lock
            )
        }
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(title, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
