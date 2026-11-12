package com.wellbeing.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.wellbeing.core.ScheduleEngine
import com.wellbeing.data.ScheduleRepository
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Manages exact boundary alarm scheduling to ensure immediate enforcement
 * the second a scheduled time window begins or ends.
 *
 * Implements Object Calisthenics: zero 'else', early returns, single-level indentation.
 */
object BoundaryAlarmScheduler {

    const val REQUEST_CODE_WINDOW_BOUNDARY = 1001

    fun scheduleNextBoundary(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        if (!canScheduleExactAlarms(alarmManager)) {
            return
        }

        val activeWindows = ScheduleRepository.getActiveWindowsCached()
        val nextBoundary = ScheduleEngine.findNextBoundary(activeWindows, LocalDateTime.now()) ?: return

        val triggerEpochMillis = convertToEpochMillis(nextBoundary)
        val pendingIntent = createBoundaryPendingIntent(
            context = context,
            flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ) ?: return

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerEpochMillis,
            pendingIntent
        )
    }

    fun cancelAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = createBoundaryPendingIntent(
            context = context,
            flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return

        alarmManager.cancel(pendingIntent)
    }

    private fun canScheduleExactAlarms(alarmManager: AlarmManager): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true
        }
        return alarmManager.canScheduleExactAlarms()
    }

    private fun convertToEpochMillis(dateTime: LocalDateTime): Long {
        return dateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private fun createBoundaryPendingIntent(context: Context, flags: Int): PendingIntent? {
        val intent = Intent(context, WindowBoundaryReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_WINDOW_BOUNDARY,
            intent,
            flags
        )
    }
}
