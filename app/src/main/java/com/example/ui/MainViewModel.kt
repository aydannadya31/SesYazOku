package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioImportManager
import com.example.audio.AudioPlayerManager
import com.example.audio.AudioRecorderManager
import com.example.auth.AuthManager
import com.example.data.AppDatabase
import com.example.data.FeedbackEntity
import com.example.data.RecordingEntity
import com.example.data.TranscriptSegmentEntity
import com.example.drive.GoogleDriveBackupManager
import com.example.drive.DriveBackupUiState
import com.example.export.PdfManager
import com.example.transcription.SpeechTranscriptionEngine
import com.example.update.UpdateManager
import com.example.update.UpdateUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val recordingDao = db.recordingDao()
    private val segmentDao = db.transcriptSegmentDao()
    private val feedbackDao = db.feedbackDao()
    private val folderDao = db.folderDao()

    val recorderManager = AudioRecorderManager(application)
    val playerManager = AudioPlayerManager(application)
    val authManager = AuthManager(application)
    val driveBackupManager = GoogleDriveBackupManager(recordingDao, segmentDao)
    val driveBackupUiState: StateFlow<DriveBackupUiState> = driveBackupManager.uiState
    val updateManager = UpdateManager(application)
    val updateUiState: StateFlow<UpdateUiState> = updateManager.uiState

    val recordings: StateFlow<List<RecordingEntity>> = recordingDao.getAllRecordings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<com.example.data.FolderEntity>> = folderDao.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val feedbacks: StateFlow<List<FeedbackEntity>> = feedbackDao.getAllFeedbacks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            try {
                kotlinx.coroutines.delay(300)
                val current = folderDao.getAllFolders().first()
                if (current.isEmpty()) {
                    val initialFolders = listOf(
                        com.example.data.FolderEntity(name = "Genel Kayıtlar", type = "PROJECT", colorHex = "#3B82F6", description = "Genel ses kayıtları"),
                        com.example.data.FolderEntity(name = "Toplantılar", type = "PROJECT", colorHex = "#10B981", description = "İş ve ekip toplantıları"),
                        com.example.data.FolderEntity(name = "Röportaj & Mülakatlar", type = "PROJECT", colorHex = "#8B5CF6", description = "Görüşme ve mülakat kayıtları"),
                        com.example.data.FolderEntity(name = "Ders & Eğitim", type = "PROJECT", colorHex = "#F59E0B", description = "Seminer ve ders sesleri"),
                        com.example.data.FolderEntity(name = "Fikirler & Notlar", type = "PROJECT", colorHex = "#EF4444", description = "Kişisel sesli notlar"),
                        com.example.data.FolderEntity(name = "Genel", type = "CATEGORY", colorHex = "#3B82F6"),
                        com.example.data.FolderEntity(name = "Toplantı", type = "CATEGORY", colorHex = "#10B981"),
                        com.example.data.FolderEntity(name = "Ders & Konferans", type = "CATEGORY", colorHex = "#F59E0B"),
                        com.example.data.FolderEntity(name = "Mülakat", type = "CATEGORY", colorHex = "#8B5CF6"),
                        com.example.data.FolderEntity(name = "Sesli Not", type = "CATEGORY", colorHex = "#06B6D4"),
                        com.example.data.FolderEntity(name = "Önemli", type = "CATEGORY", colorHex = "#EF4444")
                    )
                    for (f in initialFolders) {
                        folderDao.insertFolder(f)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // Settings
    private val prefs = application.getSharedPreferences("sesyazoku_settings", Context.MODE_PRIVATE)
    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _language = MutableStateFlow(prefs.getString("language", "tr") ?: "tr")
    val language: StateFlow<String> = _language.asStateFlow()

    private val _audioFormat = MutableStateFlow(prefs.getString("audio_format", "M4A") ?: "M4A")
    val audioFormat: StateFlow<String> = _audioFormat.asStateFlow()

    private val _audioBitrate = MutableStateFlow(prefs.getInt("audio_bitrate", 128000))
    val audioBitrate: StateFlow<Int> = _audioBitrate.asStateFlow()

    private val _audioSampleRate = MutableStateFlow(prefs.getInt("audio_sample_rate", 44100))
    val audioSampleRate: StateFlow<Int> = _audioSampleRate.asStateFlow()

    // Transcription state
    private val _isTranscribing = MutableStateFlow(false)
    val isTranscribing: StateFlow<Boolean> = _isTranscribing.asStateFlow()

    private val _transcribeProgress = MutableStateFlow(0f)
    val transcribeProgress: StateFlow<Float> = _transcribeProgress.asStateFlow()

    private val _selectedRecordingId = MutableStateFlow<Long?>(null)
    val selectedRecordingId: StateFlow<Long?> = _selectedRecordingId.asStateFlow()

    private val _selectedSegments = MutableStateFlow<List<TranscriptSegmentEntity>>(emptyList())
    val selectedSegments: StateFlow<List<TranscriptSegmentEntity>> = _selectedSegments.asStateFlow()

    fun selectRecordingForView(id: Long) {
        _selectedRecordingId.value = id
        viewModelScope.launch {
            val segments = segmentDao.getSegmentsForRecordingDirect(id)
            _selectedSegments.value = segments
        }
    }

    fun updateSpeakerLabel(recordingId: Long, speakerIndex: Int, newLabel: String) {
        viewModelScope.launch {
            segmentDao.updateSpeakerLabel(recordingId, speakerIndex, newLabel.trim())
            val updatedSegments = segmentDao.getSegmentsForRecordingDirect(recordingId)
            if (_selectedRecordingId.value == recordingId) {
                _selectedSegments.value = updatedSegments
            }
            val updatedFullText = updatedSegments.joinToString("\n\n") { "${it.speakerLabel}: ${it.text}" }
            val speakerCount = updatedSegments.map { it.speakerIndex }.distinct().size
            recordingDao.updateTranscriptionResult(recordingId, updatedFullText, speakerCount)
        }
    }

    fun getSegmentsForRecording(recordingId: Long, onResult: (List<TranscriptSegmentEntity>) -> Unit) {
        viewModelScope.launch {
            val segments = segmentDao.getSegmentsForRecordingDirect(recordingId)
            onResult(segments)
        }
    }

    fun startLiveRecording(): Boolean {
        return recorderManager.startRecording(
            audioFormat = _audioFormat.value,
            audioBitrate = _audioBitrate.value,
            sampleRate = _audioSampleRate.value,
            language = _language.value
        )
    }

    fun stopAndSaveRecording(sourceType: String = "LIVE", titlePrefix: String = "Kayıt"): Long {
        val file = recorderManager.stopRecording() ?: return -1L
        var createdId = 0L
        viewModelScope.launch {
            val duration = recorderManager.elapsedTimeMs.value
            val entity = RecordingEntity(
                title = "$titlePrefix #${System.currentTimeMillis() % 10000}",
                filePath = file.absolutePath,
                durationMs = duration,
                fileSize = file.length(),
                mimeType = "audio/m4a",
                sourceType = sourceType,
                timestamp = System.currentTimeMillis()
            )
            createdId = recordingDao.insertRecording(entity)
        }
        return createdId
    }

    fun importAudioFile(uri: Uri) {
        viewModelScope.launch {
            val entity = AudioImportManager.importAudioFile(getApplication(), uri)
            if (entity != null) {
                recordingDao.insertRecording(entity)
                Toast.makeText(getApplication(), "Ses dosyası başarıyla eklendi!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(getApplication(), "Ses dosyası yüklenemedi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun runTranscription(recording: RecordingEntity, speakerCount: Int = 2) {
        if (_isTranscribing.value) return
        _isTranscribing.value = true
        _transcribeProgress.value = 0f

        viewModelScope.launch {
            try {
                val (fullText, segments) = SpeechTranscriptionEngine.transcribeAudio(
                    context = getApplication(),
                    recordingId = recording.id,
                    filePath = recording.filePath,
                    targetSpeakerCount = speakerCount,
                    language = _language.value,
                    onProgress = { progress -> _transcribeProgress.value = progress }
                )

                segmentDao.deleteSegmentsForRecording(recording.id)
                segmentDao.insertSegments(segments)
                recordingDao.updateTranscriptionResult(recording.id, fullText, speakerCount)

                _selectedSegments.value = segments
                Toast.makeText(getApplication(), "Transkripsiyon tamamlandı!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(getApplication(), "Transkripsiyon hatası: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                _isTranscribing.value = false
                _transcribeProgress.value = 0f
            }
        }
    }

    fun renameRecording(id: Long, newTitle: String) {
        viewModelScope.launch {
            recordingDao.updateTitle(id, newTitle)
        }
    }

    fun updateTitle(id: Long, newTitle: String) = renameRecording(id, newTitle)

    fun updateThumbnail(id: Long, uriString: String?) {
        viewModelScope.launch {
            recordingDao.updateThumbnail(id, uriString)
        }
    }

    fun deleteRecording(recording: RecordingEntity) {
        viewModelScope.launch {
            recordingDao.deleteRecording(recording.id)
            segmentDao.deleteSegmentsForRecording(recording.id)
            try {
                File(recording.filePath).delete()
            } catch (_: Exception) {}
            if (_selectedRecordingId.value == recording.id) {
                _selectedRecordingId.value = null
                _selectedSegments.value = emptyList()
            }
        }
    }

    fun deleteRecording(id: Long) {
        viewModelScope.launch {
            val rec = recordingDao.getRecordingById(id)
            if (rec != null) {
                deleteRecording(rec)
            } else {
                recordingDao.deleteRecording(id)
                segmentDao.deleteSegmentsForRecording(id)
            }
        }
    }

    fun exportAndSharePdf(context: Context, recording: RecordingEntity) {
        viewModelScope.launch {
            val segments = segmentDao.getSegmentsForRecordingDirect(recording.id)
            val pdfFile = PdfManager.generatePdf(context, recording, segments)
            if (pdfFile != null) {
                PdfManager.sharePdf(context, pdfFile)
            } else {
                Toast.makeText(context, "PDF oluşturulamadı", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun savePdfToStorage(context: Context, recording: RecordingEntity) {
        viewModelScope.launch {
            val segments = segmentDao.getSegmentsForRecordingDirect(recording.id)
            val pdfFile = PdfManager.generatePdf(context, recording, segments)
            if (pdfFile != null) {
                Toast.makeText(context, "PDF kaydedildi: ${pdfFile.name}", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "PDF kaydedilemedi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun backupSelectedRecordingsToDrive(context: Context, items: List<RecordingEntity>) {
        if (items.isEmpty()) {
            Toast.makeText(context, "Lütfen yedeklemek için en az bir döküman seçin", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            val userEmail = authManager.userProfile.value.email
            driveBackupManager.backupRecordings(context, items, userEmail)
        }
    }

    fun backupRecordingToDrive(context: Context, recording: RecordingEntity) {
        backupSelectedRecordingsToDrive(context, listOf(recording))
    }

    fun resetDriveBackupState() {
        driveBackupManager.resetState()
    }

    fun openGoogleDrive(context: Context) {
        val userEmail = authManager.userProfile.value.email
        driveBackupManager.openGoogleDrive(context, userEmail)
    }

    fun shareBackupToDriveApp(context: Context, items: List<RecordingEntity>) {
        val userEmail = authManager.userProfile.value.email
        driveBackupManager.shareBackupToDeviceDriveApp(context, items, userEmail)
    }

    fun updateGoogleAccount(email: String, name: String = "") {
        authManager.setCustomGoogleAccount(email, name)
    }

    fun checkForUpdates(customUrl: String? = null) {
        viewModelScope.launch {
            updateManager.checkForUpdates(customUrl)
        }
    }

    fun downloadAndInstallUpdate(context: Context) {
        viewModelScope.launch {
            updateManager.downloadAndInstallUpdate(context)
        }
    }

    fun simulateUpdateCheck() {
        val nextVersionCode = com.example.BuildConfig.VERSION_CODE + 1
        updateManager.simulateUpdateNotification("1.0.$nextVersionCode")
    }

    fun openWebDownloadPage(context: Context) {
        updateManager.openWebDownloadPage(context)
    }

    fun getCustomUpdateUrl(): String {
        return updateManager.getCustomUpdateUrl()
    }

    fun setCustomUpdateUrl(url: String) {
        updateManager.setCustomUpdateUrl(url)
    }

    fun sendFeedback(
        subject: String,
        category: String,
        message: String,
        userEmail: String,
        audioUri: String? = null,
        imageUri: String? = null,
        pdfUri: String? = null,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val feedback = FeedbackEntity(
                subject = subject,
                category = category,
                message = message,
                timestamp = System.currentTimeMillis(),
                userEmail = userEmail,
                audioUri = audioUri,
                imageUri = imageUri,
                pdfUri = pdfUri
            )
            feedbackDao.insertFeedback(feedback)
            Toast.makeText(getApplication(), "Mesajınız üretici yönetim paneline iletildi. Teşekkürler!", Toast.LENGTH_LONG).show()
            onComplete()
        }
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    fun setLanguage(lang: String) {
        _language.value = lang
        prefs.edit().putString("language", lang).apply()
    }

    fun setAudioFormat(format: String) {
        _audioFormat.value = format
        prefs.edit().putString("audio_format", format).apply()
    }

    fun setAudioBitrate(bitrate: Int) {
        _audioBitrate.value = bitrate
        prefs.edit().putInt("audio_bitrate", bitrate).apply()
    }

    fun setAudioSampleRate(rate: Int) {
        _audioSampleRate.value = rate
        prefs.edit().putInt("audio_sample_rate", rate).apply()
    }

    fun createFolder(name: String, type: String = "PROJECT", colorHex: String = "#2563EB", description: String = "") {
        viewModelScope.launch {
            val trimmed = name.trim()
            if (trimmed.isNotBlank()) {
                folderDao.insertFolder(
                    com.example.data.FolderEntity(
                        name = trimmed,
                        type = type,
                        colorHex = colorHex,
                        description = description
                    )
                )
                Toast.makeText(getApplication(), "'$trimmed' klasörü oluşturuldu.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch {
            folderDao.deleteFolder(folderId)
            Toast.makeText(getApplication(), "Klasör silindi.", Toast.LENGTH_SHORT).show()
        }
    }

    fun renameFolder(folderId: Long, newName: String, oldName: String, type: String) {
        viewModelScope.launch {
            val trimmed = newName.trim()
            if (trimmed.isNotBlank()) {
                folderDao.updateFolderName(folderId, trimmed)
                if (type == "PROJECT") {
                    recordingDao.renameProjectInRecordings(oldName, trimmed)
                    recordingDao.renameFolderInRecordings(oldName, trimmed)
                } else {
                    recordingDao.renameCategoryInRecordings(oldName, trimmed)
                }
                Toast.makeText(getApplication(), "Klasör adı '$trimmed' olarak güncellendi.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun moveRecordingToFolder(recordingId: Long, projectName: String, category: String? = null) {
        viewModelScope.launch {
            recordingDao.updateFolderAndProject(recordingId, projectName, projectName)
            if (category != null && category.isNotBlank()) {
                recordingDao.updateCategory(recordingId, category)
            }
            Toast.makeText(getApplication(), "Kayıt '$projectName' klasörüne taşındı.", Toast.LENGTH_SHORT).show()
        }
    }

    fun moveRecordingsBatch(recordingIds: Set<Long>, projectName: String, category: String? = null) {
        viewModelScope.launch {
            for (id in recordingIds) {
                recordingDao.updateFolderAndProject(id, projectName, projectName)
                if (category != null && category.isNotBlank()) {
                    recordingDao.updateCategory(id, category)
                }
            }
            Toast.makeText(getApplication(), "${recordingIds.size} kayıt '$projectName' klasörüne taşındı.", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteRecordingsBatch(recordingIds: Set<Long>) {
        viewModelScope.launch {
            recordingDao.deleteRecordingsByIds(recordingIds.toList())
            Toast.makeText(getApplication(), "${recordingIds.size} kayıt silindi.", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateRecordingCategory(recordingId: Long, category: String) {
        viewModelScope.launch {
            recordingDao.updateCategory(recordingId, category)
            Toast.makeText(getApplication(), "Kategori '$category' olarak güncellendi.", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateRecordingProject(recordingId: Long, projectName: String) {
        viewModelScope.launch {
            recordingDao.updateFolderAndProject(recordingId, projectName, projectName)
            Toast.makeText(getApplication(), "Proje '$projectName' olarak güncellendi.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.stop()
        recorderManager.stopRecording()
    }
}
