package com.example.jarvis.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Reminder::class, Birthday::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {

    abstract fun reminders(): ReminderDao
    abstract fun birthdays(): BirthdayDao

    companion object {
        @Volatile
        private var INSTANCE: AppDb? = null

        fun get(ctx: Context): AppDb {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    ctx.applicationContext,
                    AppDb::class.java,
                    "jarvis.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
