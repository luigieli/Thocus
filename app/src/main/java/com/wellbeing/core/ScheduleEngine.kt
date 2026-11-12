package com.wellbeing.core

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Pure domain engine for evaluating schedule restrictions and boundary transitions.
 *
 * Implemented strictly following Object Calisthenics:
 * - No 'else' keyword (early returns only)
 * - Single level of indentation per method
 * - Low cognitive load, small single-responsibility functions
 */
object ScheduleEngine {

    fun isPackageBlocked(
        packageName: String,
        timeWindows: Collection<TimeWindow>,
        targetDateTime: LocalDateTime = LocalDateTime.now()
    ): Boolean {
        for (timeWindow in timeWindows) {
            if (isWindowBlockingPackage(timeWindow, packageName, targetDateTime)) {
                return true
            }
        }
        return false
    }

    private fun isWindowBlockingPackage(
        timeWindow: TimeWindow,
        packageName: String,
        targetDateTime: LocalDateTime
    ): Boolean {
        if (!timeWindow.enabled) {
            return false
        }
        if (!timeWindow.blockedPackages.contains(packageName)) {
            return false
        }
        return isWindowActiveAt(timeWindow, targetDateTime)
    }

    fun isWindowActiveAt(timeWindow: TimeWindow, targetDateTime: LocalDateTime): Boolean {
        if (!timeWindow.enabled) {
            return false
        }
        val isBaseActive = evaluateBaseWindowActive(timeWindow, targetDateTime)
        if (!isBaseActive) {
            return false
        }
        if (isBreakActiveAt(timeWindow, targetDateTime)) {
            return false
        }
        return true
    }

    private fun evaluateBaseWindowActive(timeWindow: TimeWindow, targetDateTime: LocalDateTime): Boolean {
        if (timeWindow.isAllDay) {
            return isAllDayWindowActive(timeWindow, targetDateTime)
        }
        if (timeWindow.isOvernight) {
            return isOvernightWindowActive(timeWindow, targetDateTime)
        }
        return isDaytimeWindowActive(timeWindow, targetDateTime)
    }

    private fun isAllDayWindowActive(timeWindow: TimeWindow, targetDateTime: LocalDateTime): Boolean {
        val today = targetDateTime.dayOfWeek
        return timeWindow.daysOfWeek.contains(today)
    }

    private fun isBreakActiveAt(timeWindow: TimeWindow, targetDateTime: LocalDateTime): Boolean {
        val targetTime = targetDateTime.toLocalTime()
        return timeWindow.breaks.any { isTimeWithinBreakBounds(targetTime, it.startTime, it.endTime) }
    }

    private fun isTimeWithinBreakBounds(
        targetTime: java.time.LocalTime,
        breakStart: java.time.LocalTime,
        breakEnd: java.time.LocalTime
    ): Boolean {
        if (breakStart.isAfter(breakEnd)) {
            return isOvernightBreakActive(targetTime, breakStart, breakEnd)
        }
        return isNormalBreakActive(targetTime, breakStart, breakEnd)
    }

    private fun isNormalBreakActive(
        targetTime: java.time.LocalTime,
        breakStart: java.time.LocalTime,
        breakEnd: java.time.LocalTime
    ): Boolean {
        if (targetTime.isBefore(breakStart)) {
            return false
        }
        return targetTime.isBefore(breakEnd)
    }

    private fun isOvernightBreakActive(
        targetTime: java.time.LocalTime,
        breakStart: java.time.LocalTime,
        breakEnd: java.time.LocalTime
    ): Boolean {
        if (!targetTime.isBefore(breakStart)) {
            return true
        }
        if (breakEnd == java.time.LocalTime.MIDNIGHT) {
            return false
        }
        return targetTime.isBefore(breakEnd)
    }

    private fun isDaytimeWindowActive(timeWindow: TimeWindow, targetDateTime: LocalDateTime): Boolean {
        val today = targetDateTime.dayOfWeek
        if (!timeWindow.daysOfWeek.contains(today)) {
            return false
        }

        val targetTime = targetDateTime.toLocalTime()
        if (targetTime.isBefore(timeWindow.startTime)) {
            return false
        }
        if (!targetTime.isBefore(timeWindow.endTime)) {
            return false
        }
        return true
    }

