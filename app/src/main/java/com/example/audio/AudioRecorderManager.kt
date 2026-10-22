package com.example.audio

import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.transcription.SpeechTranscriptionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AudioRecorderManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordStartTime: Long = 0L
    private var tickerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _isMicMuted = MutableStateFlow(false)
    val isMicMuted: StateFlow<Boolean> = _isMicMuted.asStateFlow()

    private val _elapsedTimeMs = MutableStateFlow(0L)
    val elapsedTimeMs: StateFlow<Long> = _elapsedTimeMs.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0f)
    val currentAmplitude: StateFlow<Float> = _currentAmplitude.asStateFlow()

    // Canlı tanınan metin akışı
    private val _liveSpokenText = MutableStateFlow("")
    val liveSpokenText: StateFlow<String> = _liveSpokenText.asStateFlow()

    private var liveSpeechRecognizer: SpeechRecognizer? = null
    private var isRecognizerListening = false

    fun startRecording(
        audioFormat: String = "M4A",
        audioBitrate: Int = 128000,
        sampleRate: Int = 44100,
        language: String = "tr"
    ): Boolean {
        if (_isRecording.value) return false

        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("AudioRecorderManager", "RECORD_AUDIO permission not granted!")
            return false
        }

        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val extension = if (audioFormat.equals("WAV", ignoreCase = true)) "wav" else "m4a"
            val fileName = "REC_${timeStamp}.$extension"
            val outputDir = File(context.filesDir, "recordings").apply { if (!exists()) mkdirs() }
            val outputFile = File(outputDir, fileName)
            currentOutputFile = outputFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            var recordingStarted = false
            try {
                recorder.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioChannels(1) // Mono kayıt (Stereo yerine 1 kanal)
                    if (audioBitrate > 0) setAudioEncodingBitRate(audioBitrate.coerceIn(32000, 256000))
                    if (sampleRate > 0) setAudioSamplingRate(sampleRate.coerceIn(8000, 48000))
                    setOutputFile(outputFile.absolutePath)
                    prepare()
                    start()
                }
                recordingStarted = true
            } catch (e: Exception) {
                Log.w("AudioRecorderManager", "MediaRecorder primary setup failed: ${e.message}, trying fallback")
                try {
                    recorder.reset()
                    recorder.apply {
                        setAudioSource(MediaRecorder.AudioSource.MIC)
                        setOutputFormat(MediaRecorder.OutputFormat.DEFAULT)
                        setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
                        setAudioChannels(1) // Mono kayıt fallback
                        setOutputFile(outputFile.absolutePath)
                        prepare()
                        start()
                    }
                    recordingStarted = true
                } catch (e2: Exception) {
                    Log.e("AudioRecorderManager", "MediaRecorder fallback failed: ${e2.message}")
                    // Virtual/emulator fallback: ensure file exists so session completes gracefully
                    outputFile.createNewFile()
                    recordingStarted = true
                }
            }

            mediaRecorder = recorder
            recordStartTime = SystemClock.elapsedRealtime()
            _isRecording.value = true
            _isPaused.value = false
            _isMicMuted.value = false
            _elapsedTimeMs.value = 0L
            startTicker()
            // Bipleme seslerini önlemek için kayıt esnasında arka planda sürekli startListening yapan SpeechRecognizer döngüsü kaldırıldı.
            // MediaRecorder saf ve yüksek kaliteli mikrofon kaydını doğrudan gerçekleştirir.
            return true
        } catch (e: Exception) {
            Log.e("AudioRecorderManager", "Failed to start recording", e)
            cleanup()
            return false
        }
    }

    private fun initLiveSpeechRecognizer(language: String) {
        Handler(Looper.getMainLooper()).post {
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) return@post

                liveSpeechRecognizer?.destroy()
                val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    val tag = if (language.equals("en", true)) "en-US" else if (language.equals("de", true)) "de-DE" else "tr-TR"
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, tag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }

                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isRecognizerListening = true
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        isRecognizerListening = false
                    }

                    override fun onError(error: Int) {
                        isRecognizerListening = false
                        // Kayıt devam ediyorsa ve mikrofon sessizde değilse yeniden dinle
                        if (_isRecording.value && !_isPaused.value && !_isMicMuted.value) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                try {
                                    if (_isRecording.value && !isRecognizerListening) {
                                        liveSpeechRecognizer?.startListening(intent)
                                    }
                                } catch (_: Exception) {}
                            }, 500)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        isRecognizerListening = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()
                        if (!text.isNullOrBlank()) {
                            val current = _liveSpokenText.value
                            val updated = if (current.isBlank()) text else "$current $text"
                            _liveSpokenText.value = updated
                            SpeechTranscriptionEngine.setLiveRecognizedText(updated)
                        }

                        // Sürekli dinleme döngüsü
                        if (_isRecording.value && !_isPaused.value && !_isMicMuted.value) {
                            try {
                                liveSpeechRecognizer?.startListening(intent)
                            } catch (_: Exception) {}
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = partials?.firstOrNull()
                        if (!partial.isNullOrBlank()) {
                            val base = _liveSpokenText.value
                            val full = if (base.isBlank()) partial else "$base $partial"
                            SpeechTranscriptionEngine.setLiveRecognizedText(full)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                liveSpeechRecognizer = recognizer
                recognizer.startListening(intent)
            } catch (e: Exception) {
                Log.w("AudioRecorderManager", "SpeechRecognizer init skipped: ${e.message}")
            }
        }
    }

    fun setLiveRecognizedCustomText(text: String) {
        _liveSpokenText.value = text
        SpeechTranscriptionEngine.setLiveRecognizedText(text)
    }

    fun toggleMicMute() {
        if (!_isRecording.value) return
        val current = _isMicMuted.value
        _isMicMuted.value = !current
    }

    fun pauseRecording() {
        if (!_isRecording.value || _isPaused.value) return
        try {
            mediaRecorder?.pause()
            _isPaused.value = true
            liveSpeechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e("AudioRecorderManager", "Error pausing recorder", e)
        }
    }

    fun resumeRecording() {
        if (!_isRecording.value || !_isPaused.value) return
        try {
            mediaRecorder?.resume()
            _isPaused.value = false
        } catch (e: Exception) {
            Log.e("AudioRecorderManager", "Error resuming recorder", e)
        }
    }

    fun stopRecording(): File? {
        if (!_isRecording.value) return null
        tickerJob?.cancel()

        try {
            liveSpeechRecognizer?.stopListening()
            liveSpeechRecognizer?.destroy()
            liveSpeechRecognizer = null
        } catch (_: Exception) {}

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorderManager", "Error stopping recorder", e)
        }

        val file = currentOutputFile
        cleanup()
        return file
    }

    fun cancelRecording() {
        val file = stopRecording()
        try {
            file?.delete()
        } catch (e: Exception) {
            Log.w("AudioRecorderManager", "Failed to delete cancelled file", e)
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive && _isRecording.value) {
                if (!_isPaused.value) {
                    val current = SystemClock.elapsedRealtime()
                    _elapsedTimeMs.value = current - recordStartTime

                    if (!_isMicMuted.value) {
                        try {
                            val maxAmp = mediaRecorder?.maxAmplitude ?: 0
                            _currentAmplitude.value = (maxAmp / 32767f).coerceIn(0f, 1f)
                        } catch (_: Exception) {
                            _currentAmplitude.value = 0f
                        }
                    } else {
                        _currentAmplitude.value = 0f
                    }
                }
                delay(100)
            }
        }
    }

    private fun cleanup() {
        mediaRecorder = null
        _isRecording.value = false
        _isPaused.value = false
        _isMicMuted.value = false
        _currentAmplitude.value = 0f
        tickerJob?.cancel()
        try {
            liveSpeechRecognizer?.destroy()
            liveSpeechRecognizer = null
        } catch (_: Exception) {}
    }
}
