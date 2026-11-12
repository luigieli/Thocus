package com.wellbeing.data

import com.wellbeing.core.BreakInterval
import com.wellbeing.core.TimeWindow
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Maps between Room TimeWindowEntity and pure domain TimeWindow.
 *
 * Implements Object Calisthenics: zero 'else', small pure functions.
 */
object TimeWindowMapper {

    fun toDomain(entity: TimeWindowEntity): TimeWindow {
        val daysOfWeek = parseDaysOfWeek(entity.daysOfWeekCsv)
        val blockedPackages = parseBlockedPackages(entity.blockedPackagesCsv)
        val startTime = parseMinuteOfDay(entity.startMinuteOfDay)
        val endTime = parseMinuteOfDay(entity.endMinuteOfDay)
        val breaks = parseBreaks(entity.breaksCsv, entity.breakStartMinuteOfDay, entity.breakEndMinuteOfDay)

        return TimeWindow(
            id = entity.id,
            name = entity.name,
            enabled = entity.enabled,
            daysOfWeek = daysOfWeek,
            startTime = startTime,
            endTime = endTime,
            blockedPackages = blockedPackages,
            breaks = breaks,
            isAllDay = entity.isAllDay
        )
    }

    fun toEntity(domain: TimeWindow): TimeWindowEntity {
        return TimeWindowEntity(
            id = domain.id,
            name = domain.name,
            enabled = domain.enabled,
            daysOfWeekCsv = formatDaysOfWeek(domain.daysOfWeek),
            startMinuteOfDay = formatToMinuteOfDay(domain.startTime),
            endMinuteOfDay = formatToMinuteOfDay(domain.endTime),
            blockedPackagesCsv = formatBlockedPackages(domain.blockedPackages),
            breaksCsv = formatBreaks(domain.breaks),
            isAllDay = domain.isAllDay,
            breakStartMinuteOfDay = formatOptionalToMinuteOfDay(domain.breakStartTime),
            breakEndMinuteOfDay = formatOptionalToMinuteOfDay(domain.breakEndTime)
        )
    }

    private fun parseDaysOfWeek(csv: String): Set<DayOfWeek> {
        if (csv.isBlank()) {
            return emptySet()
        }
        val days = mutableSetOf<DayOfWeek>()
        for (item in csv.split(",")) {
            val trimmed = item.trim()
            if (trimmed.isNotEmpty()) {
                days.add(DayOfWeek.valueOf(trimmed))
            }
        }
        return days
    }

    private fun parseBlockedPackages(csv: String): Set<String> {
        if (csv.isBlank()) {
            return emptySet()
        }
        val packages = mutableSetOf<String>()
        for (item in csv.split(",")) {
            val trimmed = item.trim()
            if (trimmed.isNotEmpty()) {
                packages.add(trimmed)
            }
        }
        return packages
    }

    private fun parseMinuteOfDay(minuteOfDay: Int): LocalTime {
        val hour = minuteOfDay / 60
        val minute = minuteOfDay % 60
        return LocalTime.of(hour, minute)
    }

    private fun formatDaysOfWeek(days: Set<DayOfWeek>): String {
        return days.joinToString(",") { it.name }
    }

    private fun formatBlockedPackages(packages: Set<String>): String {
        return packages.joinToString(",")
    }

    private fun formatToMinuteOfDay(time: LocalTime): Int {
        return (time.hour * 60) + time.minute
    }

    private fun formatOptionalToMinuteOfDay(time: LocalTime?): Int? {
        if (time == null) {
            return null
        }
        return formatToMinuteOfDay(time)
    }

    private fun parseBreaks(
        breaksCsv: String,
        legacyStartMinute: Int?,
        legacyEndMinute: Int?
    ): List<BreakInterval> {
        if (breaksCsv.isNotBlank()) {
            return parseBreaksCsv(breaksCsv)
        }
        return parseLegacyBreakInterval(legacyStartMinute, legacyEndMinute)
    }

    private fun parseBreaksCsv(csv: String): List<BreakInterval> {
        val breaksList = mutableListOf<BreakInterval>()
        for (item in csv.split(",")) {
            collectParsedBreak(breaksList, item.trim())
        }
        return breaksList
    }

    private fun collectParsedBreak(destination: MutableList<BreakInterval>, item: String) {
        val parts = item.split("-")
        if (parts.size != 2) {
            return
        }
        val startTime = parseLocalTimeSafely(parts[0]) ?: return
        val endTime = parseLocalTimeSafely(parts[1]) ?: return
        destination.add(BreakInterval(startTime, endTime))
    }

    private fun parseLocalTimeSafely(raw: String): LocalTime? {
        val clean = raw.trim()
        if (clean.isEmpty()) {
            return null
        }
        return try {
            LocalTime.parse(clean)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseLegacyBreakInterval(
        startMinute: Int?,
        endMinute: Int?
    ): List<BreakInterval> {
        if (startMinute == null) {
            return emptyList()
        }
        if (endMinute == null) {
            return emptyList()
        }
        val startTime = parseMinuteOfDay(startMinute)
        val endTime = parseMinuteOfDay(endMinute)
        return listOf(BreakInterval(startTime, endTime))
    }

    private fun formatBreaks(breaks: List<BreakInterval>): String {
        return breaks.joinToString(",") { "${it.startTime}-${it.endTime}" }
    }
}
