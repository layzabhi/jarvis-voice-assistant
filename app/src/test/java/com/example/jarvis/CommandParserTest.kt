package com.example.jarvis

import com.example.jarvis.nlu.Command
import com.example.jarvis.nlu.CommandParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class CommandParserTest {

    private val fixedNow = ZonedDateTime.of(2026, 10, 6, 10, 0, 0, 0, ZoneId.of("Asia/Kolkata"))

    @Test
    fun remindTomorrow() {
        val c = CommandParser.parse("Remind me to submit the form tomorrow at 5 pm", fixedNow)
        val expectedAt = fixedNow.plusDays(1).withHour(17).withMinute(0).withSecond(0).withNano(0)
        assertEquals(Command.AddReminder("submit the form", expectedAt), c)
    }

    @Test
    fun remindInMinutes() {
        val c = CommandParser.parse("remind me to drink water in 30 minutes", fixedNow)
        val expectedAt = fixedNow.plusMinutes(30)
        assertEquals(Command.AddReminder("drink water", expectedAt), c)
    }

    @Test
    fun remindInHours() {
        val c = CommandParser.parse("remind me to check oven in 2 hours", fixedNow)
        val expectedAt = fixedNow.plusHours(2)
        assertEquals(Command.AddReminder("check oven", expectedAt), c)
    }

    @Test
    fun setAlarmVariants() {
        val c1 = CommandParser.parse("set alarm for 6:30 am", fixedNow)
        assertEquals(Command.SetAlarm(6, 30), c1)

        val c2 = CommandParser.parse("set an alarm at 7:15 pm", fixedNow)
        assertEquals(Command.SetAlarm(19, 15), c2)

        val c3 = CommandParser.parse("wake me up at 6 am", fixedNow)
        assertEquals(Command.SetAlarm(6, 0), c3)
    }

    @Test
    fun setTimerVariants() {
        val c1 = CommandParser.parse("set a timer for 5 minutes", fixedNow)
        assertEquals(Command.SetTimer(300), c1)

        val c2 = CommandParser.parse("set timer for 45 seconds", fixedNow)
        assertEquals(Command.SetTimer(45), c2)

        val c3 = CommandParser.parse("timer 1 hour", fixedNow)
        assertEquals(Command.SetTimer(3600), c3)
    }

    @Test
    fun callVariants() {
        val c1 = CommandParser.parse("call Mom", fixedNow)
        assertEquals(Command.Call("mom"), c1)

        val c2 = CommandParser.parse("dial Rahul", fixedNow)
        assertEquals(Command.Call("rahul"), c2)

        val c3 = CommandParser.parse("phone Doctor", fixedNow)
        assertEquals(Command.Call("doctor"), c3)
    }

    @Test
    fun addBirthdayVariants() {
        val c1 = CommandParser.parse("add birthday of Amit on 15 march", fixedNow)
        assertEquals(Command.AddBirthday("amit", 3, 15), c1)

        val c2 = CommandParser.parse("save Rahul's birthday on august 20", fixedNow)
        assertEquals(Command.AddBirthday("rahul", 8, 20), c2)
    }

    @Test
    fun playMusicVariants() {
        val c1 = CommandParser.parse("play Bohemian Rhapsody on spotify", fixedNow)
        assertEquals(Command.PlayMusic("bohemian rhapsody"), c1)

        val c2 = CommandParser.parse("play Coldplay", fixedNow)
        assertEquals(Command.PlayMusic("coldplay"), c2)
    }

    @Test
    fun webSearchVariants() {
        val c1 = CommandParser.parse("search for Kotlin coroutines", fixedNow)
        assertEquals(Command.WebSearch("kotlin coroutines"), c1)

        val c2 = CommandParser.parse("google nearest grocery store", fixedNow)
        assertEquals(Command.WebSearch("nearest grocery store"), c2)

        val c3 = CommandParser.parse("look up weather in Bengaluru", fixedNow)
        assertEquals(Command.WebSearch("weather in bengaluru"), c3)
    }

    @Test
    fun unknownFallback() {
        val c = CommandParser.parse("tell me a funny bedtime story", fixedNow)
        assertTrue(c is Command.Unknown)
    }
}
