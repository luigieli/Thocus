package com.wellbeing.data

import com.wellbeing.core.TimeWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class TimeWindowMapperTest {

    @Test
    fun `mapper - correctly converts domain window to entity and back`() {
        val originalDomain = TimeWindow(
            id = 42L,
            name = "Deep Focus",
            enabled = true,
            daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            startTime = LocalTime.of(9, 30),
            endTime = LocalTime.of(17, 45),
            blockedPackages = setOf("com.instagram.android", "com.twitter.android")
        )

        val entity = TimeWindowMapper.toEntity(originalDomain)
        val mappedBack = TimeWindowMapper.toDomain(entity)

        assertEquals(originalDomain.id, mappedBack.id)
        assertEquals(originalDomain.name, mappedBack.name)
        assertEquals(originalDomain.enabled, mappedBack.enabled)
        assertEquals(originalDomain.daysOfWeek, mappedBack.daysOfWeek)
        assertEquals(originalDomain.startTime, mappedBack.startTime)
        assertEquals(originalDomain.endTime, mappedBack.endTime)
        assertEquals(originalDomain.blockedPackages, mappedBack.blockedPackages)
    }

    @Test
    fun `mapper - handles empty csv gracefully`() {
        val entity = TimeWindowEntity(
            id = 1L,
            name = "Empty Lists",
            enabled = true,
            daysOfWeekCsv = "",
            startMinuteOfDay = 0,
            endMinuteOfDay = 0,
            blockedPackagesCsv = ""
        )

        val domain = TimeWindowMapper.toDomain(entity)
        assertTrue(domain.daysOfWeek.isEmpty())
        assertTrue(domain.blockedPackages.isEmpty())
        assertEquals(LocalTime.MIDNIGHT, domain.startTime)
        assertEquals(LocalTime.MIDNIGHT, domain.endTime)
    }

    @Test
    fun `mapper - correctly maps break times`() {
        val domainWithBreak = TimeWindow(
            id = 10L,
            name = "Work with Lunch",
            enabled = true,
            daysOfWeek = setOf(DayOfWeek.MONDAY),
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(17, 0),
            blockedPackages = setOf("com.instagram.android"),
            breakStartTime = LocalTime.of(12, 0),
            breakEndTime = LocalTime.of(13, 0)
        )

        val entity = TimeWindowMapper.toEntity(domainWithBreak)
        assertEquals(720, entity.breakStartMinuteOfDay)
        assertEquals(780, entity.breakEndMinuteOfDay)

        val mappedBack = TimeWindowMapper.toDomain(entity)
        assertEquals(domainWithBreak.breakStartTime, mappedBack.breakStartTime)
        assertEquals(domainWithBreak.breakEndTime, mappedBack.breakEndTime)
        assertEquals(1, mappedBack.breakCount)
    }

    @Test
    fun `mapper - correctly serializes and deserializes multiple breaks`() {
        val domainWithMultipleBreaks = TimeWindow(
            id = 20L,
            name = "Multi-Break Study",
            enabled = true,
            daysOfWeek = setOf(DayOfWeek.TUESDAY),
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(18, 0),
            blockedPackages = setOf("com.tiktok.android"),
            breaks = listOf(
                com.wellbeing.core.BreakInterval(LocalTime.of(10, 0), LocalTime.of(10, 30)),
                com.wellbeing.core.BreakInterval(LocalTime.of(12, 0), LocalTime.of(13, 0)),
                com.wellbeing.core.BreakInterval(LocalTime.of(15, 30), LocalTime.of(16, 0))
            )
        )

        val entity = TimeWindowMapper.toEntity(domainWithMultipleBreaks)
        assertEquals("10:00-10:30,12:00-13:00,15:30-16:00", entity.breaksCsv)

        val mappedBack = TimeWindowMapper.toDomain(entity)
        assertEquals(3, mappedBack.breakCount)
        assertEquals(LocalTime.of(10, 0), mappedBack.breaks[0].startTime)
        assertEquals(LocalTime.of(10, 30), mappedBack.breaks[0].endTime)
        assertEquals(LocalTime.of(12, 0), mappedBack.breaks[1].startTime)
        assertEquals(LocalTime.of(13, 0), mappedBack.breaks[1].endTime)
        assertEquals(LocalTime.of(15, 30), mappedBack.breaks[2].startTime)
        assertEquals(LocalTime.of(16, 0), mappedBack.breaks[2].endTime)
    }
}
