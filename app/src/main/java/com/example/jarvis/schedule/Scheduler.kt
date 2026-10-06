package com.example.jarvis.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

class Scheduler(private val ctx: Context) {

    private val am = ctx.getSystemService(AlarmManager::class.java)

    fun schedule(id: Long, kind: String, text: String, at: Long) {
        if (am == null) return

        val intent = Intent(ctx, ReminderReceiver::class.java).apply {
            putExtra("kind", kind)
            putExtra("id", id)
            putExtra("text", text)
        }

        val pi = PendingIntent.getBroadcast(
            ctx,
            (kind + id).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi) // inexact fallback
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun cancel(id: Long, kind: String) {
        if (am == null) return
        val intent = Intent(ctx, ReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            ctx,
            (kind + id).hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            am.cancel(pi)
            pi.cancel()
        }
    }
}
