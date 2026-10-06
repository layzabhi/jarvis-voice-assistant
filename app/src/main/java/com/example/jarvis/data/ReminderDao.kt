package com.example.jarvis.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(r: Reminder): Long

    @Query("SELECT * FROM reminders WHERE done = 0 AND triggerAt > :now ORDER BY triggerAt ASC")
    suspend fun pending(now: Long): List<Reminder>

    @Query("UPDATE reminders SET done = 1 WHERE id = :id")
    suspend fun markDone(id: Long)

    @Query("SELECT * FROM reminders ORDER BY triggerAt DESC")
    fun allFlow(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE done = 0 ORDER BY triggerAt ASC")
    fun activeFlow(): Flow<List<Reminder>>

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: Long)
}
