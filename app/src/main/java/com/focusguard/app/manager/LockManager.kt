package com.focusguard.app.manager

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import com.focusguard.app.receiver.DeviceAdminReceiver

class LockManager(private val context: Context) {

    private val devicePolicyManager: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent: ComponentName =
        ComponentName(context, DeviceAdminReceiver::class.java)

    /** 是否有设备管理器权限 */
    fun isAdminActive(): Boolean {
        return devicePolicyManager.isAdminActive(adminComponent)
    }

    /** 获取激活设备管理器的 Intent */
    fun getAdminActivationIntent() = android.content.Intent(
        android.provider.Settings.ACTION_SECURITY_SETTINGS
    )

    /** 执行强制锁屏 */
    fun lockScreen(): Boolean {
        return try {
            if (isAdminActive()) {
                devicePolicyManager.lockNow()
                true
            } else {
                false
            }
        } catch (e: SecurityException) {
            false
        }
    }

    /** 检查应用是否是设备管理员 */
    fun isDeviceAdmin(): Boolean {
        return devicePolicyManager.isAdminActive(adminComponent)
    }
}
