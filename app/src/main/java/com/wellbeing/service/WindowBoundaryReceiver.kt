package com.wellbeing.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Triggered at the exact transition second by AlarmManager.
 *
 * Implements specifications in ALARM_SCHEDULER.md.
 */
class WindowBoundaryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // 1. Kick the user out immediately if they are currently inside a blocked app
        BlockerAccessibilityService.instance?.enforceImmediateBlock()

        // 2. Schedule the next upcoming boundary
        BoundaryAlarmScheduler.scheduleNextBoundary(context)
    }
}
