package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class CallCaptureForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "call_capture_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START = "action_start_call_capture"
        const val ACTION_STOP = "action_stop_call_capture"
        const val ACTION_SWITCH_SPEAKER = "action_switch_speaker"

        // Broadcast actions to communicate with AppViewModel
        const val BROADCAST_CALL_CAPTURE_EVENT = "com.example.CALL_CAPTURE_EVENT"
        const val EXTRA_EVENT_TYPE = "extra_event_type"
        const val EVENT_SWITCH_SPEAKER = "event_switch_speaker"
        const val EVENT_STOP_AND_SAVE = "event_stop_and_save"

        var isRunning = false
            private set
    }

    private var floatingOverlayManager: FloatingOverlayManager? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        floatingOverlayManager = FloatingOverlayManager(
            context = this,
            onSwitchSpeaker = {
                sendBroadcastToViewModel(EVENT_SWITCH_SPEAKER)
            },
            onStopAndSave = {
                sendBroadcastToViewModel(EVENT_STOP_AND_SAVE)
                stopSelf()
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_START -> {
                isRunning = true
                val notification = buildNotification("🔴 Canlı Görüşme Yakalanıyor (Zoom/Meet/Arama)", "Diğer uygulamadaki konuşmalar kaydediliyor & transkribe ediliyor.")
                startForeground(NOTIFICATION_ID, notification)

                // Show floating bubble if overlay permission granted
                floatingOverlayManager?.show()
            }
            ACTION_SWITCH_SPEAKER -> {
                sendBroadcastToViewModel(EVENT_SWITCH_SPEAKER)
            }
            ACTION_STOP -> {
                sendBroadcastToViewModel(EVENT_STOP_AND_SAVE)
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun sendBroadcastToViewModel(eventType: String) {
        val intent = Intent(BROADCAST_CALL_CAPTURE_EVENT).apply {
            setPackage(packageName)
            putExtra(EXTRA_EVENT_TYPE, eventType)
        }
        sendBroadcast(intent)
    }

    fun updateOverlaySnippet(text: String, currentSpeaker: String) {
        floatingOverlayManager?.updateSnippet(text, currentSpeaker)
    }

    private fun buildNotification(title: String, text: String): android.app.Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val switchIntent = Intent(this, CallCaptureForegroundService::class.java).apply {
            action = ACTION_SWITCH_SPEAKER
        }
        val switchPendingIntent = PendingIntent.getService(
            this,
            1,
            switchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, CallCaptureForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(title)
            .setContentText(text)
            .setSubText("Canlı Görüşme Modu")
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_rotate, "Konuşmacı Değiştir", switchPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Kaydet & Bitir", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Canlı Görüşme Yakalama",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Zoom, Meet, WhatsApp ve harici görüşmeler sırasında arkaplan transkripsiyonu"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        isRunning = false
        floatingOverlayManager?.hide()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
