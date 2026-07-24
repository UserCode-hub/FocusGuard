package com.focusguard.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.focusguard.app.FocusGuardApp
import com.focusguard.app.R
import com.focusguard.app.manager.ReportManager
import com.focusguard.app.manager.TimerManager
import com.focusguard.app.manager.ReminderManager
import com.focusguard.app.receiver.NavStateReceiver
import com.focusguard.app.ui.MainActivity
import com.focusguard.app.util.Constants
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

/**
 * 前台服务 - 核心监控服务（v3.0）
 * - 管理连续使用计时器
 * - 显示通知栏常驻计时/豁免状态
 * - 触发分级提醒（弹窗 + lockNow）
 * - 集成豁免场景：通话/导航中暂停计时
 */
class ScreenMonitorService : Service() {

    private lateinit var timerManager: TimerManager
    private lateinit var reminderManager: ReminderManager
    private lateinit var reportManager: ReportManager
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var notificationUpdateJob: Job? = null
    private var navCheckJob: Job? = null
    private var currentSessionId: Long = 0L
    /** 上一次检查时是否在监控时段 */
    private var wasInMonitoringPeriod: Boolean = false

    companion object {
        const val ACTION_START = "com.focusguard.app.START_MONITORING"
        const val ACTION_STOP = "com.focusguard.app.STOP_MONITORING"
        const val ACTION_SCREEN_ON = "com.focusguard.app.SCREEN_ON"
        const val ACTION_SCREEN_OFF = "com.focusguard.app.SCREEN_OFF"
        const val ACTION_USER_PRESENT = "com.focusguard.app.USER_PRESENT"
        const val ACTION_RESET = "com.focusguard.app.RESET_TIMER"
        const val ACTION_EXEMPTION_CHANGED = "com.focusguard.app.EXEMPTION_CHANGED"

        fun start(context: Context) {
            val intent = Intent(context, ScreenMonitorService::class.java).apply { action = ACTION_START }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(Intent(context, ScreenMonitorService::class.java).apply { action = ACTION_STOP })
        }

        fun notifyScreenOn(context: Context) {
            context.startService(Intent(context, ScreenMonitorService::class.java).apply { action = ACTION_SCREEN_ON })
        }

        fun notifyScreenOff(context: Context) {
            context.startService(Intent(context, ScreenMonitorService::class.java).apply { action = ACTION_SCREEN_OFF })
        }

        fun notifyUserPresent(context: Context) {
            context.startService(Intent(context, ScreenMonitorService::class.java).apply { action = ACTION_USER_PRESENT })
        }

        fun resetTimer(context: Context) {
            context.startService(Intent(context, ScreenMonitorService::class.java).apply { action = ACTION_RESET })
        }

        /** 重置提醒标记（用户修改阈值后调用，让新阈值从当前累计计时开始计算） */
        fun resetReminderFlag(context: Context) {
            context.startService(Intent(context, ScreenMonitorService::class.java).apply { action = ACTION_RESET_REMINDER_FLAG })
        }
    }

    private const val ACTION_RESET_REMINDER_FLAG = "com.focusguard.app.RESET_REMINDER_FLAG"

