package com.cloudacr.helper

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ACRAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "CloudACR_Helper"
        private const val MAIN_APP_PACKAGE = "com.cloudacr.app"
        private const val PERMISSION = "com.cloudacr.helper.SEND_COMMANDS"

        private val DIALER_PACKAGES = setOf(
            "com.android.dialer",
            "com.android.phone",
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.oneplus.dialer",
            "com.huawei.phone",
            "com.xiaomi.phone",
            "com.oppo.phone",
            "com.bbk.phone"
        )

        var isRunning = false
            private set
    }

    private var callActive = false
    private var lastPhoneNumber = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            packageNames = DIALER_PACKAGES.toTypedArray()
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
        Log.d(TAG, "Accessibility service connected")
        notifyMainAppConnected()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        if (pkg !in DIALER_PACKAGES) return
        try {
            val rootNode = rootInActiveWindow ?: return
            val callState = detectCallState(rootNode)
            when {
                callState == CallState.IN_CALL && !callActive -> {
                    callActive = true
                    val number = extractPhoneNumber(rootNode)
                    lastPhoneNumber = number
                    Log.d(TAG, "Helper: call started ($number)")
                    sendStartRecording(number)
                }
                callState == CallState.IDLE && callActive -> {
                    callActive = false
                    Log.d(TAG, "Helper: call ended")
                    sendStopRecording()
                }
            }
            rootNode.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "Event processing error", e)
        }
    }

    private enum class CallState { IN_CALL, IDLE, UNKNOWN }

    private fun detectCallState(root: AccessibilityNodeInfo): CallState {
        val endCallPatterns = listOf(
            "end_call", "endCall", "btn_end_call", "hangup",
            "end call", "hang up", "disconnect"
        )
        for (pattern in endCallPatterns) {
            val nodes = root.findAccessibilityNodeInfosByViewId("*:id/$pattern")
                .ifEmpty { root.findAccessibilityNodeInfosByText(pattern) }
            if (nodes.isNotEmpty()) {
                nodes.forEach { it.recycle() }
                return CallState.IN_CALL
            }
        }
        return try {
            val tm = getSystemService(TelephonyManager::class.java)
            when (tm.callState) {
                TelephonyManager.CALL_STATE_OFFHOOK -> CallState.IN_CALL
                TelephonyManager.CALL_STATE_IDLE -> CallState.IDLE
                else -> CallState.UNKNOWN
            }
        } catch (e: Exception) { CallState.UNKNOWN }
    }

    private fun extractPhoneNumber(root: AccessibilityNodeInfo): String {
        val numberIds = listOf(
            "phoneNumber", "phone_number", "callerNumber",
            "contactgrid_number", "primary", "caller_id_number"
        )
        for (id in numberIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId("*:id/$id")
            for (node in nodes) {
                val text = node.text?.toString()?.trim() ?: ""
                node.recycle()
                if (text.isNotBlank() && text.any { it.isDigit() }) return text
            }
        }
        return ""
    }

    private fun notifyMainAppConnected() {
        sendBroadcast(Intent("com.cloudacr.helper.HELPER_CONNECTED").apply {
            `package` = MAIN_APP_PACKAGE
            putExtra("sender_package", packageName)
        }, PERMISSION)
    }

    private fun sendStartRecording(phoneNumber: String) {
        sendBroadcast(Intent("com.cloudacr.helper.START_RECORDING").apply {
            `package` = MAIN_APP_PACKAGE
            putExtra("sender_package", packageName)
            putExtra("extra_phone_number", phoneNumber)
            putExtra("extra_call_type", "UNKNOWN")
        }, PERMISSION)
        Log.d(TAG, "Sent START_RECORDING to main app")
    }

    private fun sendStopRecording() {
        sendBroadcast(Intent("com.cloudacr.helper.STOP_RECORDING").apply {
            `package` = MAIN_APP_PACKAGE
            putExtra("sender_package", packageName)
        }, PERMISSION)
        Log.d(TAG, "Sent STOP_RECORDING to main app")
    }

    override fun onInterrupt() { Log.d(TAG, "Accessibility service interrupted") }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        if (callActive) sendStopRecording()
        Log.d(TAG, "Accessibility service destroyed")
    }
}
