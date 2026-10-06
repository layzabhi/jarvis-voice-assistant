package com.example.jarvis

import android.content.Context
import com.example.jarvis.actions.ActionExecutor
import com.example.jarvis.data.AppDb
import com.example.jarvis.nlu.Command
import com.example.jarvis.nlu.CommandParser
import com.example.jarvis.schedule.Scheduler
import com.example.jarvis.speech.SpeechInput
import com.example.jarvis.speech.SpeechOutput
import com.example.jarvis.speech.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AssistantEngine(
    ctx: Context,
    private val onState: (Status) -> Unit
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val executor = ActionExecutor(ctx, AppDb.get(ctx), Scheduler(ctx))

    private val tts = SpeechOutput(ctx) {
        status = Status.Idle
        onState(status)
    }

    private val stt = SpeechInput(
        ctx,
        onText = { text -> onHeard(text) },
        onFail = { say(it) }
    )

    var status: Status = Status.Idle
        private set

    fun toggleListening() {
        when (status) {
            Status.Idle -> {
                status = Status.Listening
                onState(status)
                stt.start()
            }
            Status.Listening -> {
                stt.stop()
                status = Status.Idle
                onState(status)
            }
            Status.Speaking -> {
                tts.stop()
                status = Status.Idle
                onState(status)
            }
            Status.Thinking -> Unit
        }
    }

    private fun onHeard(text: String) {
        status = Status.Thinking
        onState(status)
        scope.launch {
            val cmd = CommandParser.parse(text)
            val reply = executor.run(cmd)
            say(reply)
        }
    }

    fun say(msg: String) {
        status = Status.Speaking
        onState(status)
        tts.speak(msg)
    }

    fun executeText(text: String, onComplete: ((Command, String) -> Unit)? = null) {
        status = Status.Thinking
        onState(status)
        scope.launch {
            val cmd = CommandParser.parse(text)
            val reply = executor.run(cmd)
            say(reply)
            onComplete?.invoke(cmd, reply)
        }
    }

    fun release() {
        stt.stop()
        tts.shutdown()
        scope.cancel()
    }
}
