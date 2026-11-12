package com.wellbeing.core

import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Domain representation of a recurring time restriction window.
 *
 * Adheres strictly to the specification in SCHEDULE_ENGINE.md.
 */
data class TimeWindow(
    val id: Long = 0,
    val name: String,
    val enabled: Boolean = true,
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val startTime: LocalTime = LocalTime.MIDNIGHT,
    val endTime: LocalTime = LocalTime.MIDNIGHT,
    val blockedPackages: Set<String> = emptySet(),
    val breaks: List<BreakInterval> = emptyList(),
    val isAllDay: Boolean = false
) {
    constructor(
        id: Long = 0,
        name: String,
        enabled: Boolean = true,
        daysOfWeek: Set<DayOfWeek> = emptySet(),
        startTime: LocalTime,
        endTime: LocalTime,
        blockedPackages: Set<String> = emptySet(),
        breakStartTime: LocalTime?,
        breakEndTime: LocalTime?
    ) : this(
        id = id,
        name = name,
        enabled = enabled,
        daysOfWeek = daysOfWeek,
        startTime = startTime,
        endTime = endTime,
        blockedPackages = blockedPackages,
        breaks = createBreaksFromEndpoints(breakStartTime, breakEndTime),
        isAllDay = false
    )

    /**
     * An overnight window is one where startTime is strictly after endTime
     * (e.g. 22:00 to 06:00), crossing calendar midnight.
     */
    val isOvernight: Boolean
        get() = !isAllDay && startTime.isAfter(endTime)

    val hasBreak: Boolean
        get() = breaks.isNotEmpty()

    val breakCount: Int
        get() = breaks.size

    val breakStartTime: LocalTime?
        get() = breaks.firstOrNull()?.startTime

    val breakEndTime: LocalTime?
        get() = breaks.firstOrNull()?.endTime

    companion object {
        fun createBreaksFromEndpoints(
            breakStartTime: LocalTime?,
            breakEndTime: LocalTime?
        ): List<BreakInterval> {
            if (breakStartTime == null) {
                return emptyList()
            }
            if (breakEndTime == null) {
                return emptyList()
            }
            return listOf(BreakInterval(breakStartTime, breakEndTime))
        }
    }
}
