package com.focusguard.app.ui

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.app.BuildConfig
import com.focusguard.app.FocusGuardApp
import com.focusguard.app.R
import com.focusguard.app.manager.PermissionHelper
import com.focusguard.app.service.ScreenMonitorService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val app = context.applicationContext as FocusGuardApp
    val permissionHelper = PermissionHelper(context)

    var isHighPrecisionNav by remember { mutableStateOf(permissionHelper.isHighPrecisionNavEnabled()) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        // === 监控时段 ===
        SectionTitle("监控时段")
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("全天监控 00:00 - 24:00", fontWeight = FontWeight.Medium)
                    Text("开启后监控时段生效，提醒功能启用", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = app.scheduleManager.isEnabled(),
                    onCheckedChange = { viewModel.toggleSchedule(it) })
            }
        }

        Spacer(Modifier.height(16.dp))

        // === 提醒时间配置 ===
        var showThresholdDialog by remember { mutableStateOf(false) }

        SectionTitle("提醒时间")
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                val firstMin = app.thresholdManager.getFirstReminderMinutes()
                val lockMin = app.thresholdManager.getLockReminderMinutes()

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("⏰", fontSize = 20.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("首次提醒", fontWeight = FontWeight.Medium)
                        Text("连续使用 ${firstMin} 分钟后", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("🔒", fontSize = 20.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("锁屏提醒", fontWeight = FontWeight.Medium)
                        Text("首次提醒后 ${lockMin} 分钟（累计 ${firstMin + lockMin} 分钟）", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                TextButton(onClick = { showThresholdDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("自定义设置")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // === 提醒时间配置对话框 ===
        if (showThresholdDialog) {
            ThresholdConfigDialog(
                currentFirstMin = app.thresholdManager.getFirstReminderMinutes().toInt(),
                currentLockMin = app.thresholdManager.getLockReminderMinutes().toInt(),
                onDismiss = { showThresholdDialog = false },
                onConfirm = { firstMin, lockMin ->
                    app.thresholdManager.setFirstReminderMinutes(firstMin.toLong())
                    app.thresholdManager.setLockReminderMinutes(lockMin.toLong())
                    showThresholdDialog = false
                }
            )
        }

        // === 豁免场景 ===
        SectionTitle("豁免场景")
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("以下场景由软件自动识别，用户零配置", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp))

                ExemptionRow(icon = "📞", title = "通话中",
                    desc = "来电/去电/通话进行中，自动暂停计时和提醒")
                Spacer(Modifier.height(8.dp))
                ExemptionRow(icon = "🚗", title = "开车导航中",
                    desc = "车载蓝牙连接自动识别，暂停计时和提醒")

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // 导航精检开关
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GpsFixed, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("导航高精度模式", fontWeight = FontWeight.Medium)
                        Text("自动识别导航 App 前台运行", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = isHighPrecisionNav, onCheckedChange = {
                        isHighPrecisionNav = it
                        permissionHelper.setHighPrecisionNav(it)
                        if (it) {
                            // 引导用户开启 usage-stats 权限
                            try {
                                context.startActivity(
                                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                        data = android.net.Uri.parse("package:${context.packageName}")
                                    }
                                )
                            } catch (e: Exception) {
                                Toast.makeText(context, "请手动在设置中开启使用情况访问权限", Toast.LENGTH_LONG).show()
                            }
                        }
                    })
                }

                Text("高精度模式下，即使未连接车载蓝牙也能识别导航 App", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        // === ROM 保活 ===
        SectionTitle("后台保活")
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("ROM 后台保活指南", fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp))
                Text(
                    text = permissionHelper.getRomGuideText(),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text(
                    text = context.getString(R.string.disclaimer),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // === 关于 ===
        SectionTitle("关于")
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("专注卫士", fontWeight = FontWeight.Medium)
                    Text("版本 ${BuildConfig.VERSION_NAME}", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }

}

@Composable
fun ThresholdConfigDialog(
    currentFirstMin: Int,
    currentLockMin: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var firstMin by remember { mutableIntStateOf(currentFirstMin) }
    var lockMin by remember { mutableIntStateOf(currentLockMin) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    // 跟踪原始输入字符串，用于检测非数字字符
    var rawFirstInput by remember { mutableStateOf(currentFirstMin.toString()) }
    var rawLockInput by remember { mutableStateOf(currentLockMin.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义提醒时间") },
        text = {
            Column {
                Text("首次提醒（分钟）", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = firstMin.toString(),
                    onValueChange = {
                        rawFirstInput = it
                        // 检查原始输入是否包含非数字字符
                        if (it.any { c -> !c.isDigit() }) {
                            errorMsg = "设置时间的不合理，请重新设置"
                        } else {
                            it.take(3).toIntOrNull()?.let { n -> firstMin = n }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    suffix = { Text("分钟") },
                    supportingText = { Text("不超过 360 分钟", fontSize = 11.sp) }
                )

                Spacer(Modifier.height(12.dp))
                Text("锁屏提醒（分钟）", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = lockMin.toString(),
                    onValueChange = {
                        rawLockInput = it
                        // 检查原始输入是否包含非数字字符
                        if (it.any { c -> !c.isDigit() }) {
                            errorMsg = "设置时间的不合理，请重新设置"
                        } else {
                            it.take(3).toIntOrNull()?.let { n -> lockMin = n }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    suffix = { Text("分钟") },
                    supportingText = { Text("不超过 180 分钟", fontSize = 11.sp) }
                )

                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp))
                }

                Text("首次提醒：${firstMin} 分钟 ｜ 锁屏（累计 ${firstMin + lockMin} 分钟）",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (firstMin <= 0 || lockMin <= 0 || firstMin > 360 || lockMin > 180) {
                    errorMsg = "设置时间的不合理，请重新设置"
                } else {
                    errorMsg = null
                    onConfirm(firstMin, lockMin)
                }
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun ExemptionRow(icon: String, title: String, desc: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 20.sp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(desc, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp, top = 8.dp))
}


