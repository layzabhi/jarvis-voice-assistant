package com.example.jarvis.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "birthdays")
data class Birthday(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val month: Int,
    val day: Int
)
