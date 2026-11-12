package com.wellbeing.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.wellbeing.core.ScheduleEngine
import com.wellbeing.data.ScheduleRepository
import java.time.LocalDateTime

/**
 * Real-time window state listener and app interceptor.
 *
 * Implements Object Calisthenics: early returns, zero 'else', single level of indentation.
 */
class BlockerAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: BlockerAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        instance = null
        ForegroundTracker.clear()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) {
            return
        }
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val packageName = event.packageName?.toString() ?: return

        if (NeverBlockWhitelist.isWhitelisted(packageName, applicationContext.packageName)) {
            return
        }

        ForegroundTracker.updateForeground(packageName)
        enforceBlockIfRestricted(packageName)
    }

    override fun onInterrupt() {
        // Interrupted by system
    }

    fun enforceBlockIfRestricted(packageName: String) {
        val activeWindows = ScheduleRepository.getActiveWindowsCached()
        val isBlocked = ScheduleEngine.isPackageBlocked(
            packageName = packageName,
            timeWindows = activeWindows,
            targetDateTime = LocalDateTime.now()
        )

        if (!isBlocked) {
            return
        }

        executeInterception(packageName)
    }

    fun enforceImmediateBlock() {
        val activePackage = resolveCurrentActivePackage() ?: return

        if (NeverBlockWhitelist.isWhitelisted(activePackage, applicationContext.packageName)) {
            return
        }

        enforceBlockIfRestricted(activePackage)
    }

    private fun resolveCurrentActivePackage(): String? {
        val trackedPackage = ForegroundTracker.currentPackage
        if (trackedPackage != null) {
            return trackedPackage
        }
        return rootInActiveWindow?.packageName?.toString()
    }

    private fun executeInterception(packageName: String) {
        showBlockNotice(packageName)
    }

    private fun showBlockNotice(packageName: String) {
        val noticeIntent = Intent()
        noticeIntent.setClassName(applicationContext.packageName, "com.wellbeing.ui.BlockNoticeActivity")
        noticeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        noticeIntent.putExtra("extra_blocked_package", packageName)
        startActivity(noticeIntent)
    }
}
