package com.focusguard.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.focusguard.app.data.AppDatabase
import com.focusguard.app.manager.ExemptionManager
import com.focusguard.app.manager.LockManager
import com.focusguard.app.manager.ReportManager
import com.focusguard.app.manager.ScheduleManager
import com.focusguard.app.manager.ThresholdManager

class FocusGuardApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var exemptionManager: ExemptionManager
        private set
    lateinit var thresholdManager: ThresholdManager
        private set
    lateinit var scheduleManager: ScheduleManager
        private set
    lateinit var lockManager: LockManager
        private set
    lateinit var reportManager: ReportManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        initDatabase()
        initManagers()
        createNotificationChannels()
    }

    private fun initDatabase() {
        database = AppDatabase.getInstance(this)
    }

    private fun initManagers() {
        exemptionManager = ExemptionManager(this)
        thresholdManager = ThresholdManager(this)
        scheduleManager = ScheduleManager(this)
        lockManager = LockManager(this)
        reportManager = ReportManager(this, database)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            val usageChannel = NotificationChannel(
                CHANNEL_USAGE,
                "使用时长监控",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示当前连续使用时长/豁免状态"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannel(usageChannel)
        }
    }

    companion object {
        const val CHANNEL_USAGE = "channel_usage"

        lateinit var instance: FocusGuardApp
            private set
    }
}
