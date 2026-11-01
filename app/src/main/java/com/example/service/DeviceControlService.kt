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
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.sync.DeviceControlRelay

/**
 * Yönetim panelinden başlatılan mikrofon/kamera erişimi için ön plan servisi.
 * Android 11+ arka planda mikrofon/kamera erişimine yalnızca bu servis çalışırken izin verir.
 * Kullanıcıya kalıcı bildirim gösterilir ve tek dokunuşla durdurulabilir.
 */
class DeviceControlService : Service() {

    companion object {
        private const val TAG = "DeviceControlService"
        const val CHANNEL_ID = "device_control_channel"
        const val NOTIFICATION_ID = 2002
        const val ACTION_STOP = "com.example.service.STOP_DEVICE_CONTROL"
        private const val EXTRA_MODE = "mode"

        private val readyLock = Object()

        @Volatile
        private var foregroundActive = false

        fun start(context: Context, mode: String) {
            val intent = Intent(context, DeviceControlService::class.java).putExtra(EXTRA_MODE, mode)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "start failed: ${e.message}")
            }
        }

        fun stop(context: Context) {
            foregroundActive = false
            try {
                context.stopService(Intent(context, DeviceControlService::class.java))
            } catch (e: Exception) {
                Log.w(TAG, "stop failed: ${e.message}")
            }
        }

        /** Servis gerçekten ön planda çalışana kadar bekler; mikrofon/kamera açmadan önce çağrılmalı. */
        fun awaitReady(timeoutMs: Long): Boolean {
            val deadline = System.currentTimeMillis() + timeoutMs
            synchronized(readyLock) {
                while (!foregroundActive) {
                    val remaining = deadline - System.currentTimeMillis()
                    if (remaining <= 0) return false
                    try {
                        readyLock.wait(remaining)
                    } catch (e: InterruptedException) {
                        return false
                    }
                }
                return true
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            DeviceControlRelay.stopActiveStreams()
            stopSelf()
            return START_NOT_STICKY
        }
        val mode = intent?.getStringExtra(EXTRA_MODE) ?: "Uzaktan erişim"
        startForeground(NOTIFICATION_ID, buildNotification(mode))
        synchronized(readyLock) {
            foregroundActive = true
            readyLock.notifyAll()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        foregroundActive = false
        super.onDestroy()
    }

    private fun buildNotification(mode: String): Notification {
        createChannel()
        val openIntent = Intent(this, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = Intent(this, DeviceControlService::class.java).setAction(ACTION_STOP)
        val stopPending = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Uzaktan destek aktif")
            .setContentText("$mode - yönetim panelinden izleniyor. Durdurmak için dokunun.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openPending)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "Durdur", stopPending)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Uzaktan cihaz erişimi", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Yönetim panelinden mikrofon/kamera erişimi açıkken gösterilir"
                setShowBadge(false)
            }
        )
    }
}
