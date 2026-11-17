package com.wellbeing.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wellbeing.data.ScheduleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Triggered at the exact transition second by AlarmManager.
 *
 * Implements specifications in ALARM_SCHEDULER.md.
 */
class WindowBoundaryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        BlockerAccessibilityService.instance?.enforceImmediateBlock()
        rescheduleUpcomingBoundaryAsync(context)
    }

    private fun rescheduleUpcomingBoundaryAsync(context: Context) {
        val pendingResult = goAsync()
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        coroutineScope.launch {
            reconcileAndSchedule(context, pendingResult)
        }
    }

    private suspend fun reconcileAndSchedule(context: Context, pendingResult: PendingResult) {
        try {
            val repository = ScheduleRepository.getInstance(context)
            val windows = repository.refreshCache()
            BoundaryAlarmScheduler.scheduleNextBoundary(context, windows)
        } finally {
            pendingResult.finish()
        }
    }
}
