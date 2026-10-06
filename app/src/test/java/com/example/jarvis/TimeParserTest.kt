package com.example.jarvis

import com.example.jarvis.nlu.TimeParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class TimeParserTest {

    private val fixedNow = ZonedDateTime.of(2026, 10, 6, 10, 0, 0, 0, ZoneId.of("Asia/Kolkata"))

    @Test
    fun normalize() {
        val normalized = TimeParser.normalize("Remind me at 5:00 p.m. please!")
        assertEquals("remind me at 5:00 pm please", normalized)
    }

    @Test
    fun clockParsing() {
        assertEquals(Pair(6, 0), TimeParser.clock("6"))
        assertEquals(Pair(6, 30), TimeParser.clock("6 30"))
        assertEquals(Pair(6, 30), TimeParser.clock("6:30"))
        assertEquals(Pair(18, 30), TimeParser.clock("6:30 pm"))
        assertEquals(Pair(6, 30), TimeParser.clock("6:30 am"))
        assertEquals(Pair(18, 45), TimeParser.clock("18:45"))
        assertEquals(Pair(0, 0), TimeParser.clock("12:00 am"))
        assertEquals(Pair(12, 0), TimeParser.clock("12:00 pm"))
    }

    @Test
    fun monthDayParsing() {
        assertEquals(Pair(3, 5), TimeParser.monthDay("5 march"))
        assertEquals(Pair(3, 5), TimeParser.monthDay("march 5"))
        assertEquals(Pair(3, 5), TimeParser.monthDay("5th march"))
        assertEquals(Pair(3, 5), TimeParser.monthDay("march 5th"))
        assertEquals(Pair(8, 15), TimeParser.monthDay("15 august"))
        assertEquals(Pair(12, 25), TimeParser.monthDay("december 25"))
        assertNull(TimeParser.monthDay("random date string"))
    }

    @Test
    fun whenIsRelative() {
        val in10Min = TimeParser.whenIs("in 10 minutes", fixedNow)
        assertNotNull(in10Min)
        assertEquals(fixedNow.plusMinutes(10), in10Min)

        val in3Hours = TimeParser.whenIs("in 3 hours", fixedNow)
        assertNotNull(in3Hours)
        assertEquals(fixedNow.plusHours(3), in3Hours)
    }

    @Test
    fun whenIsTomorrow() {
        val tomorrow5pm = TimeParser.whenIs("tomorrow at 5 pm", fixedNow)
        assertNotNull(tomorrow5pm)
        assertEquals(fixedNow.plusDays(1).withHour(17).withMinute(0).withSecond(0).withNano(0), tomorrow5pm)
    }
}
