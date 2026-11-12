package com.wellbeing.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room persistence entity for recurring time windows.
 *
 * Implements specifications in DATA_LAYER.md.
 */
@Entity(tableName = "time_windows")
data class TimeWindowEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val enabled: Boolean = true,
    val daysOfWeekCsv: String,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val blockedPackagesCsv: String,
    val breaksCsv: String = "",
    val isAllDay: Boolean = false,
    val breakStartMinuteOfDay: Int? = null,
    val breakEndMinuteOfDay: Int? = null
)
