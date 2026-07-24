package com.focusguard.app.manager

import android.content.Context
import android.telephony.TelephonyManager

/**
 * 豁免场景管理器
 * 管理通话中 / 开车导航中 两种豁免场景
 * 豁免期间：计时暂停、不触发提醒/锁屏
 */
class ExemptionManager(private val context: Context) {

    private var _isInCall = false
    private var _isNavigating = false
    private var _isExempted = false

    /** 是否在通话中 */
    val isInCall: Boolean get() = _isInCall
    /** 是否在导航中 */
    val isNavigating: Boolean get() = _isNavigating
    /** 是否处于豁免状态（任一豁免场景激活） */
    val isExempted: Boolean get() = _isExempted

    private var onExemptionChanged: ((Boolean) -> Unit)? = null

    fun setOnExemptionChangedListener(listener: (Boolean) -> Unit) {
        onExemptionChanged = listener
    }

    /** 更新通话状态 */
    fun updateCallState(callState: Int) {
        val wasExempted = _isExempted
        _isInCall = when (callState) {
            TelephonyManager.CALL_STATE_IDLE -> false
            TelephonyManager.CALL_STATE_RINGING,
            TelephonyManager.CALL_STATE_OFFHOOK -> true
            else -> false
        }
        updateExemption(wasExempted)
    }

    /** 更新导航状态 */
    fun updateNavigationState(isNavigating: Boolean) {
        val wasExempted = _isExempted
        _isNavigating = isNavigating
        updateExemption(wasExempted)
    }

    private fun updateExemption(wasExempted: Boolean) {
        _isExempted = _isInCall || _isNavigating
        if (_isExempted != wasExempted) {
            onExemptionChanged?.invoke(_isExempted)
        }
    }

    fun destroy() {
        onExemptionChanged = null
    }
}
