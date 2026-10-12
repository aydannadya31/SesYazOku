package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
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
import java.io.IOException

class AudioRecorderManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _recordingDurationMs = MutableStateFlow(0L)
    val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0f)
    val currentAmplitude: StateFlow<Float> = _currentAmplitude.asStateFlow()

    private val _waveforms = MutableStateFlow<List<Float>>(emptyList())
    val waveforms: StateFlow<List<Float>> = _waveforms.asStateFlow()

    private var timerJob: Job? = null
    private var startTimeMs = 0L
    private var pausedDurationOffset = 0L

    fun startRecording(audioSource: Int = MediaRecorder.AudioSource.MIC): File? {
        stopRecording()

        val recordingsDir = File(context.filesDir, "recordings")
        if (!recordingsDir.exists()) {
            recordingsDir.mkdirs()
        }

        val file = File(recordingsDir, "rec_${System.currentTimeMillis()}.m4a")
        currentOutputFile = file

        fun createRecorder(source: Int): Boolean {
            return try {
                mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }.apply {
                    setAudioSource(source)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(128000)
                    setAudioSamplingRate(44100)
                    setOutputFile(file.absolutePath)
                    prepare()
                    start()
                }
                _isRecording.value = true
                _isPaused.value = false
                startTimeMs = System.currentTimeMillis()
                pausedDurationOffset = 0L
                _recordingDurationMs.value = 0L
                _waveforms.value = emptyList()
                startPolling()
                true
            } catch (e: Exception) {
                Log.w("AudioRecorder", "Source $source failed: ${e.message}")
                releaseRecorder()
                false
            }
        }

        val success = createRecorder(audioSource)
        if (!success && audioSource != MediaRecorder.AudioSource.MIC) {
            createRecorder(MediaRecorder.AudioSource.MIC)
        }

        return file
    }

    fun pauseRecording() {
        if (_isRecording.value && !_isPaused.value) {
            try {
                mediaRecorder?.pause()
                _isPaused.value = true
                pausedDurationOffset = _recordingDurationMs.value
            } catch (e: Exception) {
                Log.e("AudioRecorder", "pause failed", e)
            }
        }
    }

    fun resumeRecording() {
        if (_isRecording.value && _isPaused.value) {
            try {
                mediaRecorder?.resume()
                _isPaused.value = false
                startTimeMs = System.currentTimeMillis()
            } catch (e: Exception) {
                Log.e("AudioRecorder", "resume failed", e)
            }
        }
    }

    fun stopRecording(): File? {
        timerJob?.cancel()
        val file = currentOutputFile
        if (_isRecording.value) {
            try {
                mediaRecorder?.stop()
            } catch (e: Exception) {
                Log.e("AudioRecorder", "stop failed", e)
            }
        }
        releaseRecorder()
        _isRecording.value = false
        _isPaused.value = false
        _currentAmplitude.value = 0f
        return file
    }

    private fun startPolling() {
        timerJob?.cancel()
        timerJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive && _isRecording.value) {
                if (!_isPaused.value) {
                    val currentNow = System.currentTimeMillis()
                    _recordingDurationMs.value = pausedDurationOffset + (currentNow - startTimeMs)

                    val maxAmp = try {
                        mediaRecorder?.maxAmplitude ?: 0
                    } catch (e: Exception) {
                        0
                    }
                    val normalized = (maxAmp.toFloat() / 32767f).coerceIn(0.05f, 1f)
                    _currentAmplitude.value = normalized

                    val currentList = _waveforms.value.toMutableList()
                    if (currentList.size > 60) {
                        currentList.removeAt(0)
                    }
                    currentList.add(normalized)
                    _waveforms.value = currentList
                }
                delay(100)
            }
        }
    }

    private fun releaseRecorder() {
        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e("AudioRecorder", "release failed", e)
        } finally {
            mediaRecorder = null
        }
    }
}