    private fun isOvernightWindowActive(timeWindow: TimeWindow, targetDateTime: LocalDateTime): Boolean {
        if (isFirstHalfOfOvernightActive(timeWindow, targetDateTime)) {
            return true
        }
        return isSecondHalfOfOvernightActive(timeWindow, targetDateTime)
    }

    private fun isFirstHalfOfOvernightActive(timeWindow: TimeWindow, targetDateTime: LocalDateTime): Boolean {
        val today = targetDateTime.dayOfWeek
        if (!timeWindow.daysOfWeek.contains(today)) {
            return false
        }

        val targetTime = targetDateTime.toLocalTime()
        return !targetTime.isBefore(timeWindow.startTime)
    }

    private fun isSecondHalfOfOvernightActive(timeWindow: TimeWindow, targetDateTime: LocalDateTime): Boolean {
        val yesterday = targetDateTime.minusDays(1).dayOfWeek
        if (!timeWindow.daysOfWeek.contains(yesterday)) {
            return false
        }

        val targetTime = targetDateTime.toLocalTime()
        return targetTime.isBefore(timeWindow.endTime)
    }

    fun findNextBoundary(
        timeWindows: Collection<TimeWindow>,
        fromDateTime: LocalDateTime = LocalDateTime.now()
    ): LocalDateTime? {
        val enabledWindows = filterEnabledWindows(timeWindows)
        if (enabledWindows.isEmpty()) {
            return null
        }

        val candidateBoundaries = mutableListOf<LocalDateTime>()
        for (dayOffset in 0..8L) {
            collectBoundariesForOffset(candidateBoundaries, enabledWindows, fromDateTime, dayOffset)
        }

        return candidateBoundaries.minOrNull()
    }

    private fun filterEnabledWindows(timeWindows: Collection<TimeWindow>): List<TimeWindow> {
        val enabledList = mutableListOf<TimeWindow>()
        for (window in timeWindows) {
            if (window.enabled) {
                enabledList.add(window)
            }
        }
        return enabledList
    }

    private fun collectBoundariesForOffset(
        destinationList: MutableList<LocalDateTime>,
        timeWindows: List<TimeWindow>,
        fromDateTime: LocalDateTime,
        dayOffset: Long
    ) {
        val targetDate = fromDateTime.toLocalDate().plusDays(dayOffset)
        for (timeWindow in timeWindows) {
            addWindowBoundariesIfApplicable(destinationList, timeWindow, targetDate, fromDateTime)
        }
    }

    private fun addWindowBoundariesIfApplicable(
        destinationList: MutableList<LocalDateTime>,
        timeWindow: TimeWindow,
        targetDate: LocalDate,
        fromDateTime: LocalDateTime
    ) {
        if (!timeWindow.daysOfWeek.contains(targetDate.dayOfWeek)) {
            return
        }

        addStartBoundary(destinationList, timeWindow, targetDate, fromDateTime)
        addBreakBoundariesIfApplicable(destinationList, timeWindow, targetDate, fromDateTime)
        val endBoundary = computeEndBoundary(timeWindow, targetDate)
        addCandidateBoundary(destinationList, endBoundary, fromDateTime)
    }

    private fun addStartBoundary(
        destinationList: MutableList<LocalDateTime>,
        timeWindow: TimeWindow,
        targetDate: LocalDate,
        fromDateTime: LocalDateTime
    ) {
        if (timeWindow.isAllDay) {
            addCandidateBoundary(destinationList, targetDate.atStartOfDay(), fromDateTime)
            return
        }
        addCandidateBoundary(destinationList, targetDate.atTime(timeWindow.startTime), fromDateTime)
    }

    private fun addBreakBoundariesIfApplicable(
        destinationList: MutableList<LocalDateTime>,
        timeWindow: TimeWindow,
        targetDate: LocalDate,
        fromDateTime: LocalDateTime
    ) {
        for (breakInterval in timeWindow.breaks) {
            addSingleBreakBoundaries(destinationList, breakInterval, targetDate, fromDateTime)
        }
    }

    private fun addSingleBreakBoundaries(
        destinationList: MutableList<LocalDateTime>,
        breakInterval: BreakInterval,
        targetDate: LocalDate,
        fromDateTime: LocalDateTime
    ) {
        addCandidateBoundary(destinationList, targetDate.atTime(breakInterval.startTime), fromDateTime)
        val endBoundary = computeBreakEndBoundary(breakInterval, targetDate)
        addCandidateBoundary(destinationList, endBoundary, fromDateTime)
    }

