package com.wellbeing.data

import com.wellbeing.core.TimeWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Repository coordinating Room persistence and an in-memory high-speed cache
 * for zero-latency accessibility interception.
 *
 * Implements Object Calisthenics: zero 'else', small pure functions.
 */
class ScheduleRepository(
    private val timeWindowDao: TimeWindowDao,
    coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    init {
        // Continuously update memory cache whenever DB updates
        coroutineScope.launch {
            timeWindowDao.getAllWindowsFlow().collect { entities ->
                cachedWindows = entities.map { TimeWindowMapper.toDomain(it) }
            }
        }
    }

    fun getAllWindowsFlow(): Flow<List<TimeWindow>> {
        return timeWindowDao.getAllWindowsFlow().map { entities ->
            entities.map { TimeWindowMapper.toDomain(it) }
        }
    }

    suspend fun getAllWindows(): List<TimeWindow> {
        return refreshCache()
    }

    suspend fun saveWindow(window: TimeWindow): Long {
        val entity = TimeWindowMapper.toEntity(window)
        val generatedId = persistEntity(entity)
        refreshCache()
        return generatedId
    }

    suspend fun deleteWindow(windowId: Long) {
        timeWindowDao.deleteById(windowId)
        refreshCache()
    }

    suspend fun refreshCache(): List<TimeWindow> {
        val domainList = timeWindowDao.getAllWindows().map { TimeWindowMapper.toDomain(it) }
        cachedWindows = domainList
        return domainList
    }

    private suspend fun persistEntity(entity: TimeWindowEntity): Long {
        if (entity.id == 0L) {
            return timeWindowDao.insertWindow(entity)
        }
        timeWindowDao.updateWindow(entity)
        return entity.id
    }

    companion object {
        @Volatile
        private var cachedWindows: List<TimeWindow> = emptyList()

        /**
         * Zero-overhead in-memory query safe for main-thread accessibility checks.
         */
        fun getActiveWindowsCached(): List<TimeWindow> {
            return cachedWindows
        }

        /**
         * For test setups or initializations.
         */
        fun updateMemoryCacheForTesting(windows: List<TimeWindow>) {
            cachedWindows = windows
        }
    }
}
