package com.wellbeing.service

/**
 * Thread-safe, in-memory cache of the currently active foreground application.
 *
 * Updated by BlockerAccessibilityService upon receiving TYPE_WINDOW_STATE_CHANGED.
 * Consumed by WindowBoundaryReceiver when exact boundary alarms trigger.
 */
object ForegroundTracker {
    @Volatile
    var currentPackage: String? = null
        internal set

    fun updateForeground(packageName: String?) {
        currentPackage = packageName
    }

    fun clear() {
        currentPackage = null
    }
}
