package com.wellbeing.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for time restriction rules.
 */
@Dao
interface TimeWindowDao {

    @Query("SELECT * FROM time_windows ORDER BY id ASC")
    fun getAllWindowsFlow(): Flow<List<TimeWindowEntity>>

    @Query("SELECT * FROM time_windows ORDER BY id ASC")
    suspend fun getAllWindows(): List<TimeWindowEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWindow(window: TimeWindowEntity): Long

    @Update
    suspend fun updateWindow(window: TimeWindowEntity)

    @Delete
    suspend fun deleteWindow(window: TimeWindowEntity)

    @Query("DELETE FROM time_windows WHERE id = :id")
    suspend fun deleteById(id: Long)
}
