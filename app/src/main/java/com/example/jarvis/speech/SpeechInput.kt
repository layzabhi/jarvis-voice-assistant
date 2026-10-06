package com.example.jarvis.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class SpeechInput(
    private val ctx: Context,
    private val onText: (String) -> Unit,
    private val onFail: (String) -> Unit,
    private val languageTag: String = "en-IN"
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null

    fun start() {
        mainHandler.post {
            destroyRecognizer()

            if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
                onFail("Speech recognition unavailable on this device")
                return@post
            }

            try {
                recognizer = SpeechRecognizer.createSpeechRecognizer(ctx).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {}
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {}

                        override fun onError(error: Int) {
                            destroyRecognizer()
                            val msg = when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH,
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Didn't catch that"
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording issue"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission needed"
                                SpeechRecognizer.ERROR_NETWORK,
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Offline speech pack needed or network issue"
                                else -> "Didn't catch that"
                            }
                            onFail(msg)
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull()?.trim() ?: ""
                            destroyRecognizer()
                            if (text.isNotEmpty()) {
                                onText(text)
                            } else {
                                onFail("Didn't catch that")
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {}
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                    putExtra("android.speech.extra.EXTRA_PREFER_OFFLINE", true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }

                recognizer?.startListening(intent)
            } catch (e: Exception) {
                destroyRecognizer()
                onFail("Error starting voice input: ${e.message}")
            }
        }
    }

    fun stop() {
        mainHandler.post {
            try {
                recognizer?.stopListening()
            } catch (_: Exception) {}
            destroyRecognizer()
        }
    }

    private fun destroyRecognizer() {
        try {
            recognizer?.destroy()
        } catch (_: Exception) {}
        recognizer = null
    }
}
