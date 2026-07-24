package com.focusguard.app.manager

import com.focusguard.app.util.Constants
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 连续使用计时器管理器
 * - 屏幕亮起 → 开始计时
 * - 屏幕息屏 → 计时清零（除非 ≤10 秒内重新亮屏）
 * - 豁免期间 → 暂停计时，结束后恢复（从暂停点继续）
 * - 锁屏提醒后用户解锁 → 归零重新开始
 */
class TimerManager {

    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs: StateFlow<Long> = _elapsedMs.asStateFlow()

    private var lastScreenOffTime: Long = 0L
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    /** 是否在暂停中（豁免场景） */
    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    /** 暂停前已累积的毫秒数 */
    private var accumulatedMs: Long = 0L

    /** 是否已触发首次提醒 */
    var firstReminderTriggered: Boolean = false
        private set

    /** 进入监控时段时的累计计时基准（毫秒） */
    private var monitoringPeriodBaseMs: Long = 0L

    /** 当前使用会话开始时间 */
    var sessionStartTime: Long = 0L
        private set

    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /** 屏幕亮起：开始或恢复计时 */
    fun onScreenOn() {
        if (_isRunning.value && !_isPaused.value) return

        val now = System.currentTimeMillis()
        val timeSinceLastOff = now - lastScreenOffTime

        if (!_isPaused.value && lastScreenOffTime > 0 && timeSinceLastOff <= Constants.ANTICHEAT_THRESHOLD_MS) {
            // 息屏 ≤10 秒，视为同一次，不做清除
            return
        }

        if (_isPaused.value) {
            // 豁免中：屏幕亮起但不恢复计时，保持暂停
            return
        }

        // 全新开始
        reset()
        sessionStartTime = now
        _isRunning.value = true
        startTimer()
    }

    /** 屏幕息屏 */
    fun onScreenOff() {
        if (_isPaused.value) return // 豁免中息屏不记录
        lastScreenOffTime = System.currentTimeMillis()
        if (_isRunning.value) {
            stopTimer()
            _isRunning.value = false
        }
    }

    /** 用户解锁（USER_PRESENT） */
    fun onUserPresent() {
        if (_isPaused.value) return // 豁免中不处理

        val now = System.currentTimeMillis()
        val timeSinceLastOff = now - lastScreenOffTime

        if (_isRunning.value) return

        if (lastScreenOffTime > 0 && timeSinceLastOff <= Constants.ANTICHEAT_THRESHOLD_MS) {
            _isRunning.value = true
            startTimer()
        } else {
            reset()
            onScreenOn()
        }
    }

    /** 暂停计时（进入豁免场景） */
    fun pause() {
        if (!_isRunning.value || _isPaused.value) return
        stopTimer()
        _isPaused.value = true
        accumulatedMs = _elapsedMs.value
    }

    /** 恢复计时（退出豁免场景） */
    fun resume() {
        if (!_isPaused.value) return
        _isPaused.value = false
        _elapsedMs.value = accumulatedMs
        if (lastScreenOffTime == 0L || System.currentTimeMillis() - lastScreenOffTime <= Constants.ANTICHEAT_THRESHOLD_MS) {
            // 屏幕亮着且超过10秒？按新规则看，如果没有息屏或息屏不久，恢复计时
            _isRunning.value = true
            startTimer()
        }
        // 如果息屏已超过10秒，则不恢复，等待下次亮屏
    }

    /** 重置计时器（锁屏提醒后、全新会话） */
    fun reset() {
        stopTimer()
        _elapsedMs.value = 0L
        accumulatedMs = 0L
        firstReminderTriggered = false
        sessionStartTime = 0L
        _isPaused.value = false
    }

    fun forceReset() {
        reset()
        _isRunning.value = false
    }

    /** 重置首次提醒标记（用户修改阈值后），让新阈值从当前累计计时开始计算 */
    fun resetReminderFlag() {
        firstReminderTriggered = false
    }

    fun markFirstReminderTriggered() {
        firstReminderTriggered = true
    }

    /**
     * 首次提醒触发条件（用户语义）
     * 触发点 = 进入监控时段时的累计计时 + 用户设置的首次提醒时间
     * 即：_elapsedMs >= monitoringPeriodBaseMs + firstThresholdMinutes
     */
    fun isFirstReminderDue(firstThresholdMinutes: Long = Constants.DEFAULT_FIRST_REMINDER_MINUTES): Boolean {
        return !firstReminderTriggered &&
                _elapsedMs.value >= monitoringPeriodBaseMs + firstThresholdMinutes * 60 * 1000
    }

    /** 记录进入监控时段时的累计计时基准 */
    fun setMonitoringPeriodBase() {
        monitoringPeriodBaseMs = _elapsedMs.value
        firstReminderTriggered = false
    }

    /**
     * 锁屏提醒触发条件
     * 用户语义：首次提醒已触发后，再累计 lockThresholdMinutes 分钟
     * 即：累计总时长 >= firstThresholdMinutes + lockThresholdMinutes
     */
    fun isLockReminderDue(lockThresholdMinutes: Long = Constants.DEFAULT_LOCK_REMINDER_MINUTES): Boolean {
        return firstReminderTriggered &&
                _elapsedMs.value >= lockThresholdMinutes * 60 * 1000
    }

    fun getElapsedMinutes(): Long = _elapsedMs.value / 60_000

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                delay(1000)
                if (!_isPaused.value) {
                    _elapsedMs.value += 1000
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun destroy() {
        stopTimer()
        scope.cancel()
    }
}
