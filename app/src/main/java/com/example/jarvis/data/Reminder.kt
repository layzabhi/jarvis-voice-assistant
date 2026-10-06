package com.example.jarvis.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val triggerAt: Long,
    val done: Boolean = false
)
