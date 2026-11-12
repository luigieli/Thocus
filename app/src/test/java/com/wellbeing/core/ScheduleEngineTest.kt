package com.wellbeing.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ScheduleEngineTest {

    private val targetApp = "com.instagram.android"
    private val allowedApp = "com.android.calculator2"

    @Test
    fun `standard window - blocks target app during active hours on scheduled days`() {
        // Monday to Friday: 09:00 to 17:00
        val window = TimeWindow(
            name = "Work Hours",
            daysOfWeek = setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
            ),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(17, 0),
            blockedPackages = setOf(targetApp)
        )

        // Monday at 10:30 AM -> should block targetApp, allow others
        val mondayActive = LocalDateTime.of(LocalDate.of(2026, 10, 26), LocalTime.of(10, 30))
        assertTrue(ScheduleEngine.isPackageBlocked(targetApp, listOf(window), mondayActive))
        assertFalse(ScheduleEngine.isPackageBlocked(allowedApp, listOf(window), mondayActive))

        // Monday at 08:59 AM (before start) -> should not block
        val mondayBefore = LocalDateTime.of(LocalDate.of(2026, 10, 26), LocalTime.of(8, 59))
        assertFalse(ScheduleEngine.isPackageBlocked(targetApp, listOf(window), mondayBefore))

        // Monday at 17:00 (exact end) -> should not block (exclusive upper boundary)
        val mondayEnd = LocalDateTime.of(LocalDate.of(2026, 10, 26), LocalTime.of(17, 0))
        assertFalse(ScheduleEngine.isPackageBlocked(targetApp, listOf(window), mondayEnd))

        // Saturday at 10:30 AM -> non-scheduled day, should not block
        val saturday = LocalDateTime.of(LocalDate.of(2026, 10, 31), LocalTime.of(10, 30))
        assertFalse(ScheduleEngine.isPackageBlocked(targetApp, listOf(window), saturday))
    }

    @Test
    fun `overnight window - properly handles midnight crossing and next-day second half`() {
        // Sunday night: 22:00 to Monday morning: 06:00
        val sundayNightWindow = TimeWindow(
            name = "Sunday Night Sleep",
            daysOfWeek = setOf(DayOfWeek.SUNDAY),
            startTime = LocalTime.of(22, 0),
            endTime = LocalTime.of(6, 0),
            blockedPackages = setOf(targetApp)
        )

        // 2026-10-25 was a Sunday
        // Sunday at 23:30 -> first half of overnight window -> should block
        val sundayNight = LocalDateTime.of(LocalDate.of(2026, 10, 25), LocalTime.of(23, 30))
        assertTrue(ScheduleEngine.isPackageBlocked(targetApp, listOf(sundayNightWindow), sundayNight))

        // Monday (2026-10-26) at 03:00 -> second half of overnight window -> should block
        val mondayMorningEarly = LocalDateTime.of(LocalDate.of(2026, 10, 26), LocalTime.of(3, 0))
        assertTrue(ScheduleEngine.isPackageBlocked(targetApp, listOf(sundayNightWindow), mondayMorningEarly))

        // Monday at 06:01 -> outside overnight window -> should not block
        val mondayMorningLate = LocalDateTime.of(LocalDate.of(2026, 10, 26), LocalTime.of(6, 1))
        assertFalse(ScheduleEngine.isPackageBlocked(targetApp, listOf(sundayNightWindow), mondayMorningLate))
    }

    @Test
    fun `findNextBoundary - correctly identifies next start or end transition`() {
        val window = TimeWindow(
            name = "Evening Focus",
            daysOfWeek = setOf(DayOfWeek.MONDAY),
            startTime = LocalTime.of(18, 0),
            endTime = LocalTime.of(21, 0),
            blockedPackages = setOf(targetApp)
        )

        // Case 1: Before window on Monday at 15:00 -> Next boundary is Monday 18:00
        val mondayBefore = LocalDateTime.of(LocalDate.of(2026, 10, 26), LocalTime.of(15, 0))
        val nextBoundary1 = ScheduleEngine.findNextBoundary(listOf(window), mondayBefore)
        assertNotNull(nextBoundary1)
        assertEquals(LocalDateTime.of(LocalDate.of(2026, 10, 26), LocalTime.of(18, 0)), nextBoundary1)

        // Case 2: Inside window on Monday at 19:00 -> Next boundary is Monday 21:00 (window end)
        val mondayInside = LocalDateTime.of(LocalDate.of(2026, 10, 26), LocalTime.of(19, 0))
        val nextBoundary2 = ScheduleEngine.findNextBoundary(listOf(window), mondayInside)
        assertNotNull(nextBoundary2)
        assertEquals(LocalDateTime.of(LocalDate.of(2026, 10, 26), LocalTime.of(21, 0)), nextBoundary2)

        // Case 3: Empty windows list -> Returns null
        assertNull(ScheduleEngine.findNextBoundary(emptyList(), mondayBefore))
    }

    @Test
    fun `calculateActiveFocusRemainingMinutes - accurately returns remaining minutes`() {
        val window = TimeWindow(
            name = "Afternoon Study",
            daysOfWeek = setOf(DayOfWeek.MONDAY),
            startTime = LocalTime.of(14, 0),
            endTime = LocalTime.of(16, 30),
            blockedPackages = setOf(targetApp)
        )

        // Monday at 15:00 -> 1 hour 30 min remaining = 90 min
        val mondayActive = LocalDateTime.of(LocalDate.of(2026, 10, 26), LocalTime.of(15, 0))
        val remaining = ScheduleEngine.calculateActiveFocusRemainingMinutes(listOf(window), mondayActive)
        assertEquals(90L, remaining)

        // Monday at 17:00 -> not active -> returns null
        val mondayInactive = LocalDateTime.of(LocalDate.of(2026, 10, 26), LocalTime.of(17, 0))
        assertNull(ScheduleEngine.calculateActiveFocusRemainingMinutes(listOf(window), mondayInactive))
    }

    @Test
    fun `splitWithBreak - splits window into two valid segments`() {
        val baseWindow = TimeWindow(
            name = "Work Day",
            daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(17, 0),
            blockedPackages = setOf(targetApp)
        )

        val segments = ScheduleEngine.splitWithBreak(
            baseWindow = baseWindow,
            breakStart = LocalTime.of(12, 0),
            breakEnd = LocalTime.of(13, 0)
        )

        assertEquals(2, segments.size)
        assertEquals(LocalTime.of(8, 0), segments[0].startTime)
        assertEquals(LocalTime.of(12, 0), segments[0].endTime)
        assertEquals(LocalTime.of(13, 0), segments[1].startTime)
        assertEquals(LocalTime.of(17, 0), segments[1].endTime)
        assertEquals(baseWindow.daysOfWeek, segments[0].daysOfWeek)
        assertEquals(baseWindow.blockedPackages, segments[0].blockedPackages)
    }

    @Test
    fun `focus window with break - blocks before and after break, pauses during break`() {
        val window = TimeWindow(
            name = "Work with Lunch Break",
            daysOfWeek = setOf(DayOfWeek.MONDAY),
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(17, 0),
            blockedPackages = setOf(targetApp),
            breakStartTime = LocalTime.of(12, 0),
            breakEndTime = LocalTime.of(13, 0)
        )

        assertEquals(1, window.breakCount)
        assertTrue(window.hasBreak)

        // Monday at 10:00 (before break) -> should block
        val morningActive = LocalDateTime.of(LocalDate.of(2026, 10, 26), LocalTime.of(10, 0))
        assertTrue(ScheduleEngine.isPackageBlocked(targetApp, listOf(window), morningActive))

        // Monday at 12:30 (during break) -> should NOT block
        val lunchBreak = LocalDateTime.of(LocalDate.of(2026, 10, 26), LocalTime.of(12, 30))
        assertFalse(ScheduleEngine.isPackageBlocked(targetApp, listOf(window), lunchBreak))

        // Monday at 14:00 (after break) -> should block
        val afternoonActive = LocalDateTime.of(LocalDate.of(2026, 10, 26), LocalTime.of(14, 0))
        assertTrue(ScheduleEngine.isPackageBlocked(targetApp, listOf(window), afternoonActive))
    }

    @Test
    fun `focus window with break - boundary calculations include break start and break end`() {
        val window = TimeWindow(
            name = "Work with Break",
            daysOfWeek = setOf(DayOfWeek.MONDAY),
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(17, 0),
            blockedPackages = setOf(targetApp),
            breakStartTime = LocalTime.of(12, 0),
            breakEndTime = LocalTime.of(13, 0)
        )

        // Monday at 11:00 -> Next boundary is break start at 12:00
        val monday11 = LocalDateTime.of(LocalDate.of(2026, 10, 26), LocalTime.of(11, 0))
        val boundaryBeforeBreak = ScheduleEngine.findNextBoundary(listOf(window), monday11)
        assertEquals(LocalDateTime.of(LocalDate.of(2026, 10, 26), LocalTime.of(12, 0)), boundaryBeforeBreak)

        // Monday at 12:15 -> Next boundary is break end at 13:00
        val monday1215 = LocalDateTime.of(LocalDate.of(2026, 10, 26), LocalTime.of(12, 15))
        val boundaryDuringBreak = ScheduleEngine.findNextBoundary(listOf(window), monday1215)
        assertEquals(LocalDateTime.of(LocalDate.of(2026, 10, 26), LocalTime.of(13, 0)), boundaryDuringBreak)

        // Monday at 15:00 -> Next boundary is window end at 17:00
        val monday15 = LocalDateTime.of(LocalDate.of(2026, 10, 26), LocalTime.of(15, 0))
        val boundaryAfterBreak = ScheduleEngine.findNextBoundary(listOf(window), monday15)
        assertEquals(LocalDateTime.of(LocalDate.of(2026, 10, 26), LocalTime.of(17, 0)), boundaryAfterBreak)
    }

    @Test
    fun `focus window with break - calculates remaining focus minutes before break and after break`() {
        val window = TimeWindow(
            name = "Work with Break",
            daysOfWeek = setOf(DayOfWeek.MONDAY),
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(17, 0),
            blockedPackages = setOf(targetApp),
            breakStartTime = LocalTime.of(12, 0),
            breakEndTime = LocalTime.of(13, 0)
        )

        // Monday at 11:00 -> 60 min remaining until break start (12:00)
        val monday11 = LocalDateTime.of(LocalDate.of(2026, 10, 26), LocalTime.of(11, 0))
        val remainingMorning = ScheduleEngine.calculateActiveFocusRemainingMinutes(listOf(window), monday11)
        assertEquals(60L, remainingMorning)

        // Monday at 12:30 -> in break -> null
        val monday1230 = LocalDateTime.of(LocalDate.of(2026, 10, 26), LocalTime.of(12, 30))
        val remainingBreak = ScheduleEngine.calculateActiveFocusRemainingMinutes(listOf(window), monday1230)
        assertNull(remainingBreak)

        // Monday at 15:00 -> 120 min remaining until window end (17:00)
        val monday15 = LocalDateTime.of(LocalDate.of(2026, 10, 26), LocalTime.of(15, 0))
        val remainingAfternoon = ScheduleEngine.calculateActiveFocusRemainingMinutes(listOf(window), monday15)
        assertEquals(120L, remainingAfternoon)
    }

    @Test
    fun `focus window with multiple breaks - pauses during any break and reports correct break count`() {
        val window = TimeWindow(
            name = "Work with Multiple Breaks",
            daysOfWeek = setOf(DayOfWeek.MONDAY),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(17, 0),
            blockedPackages = setOf(targetApp),
            breaks = listOf(
                BreakInterval(LocalTime.of(10, 30), LocalTime.of(11, 0)),
                BreakInterval(LocalTime.of(13, 0), LocalTime.of(14, 0)),
                BreakInterval(LocalTime.of(15, 30), LocalTime.of(16, 0))
            )
        )

        assertEquals(3, window.breakCount)
        assertTrue(window.hasBreak)

        // 10:00 -> Active before Break 1
        val t1 = LocalDateTime.of(LocalDate.of(2026, 10, 26), LocalTime.of(10, 0))
        assertTrue(ScheduleEngine.isPackageBlocked(targetApp, listOf(window), t1))
        assertEquals(30L, ScheduleEngine.calculateActiveFocusRemainingMinutes(listOf(window), t1))

        // 10:45 -> During Break 1
        val t2 = LocalDateTime.of(LocalDate.of(2026, 10, 26), LocalTime.of(10, 45))
        assertFalse(ScheduleEngine.isPackageBlocked(targetApp, listOf(window), t2))
        assertNull(ScheduleEngine.calculateActiveFocusRemainingMinutes(listOf(window), t2))

        // 12:00 -> Active between Break 1 and Break 2 (next break is 13:00 -> 60 min remaining)
        val t3 = LocalDateTime.of(LocalDate.of(2026, 10, 26), LocalTime.of(12, 0))
        assertTrue(ScheduleEngine.isPackageBlocked(targetApp, listOf(window), t3))
        assertEquals(60L, ScheduleEngine.calculateActiveFocusRemainingMinutes(listOf(window), t3))

        // 13:30 -> During Break 2
        val t4 = LocalDateTime.of(LocalDate.of(2026, 10, 26), LocalTime.of(13, 30))
        assertFalse(ScheduleEngine.isPackageBlocked(targetApp, listOf(window), t4))

        // 16:30 -> Active after Break 3 (window ends at 17:00 -> 30 min remaining)
        val t5 = LocalDateTime.of(LocalDate.of(2026, 10, 26), LocalTime.of(16, 30))
        assertTrue(ScheduleEngine.isPackageBlocked(targetApp, listOf(window), t5))
        assertEquals(30L, ScheduleEngine.calculateActiveFocusRemainingMinutes(listOf(window), t5))
    }

    @Test
    fun `all day 24h window - strictly blocks except during allowed break windows`() {
        // User wants to ONLY use app 06:00-08:00, 12:00-14:00, 19:00-23:00
        val allDayWindow = TimeWindow(
            name = "Strict All Day",
            isAllDay = true,
            daysOfWeek = setOf(DayOfWeek.MONDAY),
            startTime = LocalTime.MIDNIGHT,
            endTime = LocalTime.MIDNIGHT,
            blockedPackages = setOf(targetApp),
            breaks = listOf(
                BreakInterval(LocalTime.of(6, 0), LocalTime.of(8, 0)),
                BreakInterval(LocalTime.of(12, 0), LocalTime.of(14, 0)),
                BreakInterval(LocalTime.of(19, 0), LocalTime.of(23, 0))
            )
        )

        val date = LocalDate.of(2026, 10, 26) // Monday

        // 03:00 -> Outside allowed intervals -> BLOCKED
        assertTrue(ScheduleEngine.isPackageBlocked(targetApp, listOf(allDayWindow), date.atTime(3, 0)))

        // 07:00 -> Inside Break 1 (06:00-08:00) -> ALLOWED
        assertFalse(ScheduleEngine.isPackageBlocked(targetApp, listOf(allDayWindow), date.atTime(7, 0)))

        // 10:00 -> Outside allowed intervals -> BLOCKED
        assertTrue(ScheduleEngine.isPackageBlocked(targetApp, listOf(allDayWindow), date.atTime(10, 0)))

        // 13:00 -> Inside Break 2 (12:00-14:00) -> ALLOWED
        assertFalse(ScheduleEngine.isPackageBlocked(targetApp, listOf(allDayWindow), date.atTime(13, 0)))

        // 17:00 -> Outside allowed intervals -> BLOCKED
        assertTrue(ScheduleEngine.isPackageBlocked(targetApp, listOf(allDayWindow), date.atTime(17, 0)))

        // 20:00 -> Inside Break 3 (19:00-23:00) -> ALLOWED
        assertFalse(ScheduleEngine.isPackageBlocked(targetApp, listOf(allDayWindow), date.atTime(20, 0)))

        // 23:30 -> Outside allowed intervals -> BLOCKED
        assertTrue(ScheduleEngine.isPackageBlocked(targetApp, listOf(allDayWindow), date.atTime(23, 30)))
    }

    @Test
    fun `all day window with midnight-ending break - unblocks during 23-00 to 00-00 including 23-38`() {
        val window = TimeWindow(
            name = "All Day with Midnight Break",
            isAllDay = true,
            daysOfWeek = setOf(DayOfWeek.MONDAY),
            startTime = LocalTime.MIDNIGHT,
            endTime = LocalTime.MIDNIGHT,
            blockedPackages = setOf(targetApp),
            breaks = listOf(
                BreakInterval(LocalTime.of(23, 0), LocalTime.of(0, 0))
            )
        )

        val monday = LocalDate.of(2026, 10, 26)

        // 22:50 -> before break -> BLOCKED
        assertTrue(ScheduleEngine.isPackageBlocked(targetApp, listOf(window), monday.atTime(22, 50)))

        // 23:00 -> exact start of break -> ALLOWED
        assertFalse(ScheduleEngine.isPackageBlocked(targetApp, listOf(window), monday.atTime(23, 0)))

        // 23:38 -> inside midnight-ending break -> ALLOWED (Bug fix verification)
        assertFalse(ScheduleEngine.isPackageBlocked(targetApp, listOf(window), monday.atTime(23, 38)))

        // 23:59 -> last minute of day inside break -> ALLOWED
        assertFalse(ScheduleEngine.isPackageBlocked(targetApp, listOf(window), monday.atTime(23, 59)))

        // Next boundary from 23:38 is Tuesday 00:00 (break end)
        val nextBoundary = ScheduleEngine.findNextBoundary(listOf(window), monday.atTime(23, 38))
        assertEquals(monday.plusDays(1).atStartOfDay(), nextBoundary)
    }
}
