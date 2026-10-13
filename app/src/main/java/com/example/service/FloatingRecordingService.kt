package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.audio.AudioRecorderManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FloatingRecordingService : Service() {

    companion object {
        const val CHANNEL_ID = "recording_foreground_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START_CALL_RECORDING = "com.example.service.START_CALL_RECORDING"
        const val ACTION_STOP_CALL_RECORDING = "com.example.service.STOP_CALL_RECORDING"
        const val ACTION_TOGGLE_MIC = "com.example.service.TOGGLE_MIC"

        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()

        private val _isServiceMicMuted = MutableStateFlow(false)
        val isServiceMicMuted = _isServiceMicMuted.asStateFlow()

        private val _recordedDurationSeconds = MutableStateFlow(0)
        val recordedDurationSeconds = _recordedDurationSeconds.asStateFlow()
    }

    private lateinit var recorderManager: AudioRecorderManager
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        recorderManager = AudioRecorderManager(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_CALL_RECORDING -> {
                startForeground(NOTIFICATION_ID, buildNotification("Görüşme ve Uygulama Kaydı Aktif...", false))
                val started = recorderManager.startRecording(audioFormat = "M4A")
                if (started) {
                    _isRunning.value = true
                    observeRecorder()
                } else {
                    stopSelf()
                }
            }
            ACTION_STOP_CALL_RECORDING -> {
                val file = recorderManager.stopRecording()
                _isRunning.value = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_TOGGLE_MIC -> {
                recorderManager.toggleMicMute()
                val muted = recorderManager.isMicMuted.value
                _isServiceMicMuted.value = muted
                val notification = buildNotification(
                    if (muted) "Mikrofon Sessize Alındı (Sadece Uygulama Dinleniyor)" else "Mikrofon Açık - Canlı Kayıt Yapılıyor",
                    muted
                )
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, notification)
            }
        }
        return START_NOT_STICKY
    }

    private fun observeRecorder() {
        scope.launch {
            recorderManager.elapsedTimeMs.collect { ms ->
                _recordedDurationSeconds.value = (ms / 1000).toInt()
            }
        }
    }

    private fun buildNotification(statusText: String, isMuted: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FloatingRecordingService::class.java).apply {
            action = ACTION_STOP_CALL_RECORDING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleMicIntent = Intent(this, FloatingRecordingService::class.java).apply {
            action = ACTION_TOGGLE_MIC
        }
        val toggleMicPendingIntent = PendingIntent.getService(
            this, 2, toggleMicIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SesYazOku - Uygulama & Görüşme Kaydı")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_media_pause,
                if (isMuted) "Mikrofonu Aç" else "Mikrofonu Kapat",
                toggleMicPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Kaydı Bitir",
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_recording),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_recording_desc)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        recorderManager.stopRecording()
        _isRunning.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
