package com.example.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TextToSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentSpokenSegmentIndex = MutableStateFlow(-1)
    val currentSpokenSegmentIndex: StateFlow<Int> = _currentSpokenSegmentIndex.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private var segmentsToSpeak: List<Pair<Int, String>> = emptyList()
    private var currentQueueIndex = 0

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("tr", "TR"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to default locale
                tts?.language = Locale.getDefault()
            }
            tts?.setSpeechRate(_speechRate.value)
            isInitialized = true
            setupProgressListener()
        } else {
            Log.e("TTSManager", "TTS initialization failed: $status")
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
                val index = utteranceId?.toIntOrNull() ?: -1
                _currentSpokenSegmentIndex.value = index
            }

            override fun onDone(utteranceId: String?) {
                currentQueueIndex++
                if (currentQueueIndex < segmentsToSpeak.size) {
                    val next = segmentsToSpeak[currentQueueIndex]
                    speakSingle(next.first, next.second)
                } else {
                    _isSpeaking.value = false
                    _currentSpokenSegmentIndex.value = -1
                }
            }

            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                _currentSpokenSegmentIndex.value = -1
            }
        })
    }

    fun speakSegments(segments: List<Pair<Int, String>>) {
        if (!isInitialized || segments.isEmpty()) return
        stop()
        segmentsToSpeak = segments
        currentQueueIndex = 0
        val first = segments[0]
        speakSingle(first.first, first.second)
    }

    fun speakText(text: String, languageCode: String = "tr") {
        if (!isInitialized || text.isBlank()) return
        stop()
        try {
            val locale = when (languageCode) {
                "en" -> Locale.US
                "de" -> Locale.GERMANY
                "es" -> Locale("es", "ES")
                "fr" -> Locale.FRANCE
                else -> Locale("tr", "TR")
            }
            tts?.language = locale
            tts?.setSpeechRate(_speechRate.value)

            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "0")
            }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "0")
            _isSpeaking.value = true
            _currentSpokenSegmentIndex.value = 0
        } catch (e: Exception) {
            Log.e("TTSManager", "Speak text error", e)
        }
    }

    private fun speakSingle(index: Int, text: String) {
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, index.toString())
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, index.toString())
    }

    fun setSpeechRate(rate: Float) {
        _speechRate.value = rate
        tts?.setSpeechRate(rate)
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e("TTSManager", "Stop error", e)
        }
        _isSpeaking.value = false
        _currentSpokenSegmentIndex.value = -1
        segmentsToSpeak = emptyList()
        currentQueueIndex = 0
    }

    fun release() {
        stop()
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("TTSManager", "Shutdown error", e)
        }
        tts = null
    }
}
