package com.example.jarvis.actions

import android.Manifest
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import com.example.jarvis.data.AppDb
import com.example.jarvis.data.Birthday
import com.example.jarvis.data.Reminder
import com.example.jarvis.nlu.Command
import com.example.jarvis.schedule.ReminderReceiver
import com.example.jarvis.schedule.Scheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Month
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ActionExecutor(
    private val ctx: Context,
    private val db: AppDb,
    private val scheduler: Scheduler
) {

    suspend fun run(cmd: Command): String = when (cmd) {
        is Command.Call -> call(cmd.name)
        is Command.SetAlarm -> setAlarm(cmd.hour, cmd.minute)
        is Command.SetTimer -> setTimer(cmd.seconds)
        is Command.AddReminder -> addReminder(cmd)
        is Command.AddBirthday -> addBirthday(cmd)
        is Command.PlayMusic -> play(cmd.query)
        is Command.WebSearch -> search(cmd.query)
        is Command.Unknown -> "Sorry, I didn't understand that"
    }

    private fun launch(i: Intent) {
        ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private suspend fun call(name: String): String {
        val matches = ContactResolver(ctx).find(name)
        if (matches.isEmpty()) return "I couldn't find $name in your contacts"
        if (matches.size > 1) return "I found ${matches.size} contacts. Please say the full name"

        val m = matches.first()
        val uri = Uri.parse("tel:" + Uri.encode(m.number))
        val hasCallPermission = ctx.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        try {
            val intent = Intent(if (hasCallPermission) Intent.ACTION_CALL else Intent.ACTION_DIAL, uri)
            launch(intent)
            return "Calling ${m.name}"
        } catch (e: Exception) {
            return "Unable to place call to ${m.name}: ${e.localizedMessage ?: "error"}"
        }
    }

    private fun setAlarm(h: Int, m: Int): String {
        try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM)
                .putExtra(AlarmClock.EXTRA_HOUR, h)
                .putExtra(AlarmClock.EXTRA_MINUTES, m)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            launch(intent)
            return "Alarm set for %02d:%02d".format(Locale.ROOT, h, m)
        } catch (e: Exception) {
            return "Failed to set alarm: ${e.localizedMessage ?: "clock app not found"}"
        }
    }

    private fun setTimer(seconds: Int): String {
        try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER)
                .putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            launch(intent)
            val desc = formatDuration(seconds)
            return "Timer set for $desc"
        } catch (e: Exception) {
            return "Failed to set timer: ${e.localizedMessage ?: "clock app not found"}"
        }
    }

    private suspend fun addReminder(cmd: Command.AddReminder): String = withContext(Dispatchers.IO) {
        val triggerAt = cmd.at.toInstant().toEpochMilli()
        val id = db.reminders().insert(
            Reminder(text = cmd.text, triggerAt = triggerAt, done = false)
        )
        scheduler.schedule(id, "reminder", cmd.text, triggerAt)

        val formatter = DateTimeFormatter.ofPattern("h:mm a, d MMM")
        val formattedTime = cmd.at.format(formatter)
        "Reminder set for $formattedTime"
    }

    private suspend fun addBirthday(cmd: Command.AddBirthday): String = withContext(Dispatchers.IO) {
        val id = db.birthdays().insert(
            Birthday(name = cmd.name, month = cmd.month, day = cmd.day)
        )
        val now = ZonedDateTime.now()
        val nextTrigger = ReminderReceiver.computeNextBirthday(cmd.month, cmd.day, now)
        scheduler.schedule(id, "birthday", cmd.name, nextTrigger.toInstant().toEpochMilli())

        val eveTrigger = nextTrigger.minusDays(1).withHour(18).withMinute(0)
        if (eveTrigger.isAfter(now)) {
            scheduler.schedule(id, "birthday_eve", cmd.name, eveTrigger.toInstant().toEpochMilli())
        }

        val monthName = try {
            Month.of(cmd.month).name.lowercase(Locale.ROOT).replaceFirstChar { it.uppercase() }
        } catch (_: Exception) {
            "${cmd.month}"
        }
        "Birthday saved for ${cmd.name} on $monthName ${cmd.day}"
    }

    private fun play(query: String): String {
        try {
            val uri = Uri.parse("spotify:search:" + Uri.encode(query))
            launch(Intent(Intent.ACTION_VIEW, uri))
            return "Playing $query on Spotify"
        } catch (_: ActivityNotFoundException) {
            return search(query)
        } catch (_: Exception) {
            return search(query)
        }
    }

    private fun search(query: String): String {
        try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
            }
            launch(intent)
            return "Searching for $query"
        } catch (_: ActivityNotFoundException) {
            try {
                val url = "https://www.google.com/search?q=" + Uri.encode(query)
                launch(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                return "Searching for $query"
            } catch (_: Exception) {
                return "Could not perform web search"
            }
        }
    }

    private fun formatDuration(seconds: Int): String {
        return when {
            seconds >= 3600 -> {
                val h = seconds / 3600
                val m = (seconds % 3600) / 60
                if (m > 0) "$h hour${if (h > 1) "s" else ""} $m minute${if (m > 1) "s" else ""}"
                else "$h hour${if (h > 1) "s" else ""}"
            }
            seconds >= 60 -> {
                val m = seconds / 60
                val s = seconds % 60
                if (s > 0) "$m minute${if (m > 1) "s" else ""} $s second${if (s > 1) "s" else ""}"
                else "$m minute${if (m > 1) "s" else ""}"
            }
            else -> "$seconds second${if (seconds > 1) "s" else ""}"
        }
    }
}
