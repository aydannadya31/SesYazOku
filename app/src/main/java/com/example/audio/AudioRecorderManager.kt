package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import android.util.Log
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

    fun startRecording(audioFormat: String = "M4A", audioBitrate: Int = 128000, sampleRate: Int = 44100): Boolean {
        if (_isRecording.value) return false

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

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(audioBitrate)
                setAudioSamplingRate(sampleRate)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            recordStartTime = SystemClock.elapsedRealtime()
            _isRecording.value = true
            _isPaused.value = false
            _isMicMuted.value = false
            _elapsedTimeMs.value = 0L

            startTicker()
            return true
        } catch (e: Exception) {
            Log.e("AudioRecorderManager", "Failed to start recording", e)
            cleanup()
            return false
        }
    }

    fun toggleMicMute() {
        if (!_isRecording.value) return
        val current = _isMicMuted.value
        _isMicMuted.value = !current
        // Visual indicator and state toggle for mic active/mute during session
    }

    fun pauseRecording() {
        if (!_isRecording.value || _isPaused.value) return
        try {
            mediaRecorder?.pause()
            _isPaused.value = true
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
    }
}
