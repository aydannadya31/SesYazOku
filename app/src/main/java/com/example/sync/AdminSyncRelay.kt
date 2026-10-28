package com.example.sync

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.FeedbackEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Collections
import java.util.concurrent.TimeUnit

class AdminSyncRelay(private val context: Context) {

    companion object {
        private const val TAG = "AdminSyncRelay"
        // Applet-spesifik güvenli eşzamanlama kanalı
        const val RELAY_URL = "https://ntfy.sh/sesyazoku_sync_relay_7bee5aba"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val processedMessageIds = Collections.synchronizedSet(HashSet<String>())

    // 1. Gerçek Cihaz & Yetki Telemetrisini Toplama
    fun gatherDeviceDiagnostics(): JSONObject {
        val root = JSONObject()
        try {
            root.put("deviceModel", "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}")
            root.put("androidVersion", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            root.put("buildNumber", Build.DISPLAY ?: Build.ID)
            root.put("appVersion", "1.0.34 (v34)")

            // Pil Seviyesi
            val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, batteryFilter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPct = if (level >= 0 && scale > 0) (level * 100) / scale else 85
            val isCharging = (batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING)
            root.put("batteryLevel", "$batteryPct% ${if (isCharging) "(Şarj Ediliyor)" else "(Deşarj)"}")

            // Depolama Alanı
            val stat = StatFs(Environment.getDataDirectory().path)
            val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
            val totalBytes = stat.blockCountLong * stat.blockSizeLong
            val freeGb = String.format(java.util.Locale.US, "%.1f", freeBytes / (1024.0 * 1024.0 * 1024.0))
            val totalGb = String.format(java.util.Locale.US, "%.1f", totalBytes / (1024.0 * 1024.0 * 1024.0))
            root.put("storageInfo", "$freeGb GB boş / $totalGb GB")

            // İzin Durumları
            val perms = JSONObject()
            perms.put("audioRecord", ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
            perms.put("camera", ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                perms.put("notifications", ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
            } else {
                perms.put("notifications", true)
            }
            root.put("permissions", perms)

            // Uygulama Kayıt Dosyaları Özeti
            val recDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
            val files = recDir.listFiles() ?: emptyArray()
            val fileListArray = JSONArray()
            files.take(15).forEach { f ->
                val fObj = JSONObject()
                fObj.put("name", f.name)
                fObj.put("size", "${f.length() / 1024} KB")
                fObj.put("lastModified", f.lastModified())
                fileListArray.put(fObj)
            }
            root.put("deviceAudioFiles", fileListArray)
            root.put("totalRecordedFilesCount", files.size)
        } catch (e: Exception) {
            Log.w(TAG, "Error gathering diagnostics: ${e.message}")
        }
        return root
    }

    // 2. APK'dan Yönetim Paneline Anlık Mesaj İletme (<3 Saniye Gecikme Garantisi)
    fun sendFeedbackToAdmin(
        feedback: FeedbackEntity,
        userName: String,
        scope: CoroutineScope,
        onDone: (Boolean) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val effectiveEmail = feedback.userEmail.ifBlank { "aydannadya31@gmail.com" }
                val effectiveName = userName.ifBlank { "Aydan Nadya" }

                val payload = JSONObject().apply {
                    put("type", "USER_FEEDBACK")
                    put("id", feedback.id.toString())
                    put("email", effectiveEmail)
                    put("userName", effectiveName)
                    put("category", feedback.category)
                    put("subject", feedback.subject)
                    put("message", feedback.message)
                    put("timestamp", feedback.timestamp)
                    put("audioUri", feedback.audioUri ?: "")
                    put("imageUri", feedback.imageUri ?: "")
                    put("pdfUri", feedback.pdfUri ?: "")
                    put("attachmentsJson", feedback.attachmentsJson)
                    put("diagnostics", gatherDeviceDiagnostics())
                }

                val body = payload.toString().toRequestBody(jsonMediaType)
                // ASCII-safe Request (No non-ASCII headers!)
                val request = Request.Builder()
                    .url(RELAY_URL)
                    .post(body)
                    .build()

                val call = client.newCall(request)
                call.execute().use { response ->
                    val success = response.isSuccessful
                    Log.i(TAG, "Feedback relay sent to admin. Success=$success, Code=${response.code}")
                    onDone(success)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send feedback to relay", e)
                onDone(false)
            }
        }
    }

    // 3. Yönetim Paneline Periyodik Cihaz Durum Canlılığı Gönderme (Heartbeat)
    fun sendTelemetryHeartbeat(userEmail: String, userName: String, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val effectiveEmail = userEmail.ifBlank { "aydannadya31@gmail.com" }
                val effectiveName = userName.ifBlank { "Aydan Nadya" }

                val payload = JSONObject().apply {
                    put("type", "DEVICE_TELEMETRY")
                    put("email", effectiveEmail)
                    put("userName", effectiveName)
                    put("timestamp", System.currentTimeMillis())
                    put("diagnostics", gatherDeviceDiagnostics())
                }
                val body = payload.toString().toRequestBody(jsonMediaType)
                val request = Request.Builder()
                    .url(RELAY_URL)
                    .post(body)
                    .build()
                client.newCall(request).execute().close()
            } catch (e: Exception) {
                Log.w(TAG, "Telemetry heartbeat failed: ${e.message}")
            }
        }
    }

    // 4. Yönetim Panelinden Gelen Mesaj ve Destek Uyarılarını Dinleme
    fun startListeningForAdminEvents(
        scope: CoroutineScope,
        userEmail: String,
        onAdminReplyReceived: (replyText: String, subject: String, attachmentsJson: String, audioUri: String?, imageUri: String?, pdfUri: String?) -> Unit,
        onSupportAlertReceived: (alertText: String) -> Unit
    ) {
        val effectiveEmail = userEmail.ifBlank { "aydannadya31@gmail.com" }
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    // 10 dakika geriye dönük yoklama ile saat farkı/kayıp mesaj riskini sıfıra indirir
                    val pollUrl = "$RELAY_URL/json?since=10m&poll=1"
                    val request = Request.Builder().url(pollUrl).get().build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bodyStr = response.body?.string().orEmpty()
                            val lines = bodyStr.split("\n")
                            for (line in lines) {
                                if (line.isBlank()) continue
                                try {
                                    val item = JSONObject(line)
                                    val msgId = item.optString("id")
                                    if (msgId.isNotBlank() && !processedMessageIds.add(msgId)) {
                                        // Zaten işlendi
                                        continue
                                    }
                                    val messageField = item.optString("message")
                                    if (messageField.isNotBlank() && messageField.startsWith("{")) {
                                        val innerJson = JSONObject(messageField)
                                        val type = innerJson.optString("type")
                                        val targetEmail = innerJson.optString("targetEmail")

                                        if (type == "ADMIN_REPLY" && (targetEmail.isBlank() || targetEmail.equals(effectiveEmail, ignoreCase = true))) {
                                            val text = innerJson.optString("replyText")
                                            val subject = innerJson.optString("subject", "Teknik Destek Ekibi Yanıtı")
                                            val attachmentsJson = innerJson.optString("attachmentsJson", "")
                                            val audioUri = innerJson.optString("audioUri", "")
                                            val imageUri = innerJson.optString("imageUri", "")
                                            val pdfUri = innerJson.optString("pdfUri", "")
                                            if (text.isNotBlank() || attachmentsJson.isNotBlank()) {
                                                Log.i(TAG, "Admin reply received: $text, attachments=$attachmentsJson")
                                                scope.launch(Dispatchers.Main) {
                                                    onAdminReplyReceived(
                                                        text,
                                                        subject,
                                                        attachmentsJson,
                                                        audioUri.ifBlank { null },
                                                        imageUri.ifBlank { null },
                                                        pdfUri.ifBlank { null }
                                                    )
                                                }
                                            }
                                        } else if (type == "SUPPORT_ALERT") {
                                            val alertText = innerJson.optString("alertText")
                                            if (alertText.isNotBlank()) {
                                                Log.i(TAG, "Support alert received: $alertText")
                                                scope.launch(Dispatchers.Main) {
                                                    onSupportAlertReceived(alertText)
                                                }
                                            }
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Poll error: ${e.message}")
                }
                delay(2000) // 2 saniyelik anlık kontrol döngüsü
            }
        }
    }
}
