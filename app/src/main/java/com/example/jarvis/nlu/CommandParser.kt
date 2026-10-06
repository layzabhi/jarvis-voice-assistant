package com.example.jarvis.nlu

import java.time.ZonedDateTime

object CommandParser {

    private val call = Regex("^(call|phone|dial|ring) (.+)$")
    private val alarm = Regex("^(set )?(an )?alarm (for|at) (.+)$")
    private val wakeUp = Regex("^wake me up (at|for) (.+)$")
    private val timer = Regex("^(set )?(a )?timer (for )?([0-9]+) (second|minute|hour)s?$")
    private val remind = Regex("^remind me to (.+?) ((in|at|tomorrow) .+)$")
    private val birthdayOf = Regex("^(add|set|save) birthday of (.+?) (on|for) (.+)$")
    private val personBirthday = Regex("^(add|set|save) (.+?)'?s birthday (on|for) (.+)$")
    private val music = Regex("^play (.+?)( on spotify)?$")
    private val search = Regex("^(search|google|look up) (for )?(.+)$")

    fun parse(raw: String, now: ZonedDateTime = ZonedDateTime.now()): Command {
        val t = TimeParser.normalize(raw)

        call.find(t)?.let { m ->
            return Command.Call(m.groupValues[2].trim())
        }

        alarm.find(t)?.let { m ->
            val hm = TimeParser.clock(m.groupValues[4].trim()) ?: return Command.Unknown(raw)
            return Command.SetAlarm(hm.first, hm.second)
        }

        wakeUp.find(t)?.let { m ->
            val hm = TimeParser.clock(m.groupValues[2].trim()) ?: return Command.Unknown(raw)
            return Command.SetAlarm(hm.first, hm.second)
        }

        timer.find(t)?.let { m ->
            val n = m.groupValues[4].toInt()
            val secs = when (m.groupValues[5]) {
                "hour" -> n * 3600
                "minute" -> n * 60
                else -> n
            }
            return Command.SetTimer(secs)
        }

        remind.find(t)?.let { m ->
            val at = TimeParser.whenIs(m.groupValues[2].trim(), now) ?: return Command.Unknown(raw)
            return Command.AddReminder(m.groupValues[1].trim(), at)
        }

        birthdayOf.find(t)?.let { m ->
            val name = m.groupValues[2].trim()
            val md = TimeParser.monthDay(m.groupValues[4].trim()) ?: return Command.Unknown(raw)
            return Command.AddBirthday(name, md.first, md.second)
        }

        personBirthday.find(t)?.let { m ->
            val name = m.groupValues[2].trim()
            val md = TimeParser.monthDay(m.groupValues[4].trim()) ?: return Command.Unknown(raw)
            return Command.AddBirthday(name, md.first, md.second)
        }

        music.find(t)?.let { m ->
            return Command.PlayMusic(m.groupValues[1].trim())
        }

        search.find(t)?.let { m ->
            return Command.WebSearch(m.groupValues[3].trim())
        }

        return Command.Unknown(raw)
    }
}
