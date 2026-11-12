# SCHEDULE_ENGINE.md — Focus Windows, Breaks & Boundary Evaluation Spec

> Package: `com.wellbeing.core`

## 1. Responsibilities
Provide pure, deterministic domain logic (no Android framework dependencies) to evaluate whether a target application is blocked at any given `Instant` within scheduled **Focus Windows**, calculate the exact timestamp of the next state change, calculate active focus time remaining, and account for internal break periods.

## 2. Domain Model: Focus Window

A Focus Window is a single, unified entity that represents an active restriction window, along with any optional break periods configured inside it:

```kotlin
data class BreakInterval(
    val startTime: LocalTime,
    val endTime: LocalTime
)

data class TimeWindow(
    val id: Long = 0,
    val name: String,
    val enabled: Boolean = true,
    val daysOfWeek: Set<DayOfWeek> = emptySet(), // java.time.DayOfWeek (MONDAY..SUNDAY)
    val startTime: LocalTime,                     // e.g. 09:00
    val endTime: LocalTime,                       // e.g. 17:00
    val blockedPackages: Set<String> = emptySet(),
    val breaks: List<BreakInterval> = emptyList(), // multiple breaks within the window
    val isAllDay: Boolean = false                 // 24-hour restriction on scheduled days
) {
    val breakCount: Int
        get() = breaks.size

    val hasBreak: Boolean
        get() = breaks.isNotEmpty()

    val breakStartTime: LocalTime?
        get() = breaks.firstOrNull()?.startTime

    val breakEndTime: LocalTime?
        get() = breaks.firstOrNull()?.endTime
}
```

## 3. Evaluation Rules

### `isPackageBlocked(package, timeWindows, targetDateTime)`
1. If `!timeWindow.enabled` or `package !in timeWindow.blockedPackages` $\rightarrow$ Return `false`.
2. Check if `targetDateTime` falls within the base focus window:
   * **All-Day Window (`isAllDay == true`):**
     - Active if: `targetDateTime.dayOfWeek in daysOfWeek` (active for the entire 24-hour day).
   * **Normal Window (`startTime < endTime`):**
     - Active if: `targetDateTime.dayOfWeek in daysOfWeek` AND `targetTime >= startTime` AND `targetTime < endTime`.
   * **Overnight Window (`startTime > endTime`):**
     - Active if:
       - `targetDateTime.dayOfWeek in daysOfWeek` AND `targetTime >= startTime` (first half), OR
       - `targetDateTime.minusDays(1).dayOfWeek in daysOfWeek` AND `targetTime < endTime` (second half).
3. **Break Evaluation (Allowed Usage Periods in 24h Windows & Midnight Boundaries):**
   * If `targetTime` falls within ANY break interval in `timeWindow.breaks`:
     - Normal Break (`break.startTime < break.endTime`): active if `targetTime >= break.startTime && targetTime < break.endTime`.
     - Midnight/Overnight Break (`break.startTime > break.endTime` or `break.endTime == 00:00`): active if `targetTime >= break.startTime` (until end of day 23:59:59) or `targetTime < break.endTime` (if extending past midnight).
     - During an active break, focus is paused and apps are accessible (`isPackageBlocked` returns `false`).
   * When an All-Day (24h) window is combined with breaks (e.g. breaks from 06:00-08:00, 12:00-14:00, 19:00-23:00, 23:00-00:00), the app is ONLY usable during those break periods, and strictly blocked during all other hours of the day.

### `findNextBoundary(timeWindows, fromDateTime): LocalDateTime?`
Computes the exact nearest upcoming transition across all enabled focus windows to feed into the Alarm Scheduler:
* Candidate boundaries per active day include:
  1. `startTime` (focus starts)
  2. For each break in `breaks`:
     - `break.startTime` (focus pauses)
     - `break.endTime` (focus resumes)
  3. `endTime` (focus ends)
* Must check upcoming boundaries within a 7-day lookahead.
* Returns `null` if no focus windows are enabled.

### `calculateActiveFocusRemainingMinutes(windows, now): Long?`
* If any enabled focus window is active at `now`:
  - If currently inside any break in `breaks`: active focus is paused (returns `null` or 0).
  - If active and before an upcoming break: computes minutes until the earliest upcoming `break.startTime` where `break.startTime > targetTime`.
  - If active and after all breaks: computes minutes until `endTime`.
* Returns `null` if no focus window is currently active.

## 4. Invariants & Edge Cases
* Focus windows with breaks are NEVER split into multiple database rows; they remain a single entry displaying the original start/finish times and break count.
* Zero framework imports (pure JVM / Java Time API) to ensure instant, dependency-free unit testing.
* Sunday $\rightarrow$ Monday rollover must be correctly handled for overnight schedules.
