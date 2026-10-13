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
import com.example.export.PdfManager
import com.example.transcription.SpeechTranscriptionEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val recordingDao = db.recordingDao()
    private val segmentDao = db.transcriptSegmentDao()
    private val feedbackDao = db.feedbackDao()

    val recorderManager = AudioRecorderManager(application)
    val playerManager = AudioPlayerManager(application)
    val authManager = AuthManager(application)

    val recordings: StateFlow<List<RecordingEntity>> = recordingDao.getAllRecordings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val feedbacks: StateFlow<List<FeedbackEntity>> = feedbackDao.getAllFeedbacks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun startLiveRecording() {
        recorderManager.startRecording(
            audioFormat = _audioFormat.value,
            audioBitrate = _audioBitrate.value,
            sampleRate = _audioSampleRate.value
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

    fun backupRecordingToDrive(context: Context, recording: RecordingEntity) {
        viewModelScope.launch {
            val segments = segmentDao.getSegmentsForRecordingDirect(recording.id)
            val pdfFile = PdfManager.generatePdf(context, recording, segments)
            recordingDao.updateDriveBackupStatus(recording.id, true)
            PdfManager.backupToDrive(context, recording, pdfFile)
            Toast.makeText(context, "Google Drive yedekleme başlatıldı!", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendFeedback(subject: String, category: String, message: String, userEmail: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val feedback = FeedbackEntity(
                subject = subject,
                category = category,
                message = message,
                timestamp = System.currentTimeMillis(),
                userEmail = userEmail
            )
            feedbackDao.insertFeedback(feedback)
            Toast.makeText(getApplication(), "Mesajınız üreticiye iletildi. Teşekkürler!", Toast.LENGTH_LONG).show()
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

    override fun onCleared() {
        super.onCleared()
        playerManager.stop()
        recorderManager.stopRecording()
    }
}
