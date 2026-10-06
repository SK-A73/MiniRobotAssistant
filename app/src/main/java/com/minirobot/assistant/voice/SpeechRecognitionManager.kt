package com.minirobot.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Manages real Android SpeechRecognizer for hands-free and push-to-talk voice input.
 * Accurately translates audio waveforms into text streams with robust error handling.
 */
class SpeechRecognitionManager(
    private val context: Context,
    private val onReady: () -> Unit,
    private val onRmsChanged: (Float) -> Unit,
    private val onPartialResult: (String) -> Unit,
    private val onFinalResult: (String) -> Unit,
    private val onError: (String) -> Unit
) {

    private var speechRecognizer: SpeechRecognizer? = null
    var isListening: Boolean = false
        private set

    init {
        initializeRecognizer()
    }

    private fun initializeRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "SpeechRecognizer is not available on this device")
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create SpeechRecognizer", e)
        }
    }

    fun startListening() {
        if (speechRecognizer == null) {
            initializeRecognizer()
            if (speechRecognizer == null) {
                onError("Speech recognition is unavailable on this device.")
                return
            }
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        try {
            speechRecognizer?.startListening(intent)
            isListening = true
            Log.d(TAG, "Started speech recognition")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition", e)
            isListening = false
            onError("Could not start microphone: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        if (!isListening) return
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping speech recognizer", e)
        }
        isListening = false
    }

    fun cancel() {
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling speech recognizer", e)
        }
        isListening = false
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.w(TAG, "Error destroying speech recognizer", e)
        }
        isListening = false
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "onReadyForSpeech")
                onReady()
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "onBeginningOfSpeech")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // rmsdB ranges roughly between -2 to 10
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                onRmsChanged(normalized)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "onEndOfSpeech")
                isListening = false
            }

            override fun onError(errorCode: Int) {
                isListening = false
                val errorMsg = when (errorCode) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please check microphone."
                    SpeechRecognizer.ERROR_CLIENT -> "Client speech recognition error."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
                    SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition."
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout. Please check internet connection."
                    SpeechRecognizer.ERROR_NO_MATCH -> "I didn't catch that. Please speak again."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognizer is busy. Please try again."
                    SpeechRecognizer.ERROR_SERVER -> "Server error from speech recognition."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Tap microphone to speak."
                    else -> "Voice recognition issue (Code $errorCode)."
                }
                Log.w(TAG, "SpeechRecognizer error: $errorCode - $errorMsg")
                onError(errorMsg)
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognizedText = matches?.firstOrNull()?.trim() ?: ""
                Log.d(TAG, "onResults: $recognizedText")
                if (recognizedText.isNotEmpty()) {
                    onFinalResult(recognizedText)
                } else {
                    onError("No words detected. Please try again.")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partialText = matches?.firstOrNull()?.trim() ?: ""
                if (partialText.isNotEmpty()) {
                    onPartialResult(partialText)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    companion object {
        private const val TAG = "MiniRobotSpeech"
    }
}
