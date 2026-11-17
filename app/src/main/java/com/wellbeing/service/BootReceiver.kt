package com.wellbeing.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wellbeing.data.ScheduleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Reschedules boundary alarms upon device boot.
 *
 * Implements Object Calisthenics: early return guard, single-level indentation, methods < 15 lines.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        rescheduleAlarmsAsync(context)
    }

    private fun rescheduleAlarmsAsync(context: Context) {
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
