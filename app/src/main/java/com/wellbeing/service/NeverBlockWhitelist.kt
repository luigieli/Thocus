package com.wellbeing.service

/**
 * Enforces the non-negotiable Never-Block Whitelist specified in AGENTS.md and ACCESSIBILITY.md.
 *
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
}
