# DATA_LAYER.md — Persistence & Schema Spec for Focus Windows

> Package: `com.wellbeing.data`

## 1. Responsibilities
Persist `TimeWindow` focus window rules, breaks, and blocked packages locally using Room, and provide a reactive stream (`Flow<List<TimeWindow>>`) to the rest of the application.

## 2. Room Schema

### `TimeWindowEntity`
```kotlin
@Entity(tableName = "time_windows")
data class TimeWindowEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val enabled: Boolean,
    val daysOfWeekCsv: String,        // e.g. "MONDAY,TUESDAY,WEDNESDAY"
    val startMinuteOfDay: Int,        // 0..1439
    val endMinuteOfDay: Int,          // 0..1439
    val blockedPackagesCsv: String,   // e.g. "com.instagram.android,com.zhiliaoapp.musically"
    val breaksCsv: String = "",       // e.g. "12:00-13:00,15:00-15:30"
    val isAllDay: Boolean = false,    // 24-hour restriction on scheduled days
    val breakStartMinuteOfDay: Int? = null, // legacy fallback e.g. 720 (12:00)
    val breakEndMinuteOfDay: Int? = null    // legacy fallback e.g. 780 (13:00)
)
```

## 3. Invariants
* Data converters must be deterministic.
* Room queries must never be invoked synchronously on the main thread.
* Focus windows with multiple breaks are saved as a single entity with serialized `breaksCsv` (`HH:mm-HH:mm,HH:mm-HH:mm`), avoiding split entries.
* The repository must expose an in-memory cached copy of active focus windows for the `AccessibilityService` to check with zero latency.
* Persistence operations (`saveWindow`, `deleteWindow`) must synchronously refresh `cachedWindows` via `refreshCache()` to prevent stale cache read races.
