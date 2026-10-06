package com.minirobot.assistant.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * Text-to-Speech manager enabling the cute robot companion to speak out responses audibly.
 */
class TextToSpeechManager(
    context: Context,
    private val onInitResult: (Boolean) -> Unit = {}
) {

    private var tts: TextToSpeech? = null
    private var isReady: Boolean = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private var activeStartCallback: (() -> Unit)? = null
    private var activeDoneCallback: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "US English language not supported by TTS, trying default")
                    tts?.language = Locale.getDefault()
                }
                tts?.setPitch(1.15f) // Slightly higher friendly cute tone
                tts?.setSpeechRate(1.05f)
                setupProgressListener()
                isReady = true
                Log.i(TAG, "TTS successfully initialized")
                onInitResult(true)
            } else {
                Log.e(TAG, "TTS initialization failed with code: $status")
                isReady = false
                onInitResult(false)
            }
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                mainHandler.post {
                    activeStartCallback?.invoke()
                }
            }

            override fun onDone(utteranceId: String?) {
                mainHandler.post {
                    activeDoneCallback?.invoke()
                }
            }

            override fun onError(utteranceId: String?) {
                mainHandler.post {
                    activeDoneCallback?.invoke()
                }
            }
        })
    }

    /**
     * Speaks the given text audibly. Calls onStart when vocalization begins and onDone when finished.
     */
    fun speak(
        text: String,
        onStart: () -> Unit = {},
        onDone: () -> Unit = {}
    ) {
        if (!isReady || tts == null) {
            Log.w(TAG, "TTS not ready, skipping speak")
            onDone()
            return
        }

        activeStartCallback = onStart
        activeDoneCallback = onDone

        val utteranceId = "robot_speech_${System.currentTimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping TTS", e)
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isReady = false
        } catch (e: Exception) {
            Log.w(TAG, "Error shutting down TTS", e)
        }
    }

    companion object {
        private const val TAG = "MiniRobotTTS"
    }
}
