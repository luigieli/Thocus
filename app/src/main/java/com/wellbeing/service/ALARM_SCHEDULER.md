# ALARM_SCHEDULER.md — Boundary Timing & Reboot Spec

> Package: `com.wellbeing.service`

## 1. Responsibilities
Enforce exact boundary transitions so that users actively using an app when a restricted window starts are booted immediately, with alarms surviving device reboots.

## 2. Components

### `BoundaryAlarmScheduler`
* Queries `ScheduleEngine.findNextBoundary(windows, LocalDateTime.now())`.
* Converts to epoch millis.
* Sets exact alarm via `AlarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, triggerAtMillis, pendingIntent)`.
* Accepts `windows: List<TimeWindow> = ScheduleRepository.getActiveWindowsCached()` to allow callers with freshly refreshed database entities to schedule immediately.

### `WindowBoundaryReceiver` (`BroadcastReceiver`)
* Triggered at the exact transition second.
* Checks `ForegroundTracker.currentPackage`.
* If the app currently on screen is blocked by the newly active window:
  - Invokes `BlockerAccessibilityService.enforceImmediateBlock()`.
* Uses `goAsync()` on `Dispatchers.IO` to refresh cache from Room and call `BoundaryAlarmScheduler.scheduleNextBoundary(context, refreshedWindows)`.

### `BootReceiver` (`BroadcastReceiver`)
* Listens to `android.intent.action.BOOT_COMPLETED`.
* Uses `goAsync()` on `Dispatchers.IO` to refresh `ScheduleRepository` cache from Room DB and call `BoundaryAlarmScheduler.scheduleNextBoundary(context, refreshedWindows)`, ensuring alarms are armed even on cold reboot without the UI ever opening.

## 3. Invariants
* Alarms must use `FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE`.
* Must handle Android 12+ `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` permission requirements.
* Receivers must never block the main thread with synchronous database queries; always leverage `goAsync()` and `Dispatchers.IO`.