    override fun onCreate() {
        super.onCreate()
        val app = applicationContext as FocusGuardApp
        timerManager = TimerManager()
        reminderManager = ReminderManager(this)
        reportManager = app.reportManager

        reminderManager.setLockCallback {
            scope.launch {
                timerManager.forceReset()
                currentSessionId = 0L
                updateNotification()
            }
        }

        // 监听豁免状态变化
        app.exemptionManager.setOnExemptionChangedListener { isExempted ->
            handleExemptionChange(isExempted)
        }

        // 初始化豁免状态
        if (app.exemptionManager.isExempted) {
            timerManager.pause()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> { startForegroundService(); startListening(); startNavChecks() }
            ACTION_STOP -> stopForegroundService()
            ACTION_SCREEN_ON -> handleScreenOn()
            ACTION_SCREEN_OFF -> handleScreenOff()
            ACTION_USER_PRESENT -> handleUserPresent()
            ACTION_RESET -> {
                // 先保存本次强制锁屏的会话数据，再归零
                saveCurrentSession()
                timerManager.forceReset()
                updateNotification()
            }
            ACTION_RESET_REMINDER_FLAG -> { timerManager.resetReminderFlag(); updateNotification() }
            ACTION_EXEMPTION_CHANGED -> handleExemptionChange(
                (applicationContext as FocusGuardApp).exemptionManager.isExempted
            )
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        notificationUpdateJob?.cancel()
        navCheckJob?.cancel()
        timerManager.destroy()
        scope.cancel()
    }

    private fun startForegroundService() {
        val notification = createUsageNotification("正在监控屏幕使用时长")
        startForeground(Constants.NOTIFICATION_ID_USAGE, notification)
    }

    private fun startListening() {
        notificationUpdateJob?.cancel()
        notificationUpdateJob = scope.launch {
            timerManager.elapsedMs.collectLatest { ms ->
                updateNotification(ms)
                if (timerManager.isRunning.value && !timerManager.isPaused.value) {
                    checkReminders(ms)
                }
            }
        }
    }

    /** 定时检测前台导航 App（高精度模式） */
    private fun startNavChecks() {
        navCheckJob?.cancel()
        navCheckJob = scope.launch {
            while (isActive) {
                NavStateReceiver().checkForegroundNavApp(this@ScreenMonitorService)
                delay(10_000) // 每 10 秒检测一次
            }
        }
    }

    private fun stopForegroundService() {
        notificationUpdateJob?.cancel()
        navCheckJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun handleScreenOn() {
        // 无论是否监控时段，都正常计时（仅提醒和记录受时段控制）
        if (timerManager.isPaused.value) return
        timerManager.onScreenOn()
        if (timerManager.sessionStartTime > 0 && currentSessionId == 0L) {
            currentSessionId = timerManager.sessionStartTime
        }
    }

    private fun handleScreenOff() {
        if (timerManager.isPaused.value) return
        timerManager.onScreenOff()
        // 注意：不在这里保存会话——仅在被软件强制锁屏时记录
    }

    private fun handleUserPresent() {
        // 无论是否监控时段，都正常计时（仅提醒和记录受时段控制）
        if (timerManager.isPaused.value) return
        if (timerManager.elapsedMs.value > 0 && !timerManager.isRunning.value) {
            timerManager.onUserPresent()
        } else {
            handleScreenOn()
        }
    }

    /** 豁免状态变化处理 */
    private fun handleExemptionChange(isExempted: Boolean) {
        if (isExempted) {
            // 进入豁免 → 暂停计时
            timerManager.pause()
            updateExemptionNotification(true)
        } else {
            // 退出豁免 → 恢复计时
            timerManager.resume()
            updateExemptionNotification(false)
        }
    }

    private fun checkReminders(ms: Long) {
        val app = applicationContext as FocusGuardApp
        if (app.exemptionManager.isExempted) return

        val inPeriod = app.scheduleManager.isInMonitoringPeriod()

        // 从非监控时段 → 进入监控时段：记录基准
        if (inPeriod && !wasInMonitoringPeriod) {
            timerManager.setMonitoringPeriodBase()
        }
        wasInMonitoringPeriod = inPeriod

        if (!inPeriod) return // 非监控时段不触发提醒

        val firstThreshold = app.thresholdManager.getFirstReminderMinutes()
        val lockAfterFirst = app.thresholdManager.getLockReminderMinutes()
        val lockTotal = firstThreshold + lockAfterFirst

        if (timerManager.isFirstReminderDue(firstThreshold)) {
            val elapsedMinutes = (timerManager.elapsedMs.value / 60000).toInt()
            reminderManager.triggerFirstReminder(timerManager, firstThreshold.toInt(), elapsedMinutes)
        }

        if (timerManager.isLockReminderDue(lockTotal)) {
            reminderManager.triggerLockReminder(lockAfterFirst.toInt())
        }
    }

    private fun saveCurrentSession() {
        val app = applicationContext as FocusGuardApp
        if (!app.scheduleManager.isInMonitoringPeriod()) return // 非监控时段不记录
        if (currentSessionId > 0 && timerManager.elapsedMs.value > 0) {
            scope.launch {
                val now = System.currentTimeMillis()
                reportManager.recordUsage(
                    startTime = currentSessionId,
                    endTime = now,
                    wasLocked = true
                )
                currentSessionId = 0L
            }
        }
    }

    private fun updateNotification() {
        updateNotification(timerManager.elapsedMs.value)
    }

    private fun updateNotification(elapsedMs: Long) {
        val app = applicationContext as FocusGuardApp
        val isExempted = app.exemptionManager.isExempted

        val text = when {
            isExempted -> "豁免中，已暂停提醒"
            timerManager.isRunning.value -> "连续使用 ${ReportManager.formatDurationShort(elapsedMs)}"
            else -> "监控中"
        }

        val notification = createUsageNotification(text)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(Constants.NOTIFICATION_ID_USAGE, notification)
    }

    private fun updateExemptionNotification(isExempted: Boolean) {
        if (isExempted) {
            val notification = createUsageNotification("豁免中，已暂停提醒")
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(Constants.NOTIFICATION_ID_USAGE, notification)
        }
    }

    private fun createUsageNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, FocusGuardApp.CHANNEL_USAGE)
            .setContentTitle("专注卫士")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
