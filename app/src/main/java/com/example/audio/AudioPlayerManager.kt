package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
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
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPlayingPath = MutableStateFlow<String?>(null)
    val currentPlayingPath: StateFlow<String?> = _currentPlayingPath.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        try {
            mediaPlayer?.let { player ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    val wasPlaying = player.isPlaying
                    val params = player.playbackParams
                    params.speed = speed
                    player.playbackParams = params
                    if (!wasPlaying) {
                        player.pause()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Failed to change playback speed to $speed", e)
        }
    }

    fun playOrPause(filePath: String) {
        if (_currentPlayingPath.value == filePath && mediaPlayer != null) {
            if (_isPlaying.value) {
                pause()
            } else {
                resume()
            }
            return
        }

        playFile(filePath)
    }

    fun playFile(filePath: String, startPositionMs: Long = 0L) {
        stop()

        val file = File(filePath)
        if (!file.exists()) {
            Log.e("AudioPlayerManager", "Audio file not found: $filePath")
            return
        }

        try {
            val player = MediaPlayer().apply {
                setDataSource(context, Uri.fromFile(file))
                prepare()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    try {
                        val params = playbackParams
                        params.speed = _playbackSpeed.value
                        playbackParams = params
                    } catch (e: Exception) {
                        Log.w("AudioPlayerManager", "Could not set playback speed: ${e.message}")
                    }
                }
                if (startPositionMs > 0 && startPositionMs < duration) {
                    seekTo(startPositionMs.toInt())
                }
                start()
            }

            mediaPlayer = player
            _currentPlayingPath.value = filePath
            _isPlaying.value = true
            _durationMs.value = player.duration.toLong()
            _currentPositionMs.value = startPositionMs

            player.setOnCompletionListener {
                _isPlaying.value = false
                _currentPositionMs.value = 0L
                progressJob?.cancel()
            }

            startProgressTracker()
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Failed to play audio", e)
            stop()
        }
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
            _isPlaying.value = false
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error pausing player", e)
        }
    }

    fun resume() {
        try {
            mediaPlayer?.start()
            _isPlaying.value = true
            startProgressTracker()
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error resuming player", e)
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            mediaPlayer?.seekTo(positionMs.toInt())
            _currentPositionMs.value = positionMs
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error seeking player", e)
        }
    }

    fun stop() {
        progressJob?.cancel()
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error stopping player", e)
        }
        mediaPlayer = null
        _isPlaying.value = false
        _currentPlayingPath.value = null
        _currentPositionMs.value = 0L
        _durationMs.value = 0L
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                try {
                    val pos = mediaPlayer?.currentPosition?.toLong() ?: 0L
                    _currentPositionMs.value = pos
                } catch (_: Exception) {
                }
                delay(150)
            }
        }
    }
}
