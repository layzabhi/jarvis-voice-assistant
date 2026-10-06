package com.example.jarvis.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BirthdayDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(b: Birthday): Long

    @Query("SELECT * FROM birthdays ORDER BY month ASC, day ASC")
    suspend fun all(): List<Birthday>

    @Query("SELECT * FROM birthdays ORDER BY month ASC, day ASC")
    fun allFlow(): Flow<List<Birthday>>

    @Query("DELETE FROM birthdays WHERE id = :id")
    suspend fun delete(id: Long)
}