    private fun computeBreakEndBoundary(
        breakInterval: BreakInterval,
        targetDate: LocalDate
    ): LocalDateTime {
        if (breakInterval.startTime.isAfter(breakInterval.endTime)) {
            return targetDate.plusDays(1).atTime(breakInterval.endTime)
        }
        if (breakInterval.endTime == java.time.LocalTime.MIDNIGHT) {
            return targetDate.plusDays(1).atTime(breakInterval.endTime)
        }
        return targetDate.atTime(breakInterval.endTime)
    }

    private fun addCandidateBoundary(
        destinationList: MutableList<LocalDateTime>,
        candidate: LocalDateTime,
        fromDateTime: LocalDateTime
    ) {
        if (candidate.isAfter(fromDateTime)) {
            destinationList.add(candidate)
        }
    }

    private fun computeEndBoundary(timeWindow: TimeWindow, targetDate: LocalDate): LocalDateTime {
        if (timeWindow.isAllDay) {
            return targetDate.plusDays(1).atStartOfDay()
        }
        if (timeWindow.isOvernight) {
            return targetDate.plusDays(1).atTime(timeWindow.endTime)
        }
        return targetDate.atTime(timeWindow.endTime)
    }

    fun calculateActiveFocusRemainingMinutes(
        timeWindows: Collection<TimeWindow>,
        targetDateTime: LocalDateTime = LocalDateTime.now()
    ): Long? {
        val remainingDurations = mutableListOf<Long>()
        for (window in timeWindows) {
            collectRemainingMinutesIfActive(remainingDurations, window, targetDateTime)
        }
        return remainingDurations.minOrNull()
    }

    private fun collectRemainingMinutesIfActive(
        destination: MutableList<Long>,
        window: TimeWindow,
        targetDateTime: LocalDateTime
    ) {
        if (!isWindowActiveAt(window, targetDateTime)) {
            return
        }
        val endDateTime = computeActiveEndDateTime(window, targetDateTime)
        val minutes = java.time.Duration.between(targetDateTime, endDateTime).toMinutes()
        if (minutes > 0) {
            destination.add(minutes)
        }
    }

    private fun computeActiveEndDateTime(
        window: TimeWindow,
        targetDateTime: LocalDateTime
    ): LocalDateTime {
        val today = targetDateTime.toLocalDate()
        val targetTime = targetDateTime.toLocalTime()
        val nextBreakStart = findNextUpcomingBreakStart(window.breaks, targetTime)
        if (nextBreakStart != null) {
            return today.atTime(nextBreakStart)
        }
        return computeWindowEndDateTime(window, today, targetTime)
    }

    private fun findNextUpcomingBreakStart(
        breaks: List<BreakInterval>,
        targetTime: java.time.LocalTime
    ): java.time.LocalTime? {
        val upcomingStarts = mutableListOf<java.time.LocalTime>()
        for (breakInterval in breaks) {
            collectUpcomingBreakStart(upcomingStarts, breakInterval.startTime, targetTime)
        }
        return upcomingStarts.minOrNull()
    }

    private fun collectUpcomingBreakStart(
        destination: MutableList<java.time.LocalTime>,
        breakStartTime: java.time.LocalTime,
        targetTime: java.time.LocalTime
    ) {
        if (breakStartTime.isAfter(targetTime)) {
            destination.add(breakStartTime)
        }
    }

    private fun computeWindowEndDateTime(
        window: TimeWindow,
        today: LocalDate,
        targetTime: java.time.LocalTime
    ): LocalDateTime {
        if (window.isAllDay) {
            return today.plusDays(1).atStartOfDay()
        }
        if (!window.isOvernight) {
            return today.atTime(window.endTime)
        }
        if (!targetTime.isBefore(window.startTime)) {
            return today.plusDays(1).atTime(window.endTime)
        }
        return today.atTime(window.endTime)
    }

    fun splitWithBreak(
        baseWindow: TimeWindow,
        breakStart: java.time.LocalTime,
        breakEnd: java.time.LocalTime
    ): List<TimeWindow> {
        val firstSegment = baseWindow.copy(
            id = 0L,
            name = "${baseWindow.name} (Part 1)",
            endTime = breakStart
        )
        val secondSegment = baseWindow.copy(
            id = 0L,
            name = "${baseWindow.name} (Part 2)",
            startTime = breakEnd
        )
        return listOf(firstSegment, secondSegment)
    }
}
