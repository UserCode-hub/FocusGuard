package com.focusguard.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.focusguard.app.FocusGuardApp
import com.focusguard.app.manager.DailyReport
import com.focusguard.app.service.ScreenMonitorService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FocusGuardApp

    /** 当前连续使用时长（毫秒）- 本地实时计时器 */
    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs: StateFlow<Long> = _elapsedMs.asStateFlow()

    var isServiceRunning by mutableStateOf(false)
        private set

    /** 是否处于豁免状态 */
    var isExempted by mutableStateOf(false)
        private set

    private val _todayReport = MutableStateFlow(DailyReport("", 0L, 0L, 0))
    val todayReport: StateFlow<DailyReport> = _todayReport.asStateFlow()

    private val _weeklyReports = MutableStateFlow<List<DailyReport>>(emptyList())
    val weeklyReports: StateFlow<List<DailyReport>> = _weeklyReports.asStateFlow()

    var isInMonitoringPeriod by mutableStateOf(true)
        private set

    var monitoringPeriodString by mutableStateOf("")
        private set

    private var localTimerJob: Job? = null

    init {
        // 安装后自动启动监控服务，无需用户手动操作
        ScreenMonitorService.start(getApplication())
        isServiceRunning = true

        loadData()
        startLocalTimer()
    }

    /** 本地实时计时器（每秒递增1000ms，与北京时间同步） */
    private fun startLocalTimer() {
        localTimerJob?.cancel()
        localTimerJob = viewModelScope.launch {
            var lastTickTime = System.currentTimeMillis()
            var lastReportRefreshSecond = 0L
            while (isActive) {
                delay(1000)  // 每秒触发一次

                // 定期刷新监控时段状态（每秒检查）
                isInMonitoringPeriod = app.scheduleManager.isInMonitoringPeriod()
                isExempted = app.exemptionManager.isExempted

                if (isServiceRunning && !isExempted) {
                    // 无论监控时段与否，都连续计时
                    val now = System.currentTimeMillis()
                    val delta = now - lastTickTime
                    lastTickTime = now
                    _elapsedMs.value += delta.coerceIn(900, 1100)
                } else {
                    lastTickTime = System.currentTimeMillis()
                }

                // 每 5 秒刷新一次报告数据（确保锁屏归零后再次进入能看到最新数据）
                val currentSecond = _elapsedMs.value / 1000
                if (currentSecond != lastReportRefreshSecond && currentSecond % 5 == 0L) {
                    lastReportRefreshSecond = currentSecond
                    _todayReport.value = app.reportManager.getTodayReport()
                    _weeklyReports.value = app.reportManager.getLast7DaysReports()
                }
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _todayReport.value = app.reportManager.getTodayReport()
            _weeklyReports.value = app.reportManager.getLast7DaysReports()
            monitoringPeriodString = app.scheduleManager.getPeriodString()
            isInMonitoringPeriod = app.scheduleManager.isInMonitoringPeriod()
            isExempted = app.exemptionManager.isExempted
        }
    }

    fun refreshReport() { loadData() }

    fun toggleSchedule(enabled: Boolean) {
        app.scheduleManager.setEnabled(enabled)
    }

    /** 清除所有本地数据（使用记录 + 设置） */
    fun clearAllData() {
        viewModelScope.launch {
            // 1. 清空 Room 数据库
            app.reportManager.clearAllRecords()
            // 2. 重置 SharedPreferences
            app.thresholdManager.resetToDefaults()
            app.scheduleManager.resetToDefaults()
            // 3. 清空其他 SharedPreferences（settings, setup_prefs）
            getApplication<Application>()
                .getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
                .edit().clear().apply()
            getApplication<Application>()
                .getSharedPreferences("setup_prefs", android.content.Context.MODE_PRIVATE)
                .edit().clear().apply()
            // 4. 刷新界面
            _todayReport.value = DailyReport("", 0L, 0L, 0)
            _weeklyReports.value = emptyList()
            _elapsedMs.value = 0L
        }
    }

    override fun onCleared() {
        super.onCleared()
        localTimerJob?.cancel()
    }
}
