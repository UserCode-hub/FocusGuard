package com.focusguard.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.app.manager.DailyReport
import com.focusguard.app.manager.ReportManager
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportScreen(viewModel: MainViewModel) {
    val todayReport by viewModel.todayReport.collectAsState()
    val weeklyReports by viewModel.weeklyReports.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshReport()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 今日总览
        Text(
            text = "今日总览",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 统计卡片网格
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ReportStatCard(
                modifier = Modifier.weight(1f),
                title = "总使用时长",
                value = ReportManager.formatDuration(todayReport.totalDurationMs),
                icon = Icons.Default.Timer,
                color = Color(0xFF1565C0)
            )
            ReportStatCard(
                modifier = Modifier.weight(1f),
                title = "最长连续",
                value = ReportManager.formatDuration(todayReport.maxContinuousMs),
                icon = Icons.Default.TrendingUp,
                color = Color(0xFFF57C00)
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ReportStatCard(
                modifier = Modifier.weight(1f),
                title = "解锁次数",
                value = "${todayReport.unlockCount} 次",
                icon = Icons.Default.Lock,
                color = Color(0xFF388E3C)
            )
            ReportStatCard(
                modifier = Modifier.weight(1f),
                title = "监控状态",
                value = if (viewModel.isServiceRunning) "运行中" else "已停止",
                icon = Icons.Default.Shield,
                color = if (viewModel.isServiceRunning) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
            )
        }

        Spacer(Modifier.height(24.dp))

        // 最近7天记录
        Text(
            text = "近 7 天记录",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (weeklyReports.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "暂无数据",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            weeklyReports.forEach { report ->
                WeekRow(report)
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        // 免责声明
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            )
        ) {
            Text(
                text = "所有使用数据仅存储在本地，不上传云端。",
                modifier = Modifier.padding(12.dp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun ReportStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = color
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun WeekRow(report: DailyReport) {
    val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
    val displayDate = try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(report.date)
        dateFormat.format(date!!)
    } catch (e: Exception) {
        report.date
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = displayDate,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
                Text(
                    text = "解锁 ${report.unlockCount} 次",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = ReportManager.formatDurationShort(report.totalDurationMs),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "最长 ${ReportManager.formatDurationShort(report.maxContinuousMs)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
