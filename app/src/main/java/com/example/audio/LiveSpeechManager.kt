package com.example.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class LiveSegment(
    val speakerId: String,
    val speakerName: String,
    val speakerColorHex: String,
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long
)

class LiveSpeechManager(private val context: Context) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var selectedLanguageTag: String = "tr-TR"

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _liveSegments = MutableStateFlow<List<LiveSegment>>(emptyList())
    val liveSegments: StateFlow<List<LiveSegment>> = _liveSegments.asStateFlow()

    private val _currentSpeakerId = MutableStateFlow("speaker_1")
    val currentSpeakerId: StateFlow<String> = _currentSpeakerId.asStateFlow()

    private val _currentSpeakerName = MutableStateFlow("Konuşmacı 1")
    val currentSpeakerName: StateFlow<String> = _currentSpeakerName.asStateFlow()

    private val _currentSpeakerColorHex = MutableStateFlow("#6366F1")
    val currentSpeakerColorHex: StateFlow<String> = _currentSpeakerColorHex.asStateFlow()

    private var sessionStartTimeMs = 0L
    private var segmentStartTimeMs = 0L

    private var speakerPresets = listOf(
        Triple("speaker_1", "Konuşmacı 1", "#6366F1"),
        Triple("speaker_2", "Konuşmacı 2", "#06B6D4"),
        Triple("speaker_3", "Konuşmacı 3", "#10B981"),
        Triple("speaker_4", "Konuşmacı 4", "#F59E0B")
    )
    private var currentSpeakerIndex = 0

    fun setSpeakerPresets(presets: List<Triple<String, String, String>>) {
        if (presets.isNotEmpty()) {
            speakerPresets = presets
            currentSpeakerIndex = 0
            val first = presets[0]
            _currentSpeakerId.value = first.first
            _currentSpeakerName.value = first.second
            _currentSpeakerColorHex.value = first.third
        }
    }

    fun setLanguage(languageTag: String) {
        selectedLanguageTag = languageTag
    }

    fun startListening(startTimeOffsetMs: Long = 0L) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w("LiveSpeech", "Speech recognition not available on device")
            return
        }

        sessionStartTimeMs = System.currentTimeMillis() - startTimeOffsetMs
        segmentStartTimeMs = startTimeOffsetMs
        isListening = true
        _partialText.value = ""

        initializeRecognizer()
    }

    private fun initializeRecognizer() {
        if (!isListening) return

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLanguageTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("LiveSpeech", "Error initializing recognizer", e)
        }
    }

    private fun createListener(): RecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {
            if (segmentStartTimeMs == 0L) {
                segmentStartTimeMs = (System.currentTimeMillis() - sessionStartTimeMs).coerceAtLeast(0L)
            }
        }
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            Log.d("LiveSpeech", "Recognizer error code: $error")
            if (isListening) {
                // Restart listening smoothly after timeout / silence
                try {
                    initializeRecognizer()
                } catch (e: Exception) {
                    Log.e("LiveSpeech", "Restart after error failed", e)
                }
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val resultText = matches?.firstOrNull()?.trim()
            if (!resultText.isNullOrBlank()) {
                commitSegment(resultText)
            }
            _partialText.value = ""

            if (isListening) {
                initializeRecognizer()
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()?.trim()
            if (!text.isNullOrBlank()) {
                _partialText.value = text
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun commitSegment(text: String) {
        val nowMs = (System.currentTimeMillis() - sessionStartTimeMs).coerceAtLeast(segmentStartTimeMs + 1000L)
        val segment = LiveSegment(
            speakerId = _currentSpeakerId.value,
            speakerName = _currentSpeakerName.value,
            speakerColorHex = _currentSpeakerColorHex.value,
            text = text,
            startTimeMs = segmentStartTimeMs,
            endTimeMs = nowMs
        )
        _liveSegments.value = _liveSegments.value + segment
        segmentStartTimeMs = nowMs
        _partialText.value = ""
    }

    fun addManualSegment(text: String, startMs: Long, endMs: Long) {
        val segment = LiveSegment(
            speakerId = _currentSpeakerId.value,
            speakerName = _currentSpeakerName.value,
            speakerColorHex = _currentSpeakerColorHex.value,
            text = text,
            startTimeMs = startMs,
            endTimeMs = endMs
        )
        _liveSegments.value = _liveSegments.value + segment
    }

    fun switchSpeaker() {
        currentSpeakerIndex = (currentSpeakerIndex + 1) % speakerPresets.size
        val preset = speakerPresets[currentSpeakerIndex]
        _currentSpeakerId.value = preset.first
        _currentSpeakerName.value = preset.second
        _currentSpeakerColorHex.value = preset.third
        segmentStartTimeMs = (System.currentTimeMillis() - sessionStartTimeMs).coerceAtLeast(0L)
    }

    fun setSpeaker(id: String, name: String, colorHex: String) {
        _currentSpeakerId.value = id
        _currentSpeakerName.value = name
        _currentSpeakerColorHex.value = colorHex
        segmentStartTimeMs = (System.currentTimeMillis() - sessionStartTimeMs).coerceAtLeast(0L)
    }

    fun stopListening() {
        isListening = false
        if (_partialText.value.isNotBlank()) {
            commitSegment(_partialText.value)
        }
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("LiveSpeech", "Stop recognizer error", e)
        } finally {
            speechRecognizer = null
        }
    }

    fun clear() {
        stopListening()
        _liveSegments.value = emptyList()
        _partialText.value = ""
        currentSpeakerIndex = 0
        val preset = speakerPresets[0]
        _currentSpeakerId.value = preset.first
        _currentSpeakerName.value = preset.second
        _currentSpeakerColorHex.value = preset.third
    }
}
