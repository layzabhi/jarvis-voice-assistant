package com.example.jarvis.nlu

import java.time.ZonedDateTime
import java.util.Locale

object TimeParser {

    private val monthNames = mapOf(
        "january" to 1, "jan" to 1,
        "february" to 2, "feb" to 2,
        "march" to 3, "mar" to 3,
        "april" to 4, "apr" to 4,
        "may" to 5,
        "june" to 6, "jun" to 6,
        "july" to 7, "jul" to 7,
        "august" to 8, "aug" to 8,
        "september" to 9, "sep" to 9, "sept" to 9,
        "october" to 10, "oct" to 10,
        "november" to 11, "nov" to 11,
        "december" to 12, "dec" to 12
    )

    fun normalize(s: String): String = s.lowercase(Locale.ROOT)
        .replace("a.m.", "am")
        .replace("p.m.", "pm")
        .replace(Regex("[,.!?]"), "")
        .trim()

    // 6, 6 30, 6:30 pm, 18:45, 6 pm -> (hour, minute)
    fun clock(s: String): Pair<Int, Int>? {
        val m = Regex("([0-9]{1,2})(?:[: ]([0-9]{2}))?[ ]?(am|pm)?").find(s) ?: return null
        var h = m.groupValues[1].toInt()
        val min = m.groupValues[2].ifEmpty { "0" }.toInt()
        when (m.groupValues[3]) {
            "pm" -> if (h < 12) h += 12
            "am" -> if (h == 12) h = 0
        }
        return if (h in 0..23 && min in 0..59) h to min else null
    }

    fun whenIs(s: String, now: ZonedDateTime): ZonedDateTime? {
        Regex("in ([0-9]+) (minute|hour|day)s?").find(s)?.let {
            val n = it.groupValues[1].toLong()
            return when (it.groupValues[2]) {
                "minute" -> now.plusMinutes(n)
                "hour" -> now.plusHours(n)
                else -> now.plusDays(n)
            }
        }
        val (h, m) = clock(s.substringAfter("at ", s)) ?: return null
        var t = now.withHour(h).withMinute(m).withSecond(0).withNano(0)
        if ("tomorrow" in s) {
            t = t.plusDays(1)
        } else if (!t.isAfter(now)) {
            t = t.plusDays(1)
        }
        return t
    }

    // Handles "5 march", "march 5", "5th march", "5th of march", "march 5th", etc.
    fun monthDay(s: String): Pair<Int, Int>? {
        val cleaned = s.lowercase(Locale.ROOT)
            .replace("st", "")
            .replace("nd", "")
            .replace("rd", "")
            .replace("th", "")
            .replace(" of ", " ")
            .trim()

        // Pattern 1: Day Month (e.g. "5 march" or "25 december")
        Regex("^([0-9]{1,2}) +([a-z]+)$").find(cleaned)?.let { m ->
            val day = m.groupValues[1].toInt()
            val month = monthNames[m.groupValues[2]] ?: return null
            if (day in 1..31) return month to day
        }

        // Pattern 2: Month Day (e.g. "march 5" or "december 25")
        Regex("^([a-z]+) +([0-9]{1,2})$").find(cleaned)?.let { m ->
            val month = monthNames[m.groupValues[1]] ?: return null
            val day = m.groupValues[2].toInt()
            if (day in 1..31) return month to day
        }

        return null
    }
}
