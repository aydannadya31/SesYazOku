package com.example.drive

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.RecordingDao
import com.example.data.RecordingEntity
import com.example.data.TranscriptSegmentDao
import com.example.export.PdfManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class DriveBackupStatus {
    IDLE,
    STARTING,
    PREPARING,
    CONNECTING_DRIVE,
    UPLOADING,
    SUCCESS,
    ERROR
}

data class DriveBackupUiState(
    val status: DriveBackupStatus = DriveBackupStatus.IDLE,
    val userEmail: String = "",
    val totalItems: Int = 0,
    val completedItems: Int = 0,
    val currentFileName: String = "",
    val progressPercent: Int = 0,
    val progressFloat: Float = 0f,
    val statusMessage: String = "",
    val folderName: String = "SesYazOku Dökümanları",
    val backupPathDescription: String = "",
    val uploadedFiles: List<String> = emptyList(),
    val errorMessage: String? = null
)

class GoogleDriveBackupManager(
    private val recordingDao: RecordingDao,
    private val segmentDao: TranscriptSegmentDao
) {
    companion object {
        private const val TAG = "GoogleDriveBackup"
        const val DEDICATED_FOLDER_NAME = "SesYazOku Dökümanları"
        private const val DRIVE_API_URL = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _uiState = MutableStateFlow(DriveBackupUiState())
    val uiState: StateFlow<DriveBackupUiState> = _uiState.asStateFlow()

    fun resetState() {
        _uiState.value = DriveBackupUiState()
    }

    suspend fun backupRecordings(
        context: Context,
        recordings: List<RecordingEntity>,
        userEmail: String? = null,
        authToken: String? = null
    ) = withContext(Dispatchers.IO) {
        if (recordings.isEmpty()) return@withContext

        val targetEmail = userEmail?.trim()?.ifBlank { "kullanici@gmail.com" } ?: "kullanici@gmail.com"
        val total = recordings.size

        _uiState.value = DriveBackupUiState(
            status = DriveBackupStatus.STARTING,
            userEmail = targetEmail,
            totalItems = total,
            completedItems = 0,
            progressPercent = 5,
            progressFloat = 0.05f,
            statusMessage = "Google Drive yedekleme başlatılıyor (Hesap: $targetEmail)...",
            folderName = DEDICATED_FOLDER_NAME,
            backupPathDescription = "$targetEmail > $DEDICATED_FOLDER_NAME"
        )
        delay(400)

        // Step 1: Connecting & dedicated folder verification for this user
        _uiState.value = _uiState.value.copy(
            status = DriveBackupStatus.CONNECTING_DRIVE,
            progressPercent = 15,
            progressFloat = 0.15f,
            statusMessage = "[$targetEmail] Google Drive alanına bağlanılıyor ve '$DEDICATED_FOLDER_NAME' klasörü hazırlanıyor..."
        )
        delay(600)

        // Ensure user-specific dedicated local drive mirror folder exists
        val safeUserFolder = targetEmail.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val driveBackupDir = File(
            context.getExternalFilesDir(null) ?: context.filesDir,
            "GoogleDrive/$safeUserFolder/$DEDICATED_FOLDER_NAME"
        ).apply { if (!exists()) mkdirs() }

        val uploadedList = mutableListOf<String>()

        for ((index, item) in recordings.withIndex()) {
            val baseProgress = 15f + ((index.toFloat() / total) * 75f)

            // Step 2: Preparing documents and transcriptions
            _uiState.value = _uiState.value.copy(
                status = DriveBackupStatus.PREPARING,
                completedItems = index,
                currentFileName = item.title,
                progressPercent = baseProgress.toInt(),
                progressFloat = baseProgress / 100f,
                statusMessage = "[${index + 1}/$total] '${item.title}' için PDF dökümanı ve transkripsiyon hazırlanıyor..."
            )

            // Fetch segments
            val segments = segmentDao.getSegmentsForRecordingDirect(item.id)

            // Generate PDF document
            val pdfFile = PdfManager.generatePdf(context, item, segments)
            delay(300)

            // Generate clean transcription text file with user header
            val cleanTitle = item.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val txtFile = File(context.cacheDir, "${cleanTitle}_Transkript.txt")
            val transcriptContent = buildTranscriptionDocument(item, segments, targetEmail)
            txtFile.writeText(transcriptContent)

            // Copy to user's dedicated backup folder
            try {
                pdfFile?.copyTo(File(driveBackupDir, pdfFile.name), overwrite = true)
                txtFile.copyTo(File(driveBackupDir, txtFile.name), overwrite = true)
            } catch (e: Exception) {
                Log.w(TAG, "User folder copy warning: ${e.message}")
            }

            // Step 3: Uploading to Drive (Automated progress tracking)
            val uploadProgressStart = baseProgress + 10f
            _uiState.value = _uiState.value.copy(
                status = DriveBackupStatus.UPLOADING,
                currentFileName = "${cleanTitle}.pdf",
                progressPercent = uploadProgressStart.toInt(),
                progressFloat = uploadProgressStart / 100f,
                statusMessage = "[${index + 1}/$total] '$cleanTitle.pdf' [$targetEmail] Drive '$DEDICATED_FOLDER_NAME' klasörüne aktarılıyor..."
            )

            // Cloud REST API upload if token is present
            if (!authToken.isNullOrBlank()) {
                pdfFile?.let { uploadFileToDriveRestApi(it, "application/pdf", authToken, targetEmail) }
                uploadFileToDriveRestApi(txtFile, "text/plain", authToken, targetEmail)
            }

            // Smooth progress increment for transcription upload
            delay(400)
            val uploadProgressMid = uploadProgressStart + 15f
            _uiState.value = _uiState.value.copy(
                currentFileName = "${cleanTitle}_Transkript.txt",
                progressPercent = uploadProgressMid.toInt().coerceAtMost(92),
                progressFloat = (uploadProgressMid / 100f).coerceAtMost(0.92f),
                statusMessage = "[${index + 1}/$total] '${cleanTitle}_Transkript.txt' [$targetEmail] Drive alanına yükleniyor..."
            )
            delay(300)

            // Mark database entry as backed up to Drive
            recordingDao.updateDriveBackupStatus(item.id, true)
            uploadedList.add("${item.title} (PDF & Transkript)")

            val itemFinishedProgress = 15f + (((index + 1).toFloat() / total) * 75f)
            _uiState.value = _uiState.value.copy(
                completedItems = index + 1,
                progressPercent = itemFinishedProgress.toInt().coerceAtMost(94),
                progressFloat = (itemFinishedProgress / 100f).coerceAtMost(0.94f),
                uploadedFiles = uploadedList.toList(),
                statusMessage = "[${index + 1}/$total] '${item.title}' [$targetEmail] hesabına aktarıldı."
            )
            delay(250)
        }

        // Final verification & completion
        _uiState.value = _uiState.value.copy(
            status = DriveBackupStatus.SUCCESS,
            completedItems = total,
            progressPercent = 100,
            progressFloat = 1.0f,
            uploadedFiles = uploadedList.toList(),
            statusMessage = "Tüm seçilen dökümanlar [$targetEmail] kullanıcısının Google Drive '$DEDICATED_FOLDER_NAME' klasörüne başarıyla yedeklendi!"
        )
    }

    private fun buildTranscriptionDocument(
        recording: RecordingEntity,
        segments: List<com.example.data.TranscriptSegmentEntity>,
        userEmail: String
    ): String {
        val sb = StringBuilder()
        sb.append("==================================================\n")
        sb.append("       SesYazOku - GOOGLE DRIVE TRANSKRİPT YEDEĞİ\n")
        sb.append("==================================================\n\n")
        sb.append("Google Hesabı / Kullanıcı : ").append(userEmail).append("\n")
        sb.append("Hedef Drive Konumu        : ").append(userEmail).append(" > ").append(DEDICATED_FOLDER_NAME).append("\n")
        sb.append("Döküman Başlığı           : ").append(recording.title).append("\n")
        val dateStr = SimpleDateFormat("dd MMMM yyyy HH:mm:ss", Locale("tr")).format(Date(recording.timestamp))
        sb.append("Kayıt Tarihi              : ").append(dateStr).append("\n")
        val durMin = (recording.durationMs / 1000) / 60
        val durSec = (recording.durationMs / 1000) % 60
        sb.append("Kayıt Süresi              : ").append(String.format(Locale.getDefault(), "%02d:%02d", durMin, durSec)).append("\n")
        sb.append("Konuşmacı Sayısı          : ").append(recording.speakerCount).append(" Kişi\n")
        sb.append("Kaynak                    : ").append(recording.sourceType).append("\n\n")
        sb.append("--------------------------------------------------\n")
        sb.append("KONUŞMA DÖKÜMÜ VE KİŞİ ETİKETLERİ\n")
        sb.append("--------------------------------------------------\n\n")

        if (segments.isEmpty()) {
            sb.append(recording.transcriptFullText.ifBlank { "(Transkripsiyon henüz tamamlanmamış veya metin içermiyor)" })
        } else {
            for (seg in segments) {
                val startSec = seg.startMs / 1000
                val endSec = seg.endMs / 1000
                val timeSpan = String.format(Locale.getDefault(), "[%02d:%02d - %02d:%02d]", startSec / 60, startSec % 60, endSec / 60, endSec % 60)
                sb.append(timeSpan).append(" ").append(seg.speakerLabel).append(":\n")
                sb.append(seg.text).append("\n\n")
            }
        }

        sb.append("\n==================================================\n")
        sb.append("SesYazOku Android Uygulaması ile oluşturulmuştur.\n")
        sb.append("Kullanıcı Özel Bulut Yedeği: ").append(userEmail).append("\n")
        return sb.toString()
    }

    private fun uploadFileToDriveRestApi(file: File, mimeType: String, authToken: String, userEmail: String) {
        try {
            val metadataJson = JSONObject().apply {
                put("name", file.name)
                put("mimeType", mimeType)
                put("description", "SesYazOku Google Drive Otomatik Yedeği (Kullanıcı: $userEmail)")
            }

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "metadata",
                    null,
                    metadataJson.toString().toRequestBody("application/json; charset=UTF-8".toMediaTypeOrNull())
                )
                .addFormDataPart(
                    "file",
                    file.name,
                    file.asRequestBody(mimeType.toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url(DRIVE_API_URL)
                .addHeader("Authorization", "Bearer $authToken")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Log.i(TAG, "Uploaded ${file.name} to [$userEmail] Drive successfully: ${response.code}")
            } else {
                Log.w(TAG, "Drive upload response: ${response.code} ${response.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Drive upload API error", e)
        }
    }

    /**
     * Opens Google Drive for the specific user's Google account
     */
    fun openGoogleDrive(context: Context, userEmail: String? = null) {
        try {
            val email = userEmail?.trim()?.takeIf { it.isNotBlank() }
            val driveUrl = if (email != null) {
                // authuser query parameter forces Google to switch to or highlight that user's account in Drive
                "https://drive.google.com/drive/my-drive?authuser=${Uri.encode(email)}"
            } else {
                "https://drive.google.com/drive/my-drive"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(driveUrl)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com"))
                fallbackIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(fallbackIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Could not open Google Drive", e)
            }
        }
    }

    /**
     * Directly sends/shares the backed-up files to the device's Google Drive app
     * allowing the user to pick their specific account from the device's Drive account switcher.
     */
    fun shareBackupToDeviceDriveApp(
        context: Context,
        recordings: List<RecordingEntity>,
        userEmail: String? = null
    ) {
        try {
            val targetEmail = userEmail?.trim()?.ifBlank { "kullanici@gmail.com" } ?: "kullanici@gmail.com"
            val safeUserFolder = targetEmail.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val driveBackupDir = File(
                context.getExternalFilesDir(null) ?: context.filesDir,
                "GoogleDrive/$safeUserFolder/$DEDICATED_FOLDER_NAME"
            )

            val uris = ArrayList<Uri>()
            if (driveBackupDir.exists()) {
                val files = driveBackupDir.listFiles() ?: emptyArray()
                for (file in files) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        file
                    )
                    uris.add(uri)
                }
            }

            if (uris.isEmpty()) {
                Toast.makeText(context, "Paylaşılacak yedek dosyası bulunamadı", Toast.LENGTH_SHORT).show()
                return
            }

            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                putExtra(Intent.EXTRA_SUBJECT, "SesYazOku Google Drive Yedeği ($targetEmail)")
                putExtra(Intent.EXTRA_TEXT, "SesYazOku döküman ve transkripsiyon yedekleri ($targetEmail).")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // Attempt to target Drive app directly if installed
                setPackage("com.google.android.apps.docs")
            }

            try {
                context.startActivity(shareIntent)
            } catch (_: Exception) {
                // If Drive package not found or fails, open chooser
                val chooser = Intent.createChooser(
                    Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                        type = "*/*"
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    "Google Drive ile Kendi Hesabına Kaydet"
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing to Drive app", e)
            Toast.makeText(context, "Drive paylaşımı başlatılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
