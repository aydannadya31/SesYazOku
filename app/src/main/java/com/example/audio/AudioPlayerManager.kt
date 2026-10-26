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

    private val _skipSilenceEnabled = MutableStateFlow(false)
    val skipSilenceEnabled: StateFlow<Boolean> = _skipSilenceEnabled.asStateFlow()

    private val _vocalBoostEnabled = MutableStateFlow(false)
    val vocalBoostEnabled: StateFlow<Boolean> = _vocalBoostEnabled.asStateFlow()

    private var equalizer: android.media.audiofx.Equalizer? = null
    private var activeSegmentBoundaries: List<Pair<Long, Long>> = emptyList()

    fun setSkipSilence(enabled: Boolean) {
        _skipSilenceEnabled.value = enabled
    }

    fun toggleSkipSilence() {
        _skipSilenceEnabled.value = !_skipSilenceEnabled.value
    }

    fun setSegmentBoundaries(segments: List<Pair<Long, Long>>) {
        activeSegmentBoundaries = segments.sortedBy { it.first }
    }

    fun toggleVocalBoost() {
        val next = !_vocalBoostEnabled.value
        _vocalBoostEnabled.value = next
        applyVocalBoostEffect(next)
    }

    private fun applyVocalBoostEffect(enable: Boolean) {
        try {
            val player = mediaPlayer ?: return
            if (enable) {
                if (equalizer == null) {
                    equalizer = android.media.audiofx.Equalizer(0, player.audioSessionId).apply {
                        enabled = true
                    }
                }
                equalizer?.let { eq ->
                    eq.enabled = true
                    val bands = eq.numberOfBands
                    for (i in 0 until bands) {
                        val freq = eq.getCenterFreq(i.toShort()) / 1000 // in Hz
                        val range = eq.bandLevelRange
                        val maxBoost = range[1]
                        val minCut = range[0]
                        if (freq in 800..4500) {
                            // Boost vocal speech frequencies by +6 dB (600 mB)
                            eq.setBandLevel(i.toShort(), (600).coerceAtMost(maxBoost.toInt()).toShort())
                        } else if (freq < 200) {
                            // Cut low rumble / air conditioner noise by -4 dB
                            eq.setBandLevel(i.toShort(), (-400).coerceAtLeast(minCut.toInt()).toShort())
                        }
                    }
                }
            } else {
                equalizer?.enabled = false
                equalizer?.release()
                equalizer = null
            }
        } catch (e: Exception) {
            Log.w("AudioPlayerManager", "Vocal boost equalizer not supported on this device/emulator: ${e.message}")
        }
    }

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

            if (_vocalBoostEnabled.value) {
                applyVocalBoostEffect(true)
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
            if (_vocalBoostEnabled.value) {
                applyVocalBoostEffect(true)
            }
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
            equalizer?.enabled = false
            equalizer?.release()
        } catch (_: Exception) {}
        equalizer = null
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

                    // Feature 5: Skip Silence Mode
                    if (_skipSilenceEnabled.value && activeSegmentBoundaries.isNotEmpty()) {
                        val bounds = activeSegmentBoundaries
                        // Check if pos is in a gap before the first segment
                        if (bounds.isNotEmpty() && pos < bounds.first().first - 1000L) {
                            seekTo(bounds.first().first)
                        } else {
                            // Check if pos is between segment end and next segment start
                            for (i in 0 until bounds.size - 1) {
                                val currentEnd = bounds[i].second
                                val nextStart = bounds[i + 1].first
                                if (pos > currentEnd + 800L && pos < nextStart - 300L && (nextStart - currentEnd) >= 1200L) {
                                    seekTo(nextStart)
                                    break
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                }
                delay(120)
            }
        }
    }
}
