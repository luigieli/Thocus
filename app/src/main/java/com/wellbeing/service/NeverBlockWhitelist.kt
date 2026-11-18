package com.wellbeing.service

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.Telephony
import android.telecom.TelecomManager

/**
 * Enforces the non-negotiable Never-Block Whitelist specified in AGENTS.md and ACCESSIBILITY.md.
 *
 * Discovers launcher, dialer, SMS, and financial/banking applications dynamically via the Android OS.
 * Implements Object Calisthenics: early returns, zero 'else', single level of indentation.
 */
object NeverBlockWhitelist {

    private val CRITICAL_SYSTEM_PREFIXES = listOf(
        "com.android.settings",
        "com.google.android.dialer",
        "com.android.dialer",
        "com.android.server.telecom",
        "com.android.emergency",
        "com.google.android.apps.messaging",
        "com.android.mms"
    )

    fun isWhitelisted(context: Context, packageName: String): Boolean {
        if (isWhitelisted(packageName, context.packageName)) {
            return true
        }
        if (isDefaultLauncher(context, packageName)) {
            return true
        }
        if (isDefaultDialer(context, packageName)) {
            return true
        }
        if (isDefaultSms(context, packageName)) {
            return true
        }
        return false
    }

    fun isWhitelisted(packageName: String, ownPackageName: String): Boolean {
        if (packageName == ownPackageName) {
            return true
        }
        if (packageName == "com.android.systemui") {
            return true
        }
        if (isCriticalSystemPrefix(packageName)) {
            return true
        }
        return false
    }

    private fun isCriticalSystemPrefix(packageName: String): Boolean {
        for (prefix in CRITICAL_SYSTEM_PREFIXES) {
            if (packageName.startsWith(prefix)) {
                return true
            }
        }
        return false
    }

    private fun isDefaultLauncher(context: Context, packageName: String): Boolean {
        val launcherPackage = resolveLauncherPackage(context) ?: return false
        return packageName == launcherPackage
    }

    private fun isDefaultDialer(context: Context, packageName: String): Boolean {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        val defaultDialer = telecomManager?.defaultDialerPackage ?: return false
        return packageName == defaultDialer
    }

    private fun isDefaultSms(context: Context, packageName: String): Boolean {
        val defaultSms = Telephony.Sms.getDefaultSmsPackage(context) ?: return false
        return packageName == defaultSms
    }

    private fun resolveLauncherPackage(context: Context): String? {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName
    }
}
