package com.wellbeing.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Reschedules boundary alarms upon device boot.
 *
 * Implements Object Calisthenics: early return guard.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        BoundaryAlarmScheduler.scheduleNextBoundary(context)
    }
}
