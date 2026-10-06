package com.example.jarvis.nlu

import java.time.ZonedDateTime

sealed interface Command {
    data class Call(val name: String) : Command
    data class SetAlarm(val hour: Int, val minute: Int) : Command
    data class SetTimer(val seconds: Int) : Command
    data class AddReminder(val text: String, val at: ZonedDateTime) : Command
    data class AddBirthday(val name: String, val month: Int, val day: Int) : Command
    data class PlayMusic(val query: String) : Command
    data class WebSearch(val query: String) : Command
    data class Unknown(val raw: String) : Command
}
