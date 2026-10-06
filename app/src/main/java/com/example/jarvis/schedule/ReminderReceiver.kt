package com.example.jarvis.schedule

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.jarvis.MainActivity
import com.example.jarvis.R
import com.example.jarvis.data.AppDb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Year
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra("kind") ?: "reminder"
        val id = intent.getLongExtra("id", -1L)
        val text = intent.getStringExtra("text") ?: "Notification"

        val nm = context.getSystemService(NotificationManager::class.java)
        createChannelIfNeeded(nm)

        val title = when (kind) {
            "birthday" -> "Birthday Today! 🎉"
            "birthday_eve" -> "Upcoming Birthday 🎂"
            else -> "Reminder ⏰"
        }

        val message = when (kind) {
            "birthday" -> "It's $text's birthday today!"
            "birthday_eve" -> "Tomorrow is $text's birthday!"
            else -> text
        }

        val openIntent = Intent(context, MainActivity::class.java)
        val contentPi = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .build()

        val notificationId = ((kind + id).hashCode() and 0x7FFFFFFF)
        nm?.notify(notificationId, notification)

        val db = AppDb.get(context)
        val scheduler = Scheduler(context)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (kind) {
                    "reminder" -> {
                        if (id > 0) {
                            db.reminders().markDone(id)
                        }
                    }
                    "birthday" -> {
                        if (id > 0) {
                            val birthday = db.birthdays().all().find { it.id == id }
                            if (birthday != null) {
                                val nextTrigger = computeNextBirthday(birthday.month, birthday.day, ZonedDateTime.now())
                                scheduler.schedule(birthday.id, "birthday", birthday.name, nextTrigger.toInstant().toEpochMilli())

                                val eveTrigger = nextTrigger.minusDays(1).withHour(18).withMinute(0)
                                if (eveTrigger.isAfter(ZonedDateTime.now())) {
                                    scheduler.schedule(birthday.id, "birthday_eve", birthday.name, eveTrigger.toInstant().toEpochMilli())
                                }
                            }
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun createChannelIfNeeded(nm: NotificationManager?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm != null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Jarvis Reminders & Birthdays",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for Jarvis reminders and birthday events"
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "jarvis_reminders"

        fun computeNextBirthday(month: Int, day: Int, now: ZonedDateTime): ZonedDateTime {
            var targetYear = now.year
            val adjustedDay = adjustDayForLeapYear(targetYear, month, day)
            var target = now.withYear(targetYear)
                .withMonth(month)
                .withDayOfMonth(adjustedDay)
                .withHour(9)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)

            if (!target.isAfter(now)) {
                targetYear += 1
                val nextAdjustedDay = adjustDayForLeapYear(targetYear, month, day)
                target = target.withYear(targetYear)
                    .withMonth(month)
                    .withDayOfMonth(nextAdjustedDay)
            }
            return target
        }

        private fun adjustDayForLeapYear(year: Int, month: Int, day: Int): Int {
            if (month == 2 && day == 29) {
                return if (Year.isLeap(year.toLong())) 29 else 28
            }
            return day
        }
    }
}
