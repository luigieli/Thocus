package com.wellbeing.data

import com.wellbeing.core.TimeWindow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleRepositoryTest {

    private class FakeTimeWindowDao : TimeWindowDao {
        val windowList = mutableListOf<TimeWindowEntity>()
        val stateFlow = MutableStateFlow<List<TimeWindowEntity>>(emptyList())

        override fun getAllWindowsFlow(): Flow<List<TimeWindowEntity>> {
            return stateFlow
        }

        override suspend fun getAllWindows(): List<TimeWindowEntity> {
            return windowList.toList()
        }

        override suspend fun insertWindow(window: TimeWindowEntity): Long {
            val generatedId = (windowList.size + 1).toLong()
            val inserted = window.copy(id = generatedId)
            windowList.add(inserted)
            stateFlow.value = windowList.toList()
            return generatedId
        }

        override suspend fun updateWindow(window: TimeWindowEntity) {
            val foundIndex = windowList.indexOfFirst { it.id == window.id }
            if (foundIndex < 0) {
                return
            }
            windowList[foundIndex] = window
            stateFlow.value = windowList.toList()
        }

        override suspend fun deleteById(id: Long) {
            windowList.removeAll { it.id == id }
            stateFlow.value = windowList.toList()
        }

        override suspend fun deleteWindow(window: TimeWindowEntity) {
            deleteById(window.id)
        }
    }

    @Test
    fun `saveWindow updates in-memory cachedWindows synchronously`() = runTest {
        val fakeDao = FakeTimeWindowDao()
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val repository = ScheduleRepository(fakeDao, TestScope(testDispatcher))

        val focusWindow = TimeWindow(
            name = "Work Session",
            enabled = true,
            daysOfWeek = setOf(DayOfWeek.MONDAY),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(17, 0),
            blockedPackages = setOf("com.instagram.android")
        )

        repository.saveWindow(focusWindow)

        val cached = ScheduleRepository.getActiveWindowsCached()
        assertEquals(1, cached.size)
        assertEquals("Work Session", cached[0].name)
        assertTrue(cached[0].blockedPackages.contains("com.instagram.android"))
    }

    @Test
    fun `deleteWindow removes window from memory cache`() = runTest {
        val fakeDao = FakeTimeWindowDao()
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val repository = ScheduleRepository(fakeDao, TestScope(testDispatcher))

        val focusWindow = TimeWindow(
            name = "Temporary",
            enabled = true,
            daysOfWeek = setOf(DayOfWeek.FRIDAY),
            startTime = LocalTime.of(14, 0),
            endTime = LocalTime.of(18, 0),
            blockedPackages = setOf("com.twitter.android")
        )

        val id = repository.saveWindow(focusWindow)
        assertEquals(1, ScheduleRepository.getActiveWindowsCached().size)

        repository.deleteWindow(id)
        assertEquals(0, ScheduleRepository.getActiveWindowsCached().size)
    }
}
