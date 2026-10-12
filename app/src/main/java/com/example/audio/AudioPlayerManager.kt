package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
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

class AudioPlayerManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingPath: String? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private var progressJob: Job? = null

    fun playOrPause(filePathOrUri: String) {
        if (currentPlayingPath == filePathOrUri && mediaPlayer != null) {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                _isPlaying.value = false
            } else {
                mediaPlayer?.start()
                _isPlaying.value = true
                startTrackingProgress()
            }
            return
        }

        // New source
        stop()
        currentPlayingPath = filePathOrUri

        try {
            mediaPlayer = MediaPlayer().apply {
                if (filePathOrUri.startsWith("content://") || filePathOrUri.startsWith("file://")) {
                    setDataSource(context, Uri.parse(filePathOrUri))
                } else {
                    val file = File(filePathOrUri)
                    if (file.exists()) {
                        setDataSource(file.absolutePath)
                    } else {
                        Log.w("AudioPlayer", "File does not exist: $filePathOrUri")
                        return
                    }
                }

                setOnPreparedListener { mp ->
                    _durationMs.value = mp.duration.toLong()
                    setSpeed(_playbackSpeed.value)
                    mp.start()
                    _isPlaying.value = true
                    startTrackingProgress()
                }

                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPositionMs.value = 0L
                    progressJob?.cancel()
                }

                setOnErrorListener { _, what, extra ->
                    Log.e("AudioPlayer", "MediaPlayer error: what=$what, extra=$extra")
                    _isPlaying.value = false
                    false
                }

                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Failed to start player", e)
            _isPlaying.value = false
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            mediaPlayer?.seekTo(positionMs.toInt())
            _currentPositionMs.value = positionMs
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Seek error", e)
        }
    }

    fun jumpForward5s() {
        val target = (_currentPositionMs.value + 5000L).coerceAtMost(_durationMs.value)
        seekTo(target)
    }

    fun jumpBackward5s() {
        val target = (_currentPositionMs.value - 5000L).coerceAtLeast(0L)
        seekTo(target)
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        mp.playbackParams = mp.playbackParams.setSpeed(speed)
                    }
                }
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Set speed error", e)
            }
        }
    }

    fun stop() {
        progressJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Stop error", e)
        } finally {
            mediaPlayer = null
            _isPlaying.value = false
            _currentPositionMs.value = 0L
        }
    }

    private fun startTrackingProgress() {
        progressJob?.cancel()
        progressJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive && _isPlaying.value) {
                try {
                    val pos = mediaPlayer?.currentPosition?.toLong() ?: 0L
                    _currentPositionMs.value = pos
                } catch (e: Exception) {
                    // Ignore transient position lookup error
                }
                delay(150)
            }
        }
    }

    fun release() {
        stop()
    }
}
