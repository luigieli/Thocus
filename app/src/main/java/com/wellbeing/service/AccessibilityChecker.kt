package com.wellbeing.service

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

/**
 * Utility to verify accessibility service status in system settings.
 *
 * Implements Object Calisthenics: zero 'else', early returns, single-level indentation.
 */
object AccessibilityChecker {

    fun isServiceEnabledInSettings(context: Context): Boolean {
        if (checkAccessibilityManager(context)) {
            return true
        }
        return checkSecureSettings(context)
    }

    private fun checkAccessibilityManager(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val services = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK) ?: return false
        val targetName = BlockerAccessibilityService::class.java.name
        for (service in services) {
            if (matchesTargetService(service.id, targetName)) {
                return true
            }
        }
        return false
    }

    private fun matchesTargetService(serviceId: String?, targetName: String): Boolean {
        if (serviceId == null) {
            return false
        }
        return serviceId.contains(targetName)
    }

    private fun checkSecureSettings(context: Context): Boolean {
        val setting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return setting.contains(BlockerAccessibilityService::class.java.name)
    }
}
