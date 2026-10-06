package com.example.jarvis.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.jarvis.data.AppDb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val scheduler = Scheduler(context)
        val db = AppDb.get(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = System.currentTimeMillis()
                val nowZdt = ZonedDateTime.now()

                // Reschedule pending reminders
                val pendingReminders = db.reminders().pending(now)
                for (r in pendingReminders) {
                    scheduler.schedule(r.id, "reminder", r.text, r.triggerAt)
                }

                // Reschedule all birthdays
                val birthdays = db.birthdays().all()
                for (b in birthdays) {
                    val nextTrigger = ReminderReceiver.computeNextBirthday(b.month, b.day, nowZdt)
                    scheduler.schedule(b.id, "birthday", b.name, nextTrigger.toInstant().toEpochMilli())

                    val eveTrigger = nextTrigger.minusDays(1).withHour(18).withMinute(0)
                    if (eveTrigger.isAfter(nowZdt)) {
                        scheduler.schedule(b.id, "birthday_eve", b.name, eveTrigger.toInstant().toEpochMilli())
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
