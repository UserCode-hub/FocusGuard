package com.focusguard.app.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.app.manager.PermissionHelper
import com.focusguard.app.service.ScreenMonitorService
import com.focusguard.app.util.Constants

class SetupWizardActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SetupWizardContent(
                onComplete = {
                    PermissionHelper(this).markSetupCompleted()
                    ScreenMonitorService.start(this)
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            )
        }
    }
}

data class SetupStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val buttonText: String,
    val skipText: String = "暂不开启",
    val isRequired: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupWizardContent(onComplete: () -> Unit) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(0) }

    val steps = remember {
        listOf(
            SetupStep("欢迎使用专注卫士",
                "自动监控屏幕使用时长，超时提醒+锁屏，帮您减少无意识刷机。\n\n通话中、开车导航时自动静默，不打扰正当使用。\n点击\"开始设置\"将弹出设备管理器授权申请。",
                Icons.Default.Shield, "开始设置"),
            SetupStep("设备管理器权限（必需）",
                "专注卫士需要设备管理器权限来锁定屏幕（作为提醒）。\n\n⚠️ 卸载方式：设置 → 安全 → 设备管理器",
                Icons.Default.AdminPanelSettings, "授予权限", isRequired = true),
            SetupStep("自启动权限（强烈建议）",
                "开启后，关机重启也能自动保护您。建议允许专注卫士自启动。",
                Icons.Default.PowerSettingsNew, "去开启"),
            SetupStep("电池优化白名单（强烈建议）",
                "关闭电池优化，让提醒更稳定。系统可能为了省电而停止后台运行。",
                Icons.Default.BatteryChargingFull, "去设置"),
            SetupStep("通知权限（建议）",
                "开启通知，获取使用时长提醒和常驻计时显示。",
                Icons.Default.Notifications, "去开启"),
            SetupStep("导航检测精度（可选）",
                "开车导航免打扰默认通过车载蓝牙自动识别，无需任何设置。\n如需更高精度（自动识别导航 App），可开启\"导航精检\"。",
                Icons.Default.GpsFixed, "去开启 UsageStats 权限"),
            SetupStep("设置完成！",
                "专注卫士已就绪，将自动开始监控您的屏幕使用时间。\n\n通话中、开车导航时自动暂停提醒。\n您可在设置页面调整监控时段。",
                Icons.Default.CheckCircle, "立即体验")
        )
    }

    // 设备管理器授权启动器
    val deviceAdminLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // 授权后自动进入下一步
        if (currentStep == 0 || currentStep == 1) {
            currentStep = 2
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (currentStep > 0 && currentStep < steps.size - 1) {
                LinearProgressIndicator(
                    progress = { (currentStep).toFloat() / (steps.size - 2) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            AnimatedVisibility(
                visible = true,
                enter = slideInHorizontally { it },
                exit = slideOutHorizontally { -it }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = steps[currentStep].icon,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp).padding(bottom = 16.dp),
                        tint = if (currentStep == 1) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                    Text(steps[currentStep].title, fontSize = 22.sp,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp))
                    Text(steps[currentStep].description, fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp, modifier = Modifier.padding(bottom = 24.dp))
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    when (currentStep) {
                        0 -> {
                            // 欢迎页点击开始设置 → 直接弹出设备管理器授权
                            val dpm = context.getSystemService(android.content.Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
                            val adminComponent = android.content.ComponentName(
                                context, com.focusguard.app.receiver.DeviceAdminReceiver::class.java
                            )
                            if (!dpm.isAdminActive(adminComponent)) {
                                val intent = android.content.Intent(
                                    android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN
                                ).apply {
                                    putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                    putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                        "专注卫士需要设备管理器权限来锁定屏幕（作为提醒）")
                                }
                                deviceAdminLauncher.launch(intent)
                            } else {
                                currentStep = 2
                            }
                        }
                        1 -> {
                            val dpm = context.getSystemService(android.content.Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
                            val adminComponent = android.content.ComponentName(
                                context, com.focusguard.app.receiver.DeviceAdminReceiver::class.java
                            )
                            if (!dpm.isAdminActive(adminComponent)) {
                                val intent = android.content.Intent(
                                    android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN
                                ).apply {
                                    putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                    putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                        "专注卫士需要设备管理器权限来锁定屏幕（作为提醒）")
                                }
                                deviceAdminLauncher.launch(intent)
                            } else { currentStep = 2 }
                        }
                        2 -> {
                            context.startActivity(
                                android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = android.net.Uri.parse("package:${context.packageName}")
                                }
                            ); currentStep = 3
                        }
                        3 -> {
                            try {
                                context.startActivity(
                                    android.content.Intent(
                                        android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                        android.net.Uri.parse("package:${context.packageName}")
                                    )
                                )
                            } catch (e: Exception) {
                                Toast.makeText(context, "请手动在电池设置中关闭优化", Toast.LENGTH_LONG).show()
                            }; currentStep = 4
                        }
                        4 -> {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                context.startActivity(
                                    android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                )
                            }; currentStep = 5
                        }
                        5 -> {
                            try {
                                context.startActivity(
                                    android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                        data = android.net.Uri.parse("package:${context.packageName}")
                                    }
                                )
                            } catch (e: Exception) {
                                Toast.makeText(context, "请手动在设置中开启使用情况访问权限", Toast.LENGTH_LONG).show()
                            }; currentStep = 6
                        }
                        6 -> onComplete()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp)
            ) { Text(steps[currentStep].buttonText, fontSize = 18.sp) }

            if (currentStep > 0 && currentStep < steps.size - 1 && !steps[currentStep].isRequired) {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { currentStep++ }) {
                    Text("跳过", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
