package com.example.jarvis.speech

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

class SpeechOutput(
    ctx: Context,
    private val onDone: () -> Unit
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isReady = false
    private var pendingMessage: String? = null
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(ctx.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("en", "IN")
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}

                    override fun onDone(utteranceId: String?) {
                        mainHandler.post { onDone() }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        mainHandler.post { onDone() }
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        mainHandler.post { onDone() }
                    }
                })
                isReady = true
                pendingMessage?.let { msg ->
                    pendingMessage = null
                    speak(msg)
                }
            }
        }
    }

    fun speak(text: String) {
        if (!isReady) {
            pendingMessage = text
            return
        }
        val utteranceId = UUID.randomUUID().toString()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
        mainHandler.post { onDone() }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
    }
}
