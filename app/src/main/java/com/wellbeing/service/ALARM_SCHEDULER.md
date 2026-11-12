# ALARM_SCHEDULER.md — Boundary Timing & Reboot Spec

> Package: `com.wellbeing.service`

## 1. Responsibilities
Enforce exact boundary transitions so that users actively using an app when a restricted window starts are booted immediately, with alarms surviving device reboots.

## 2. Components

### `BoundaryAlarmScheduler`
* Queries `ScheduleEngine.findNextBoundary(windows, LocalDateTime.now())`.
* Converts to epoch millis.
* Sets exact alarm via `AlarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, triggerAtMillis, pendingIntent)`.

### `WindowBoundaryReceiver` (`BroadcastReceiver`)
* Triggered at the exact transition second.
* Checks `ForegroundTracker.currentPackage`.
* If the app currently on screen is blocked by the newly active window:
  - Invokes `BlockerAccessibilityService.enforceImmediateBlock()`.
* Calls `BoundaryAlarmScheduler.scheduleNext(context)` to arm the next transition.

### `BootReceiver` (`BroadcastReceiver`)
* Listens to `android.intent.action.BOOT_COMPLETED`.
* Immediately calls `BoundaryAlarmScheduler.scheduleNext(context)` to re-establish exact alarms.

## 3. Invariants
* Alarms must use `FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE`.
* Must handle Android 12+ `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` permission requirements.
