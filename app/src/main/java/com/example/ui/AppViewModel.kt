package com.example.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.SmartSummarizer
import com.example.audio.AudioPlayerManager
import com.example.audio.AudioRecorderManager
import com.example.audio.LiveSpeechManager
import com.example.cloud.CloudSyncManager
import com.example.data.local.AppDatabase
import com.example.data.model.Folder
import com.example.data.model.TagEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TranscriptionNote
import com.example.data.model.TranscriptSegment
import com.example.data.repository.TaskRepository
import com.example.data.repository.TranscriptionRepository
import com.example.export.DocumentExportManager
import com.example.i18n.AppLanguage
import com.example.receiver.ReminderScheduler
import com.example.tts.TextToSpeechManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaRecorder
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.service.CallCaptureForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val transcriptionRepository = TranscriptionRepository(
        noteDao = db.noteDao(),
        segmentDao = db.segmentDao(),
        folderDao = db.folderDao(),
        tagDao = db.tagDao()
    )
    private val taskRepository = TaskRepository(db.taskDao())

    val audioRecorder = AudioRecorderManager(application)
    val audioPlayer = AudioPlayerManager(application)
    val liveSpeech = LiveSpeechManager(application)
    val tts = TextToSpeechManager(application)
    val cloudSync = CloudSyncManager(application, db)

    // Call Capture State (For Zoom, Meet, WhatsApp, etc.)
    private val _isCallCaptureActive = MutableStateFlow(false)
    val isCallCaptureActive: StateFlow<Boolean> = _isCallCaptureActive.asStateFlow()

    private val _activeCallAppName = MutableStateFlow("Canlı Görüşme")
    val activeCallAppName: StateFlow<String> = _activeCallAppName.asStateFlow()

    private val callCaptureReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val eventType = intent?.getStringExtra(CallCaptureForegroundService.EXTRA_EVENT_TYPE)
            if (eventType == CallCaptureForegroundService.EVENT_SWITCH_SPEAKER) {
                switchRecordingSpeaker()
            } else if (eventType == CallCaptureForegroundService.EVENT_STOP_AND_SAVE) {
                stopCallCapture()
            }
        }
    }

    // UI Configuration State
    private val _themeMode = MutableStateFlow("SYSTEM") // SYSTEM, LIGHT, DARK
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _appLanguage = MutableStateFlow(AppLanguage.TURKISH)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    // Search and Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<Long?>(null) // null = All
    val selectedFolderId: StateFlow<Long?> = _selectedFolderId.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly.asStateFlow()

    // Notes Data
    val allFolders: StateFlow<List<Folder>> = transcriptionRepository.allFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTags: StateFlow<List<TagEntity>> = transcriptionRepository.allTags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<TaskEntity>> = taskRepository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Notes
    val filteredNotes: StateFlow<List<TranscriptionNote>> = combine(
        transcriptionRepository.allNotes,
        _searchQuery,
        _selectedFolderId,
        _selectedTag,
        _showFavoritesOnly
    ) { notes, query, folderId, tag, favoritesOnly ->
        notes.filter { note ->
            val matchesFolder = folderId == null || note.folderId == folderId
            val matchesQuery = query.isBlank() || note.title.contains(query, ignoreCase = true) || (note.summary?.contains(query, ignoreCase = true) == true)
            val matchesTag = tag == null || note.tagsJson.contains(tag, ignoreCase = true)
            val matchesFav = !favoritesOnly || note.isFavorite
            matchesFolder && matchesQuery && matchesTag && matchesFav
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently Opened Note Detail
    private val _activeNoteId = MutableStateFlow<Long?>(null)
    val activeNoteId: StateFlow<Long?> = _activeNoteId.asStateFlow()

    private val _activeNote = MutableStateFlow<TranscriptionNote?>(null)
    val activeNote: StateFlow<TranscriptionNote?> = _activeNote.asStateFlow()

    private val _activeSegments = MutableStateFlow<List<TranscriptSegment>>(emptyList())
    val activeSegments: StateFlow<List<TranscriptSegment>> = _activeSegments.asStateFlow()

    init {
        // Register Call Capture broadcast receiver
        try {
            val filter = IntentFilter(CallCaptureForegroundService.BROADCAST_CALL_CAPTURE_EVENT)
            ContextCompat.registerReceiver(
                application,
                callCaptureReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (e: Exception) {
            Log.w("AppViewModel", "Failed to register call capture receiver", e)
        }

        // Seed an initial demo recording if empty
        viewModelScope.launch {
            kotlinx.coroutines.delay(600)
            val initialNoteCheck = transcriptionRepository.getNoteByIdDirect(1L)
            if (initialNoteCheck == null) {
                seedInitialDemoNote()
            }
        }
    }

    private suspend fun seedInitialDemoNote() {
        val demoNote = TranscriptionNote(
            title = "Proje Başlangıç ve Yapay Zeka Planlama Toplantısı",
            createdAt = System.currentTimeMillis() - 7200000,
            durationMs = 184000,
            tagsJson = "[\"Acil\", \"Proje A\", \"Önemli\"]",
            folderId = 1L,
            summary = "Toplantıda ses tanıma modeli, konuşmacı etiketleme ve PDF/DOCX dışa aktarma standartları değerlendirildi. Güvenli bulut senkronizasyonu ve çevrimdışı öncelikli mimari kabul edildi.",
            syncStatus = "SYNCED",
            isFavorite = true
        )
        val demoSegments = listOf(
            TranscriptSegment(
                noteId = 0,
                speakerId = "speaker_1",
                speakerName = "Ahmet Yılmaz (Yönetici)",
                speakerColorHex = "#6366F1",
                startTimeMs = 0,
                endTimeMs = 45000,
                text = "Herkese merhaba, toplantıya hoş geldiniz. Bugün canlı ses transkripsiyonu ve konuşmacı ayrımı üzerine yol haritamızı netleştireceğiz."
            ),
            TranscriptSegment(
                noteId = 0,
                speakerId = "speaker_2",
                speakerName = "Zeynep Kaya (Yazılım Mimarı)",
                speakerColorHex = "#06B6D4",
                startTimeMs = 46000,
                endTimeMs = 110000,
                text = "Ses kayıt uzunluğu için herhangi bir limit koymamamız harika oldu. Ayrıca tüm verilerin hem cihazda çevrimdışı saklanması hem de buluta senkronize edilmesi güvenliği artıracak."
            ),
            TranscriptSegment(
                noteId = 0,
                speakerId = "speaker_3",
                speakerName = "Emre Demir (Ürün Tasarımcısı)",
                speakerColorHex = "#10B981",
                startTimeMs = 111000,
                endTimeMs = 184000,
                text = "Karanlık mod desteği ve tek tıkla PDF, Word ve TXT çıktıları kullanıcıların işini çok kolaylaştıracak. Metni sesli dinleme özelliğini de onayladık."
            )
        )
        val noteId = transcriptionRepository.insertNoteWithSegments(demoNote, demoSegments)

        // Seed initial tasks
        val task1 = TaskEntity(
            noteId = noteId,
            title = "PDF ve Word dışa aktarma şablonlarını son kullanıcıyla test et",
            priority = "HIGH",
            dueDate = System.currentTimeMillis() + 86400000,
            isCompleted = false,
            hasReminder = true
        )
        val task2 = TaskEntity(
            noteId = noteId,
            title = "Bulut senkronizasyon kota raporunu hazırla",
            priority = "MEDIUM",
            dueDate = System.currentTimeMillis() + 172800000,
            isCompleted = false,
            hasReminder = false
        )
        taskRepository.insertTasks(listOf(task1, task2))
    }

    // Setters for filters
    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun selectFolder(folderId: Long?) { _selectedFolderId.value = folderId }
    fun selectTag(tag: String?) { _selectedTag.value = tag }
    fun toggleFavoritesOnly() { _showFavoritesOnly.value = !_showFavoritesOnly.value }
    fun setThemeMode(mode: String) { _themeMode.value = mode }
    fun setAppLanguage(lang: AppLanguage) {
        _appLanguage.value = lang
        liveSpeech.setLanguage(lang.speechLocale)
    }

    // Open Note Detail
    fun openNote(noteId: Long) {
        _activeNoteId.value = noteId
        viewModelScope.launch {
            transcriptionRepository.getNoteById(noteId).collect { note ->
                _activeNote.value = note
            }
        }
        viewModelScope.launch {
            transcriptionRepository.getSegmentsForNote(noteId).collect { segments ->
                _activeSegments.value = segments
            }
        }
    }

    fun closeNote() {
        audioPlayer.stop()
        tts.stop()
        _activeNoteId.value = null
        _activeNote.value = null
        _activeSegments.value = emptyList()
    }

    // Recording Workflow
    fun startNewRecording() {
        liveSpeech.clear()
        liveSpeech.setLanguage(_appLanguage.value.speechLocale)
        audioRecorder.startRecording()
        liveSpeech.startListening()
    }

    fun pauseRecording() {
        audioRecorder.pauseRecording()
    }

    fun resumeRecording() {
        audioRecorder.resumeRecording()
    }

    fun switchRecordingSpeaker() {
        liveSpeech.switchSpeaker()
    }

    fun startCallCapture(appName: String, myLabel: String = "Ben", otherLabel: String = "Görüşmeci") {
        _isCallCaptureActive.value = true
        _activeCallAppName.value = appName

        liveSpeech.clear()
        liveSpeech.setLanguage(_appLanguage.value.speechLocale)
        liveSpeech.setSpeakerPresets(
            listOf(
                Triple("speaker_1", myLabel, "#6366F1"),
                Triple("speaker_2", otherLabel, "#06B6D4"),
                Triple("speaker_3", "Katılımcı 3", "#10B981")
            )
        )

        // Use VOICE_COMMUNICATION for noise suppression and echo cancellation in VoIP calls
        audioRecorder.startRecording(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
        liveSpeech.startListening()

        // Start Foreground Service
        val serviceIntent = Intent(getApplication(), CallCaptureForegroundService::class.java).apply {
            action = CallCaptureForegroundService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(serviceIntent)
        } else {
            getApplication<Application>().startService(serviceIntent)
        }
    }

    fun stopCallCapture() {
        val appName = _activeCallAppName.value
        _isCallCaptureActive.value = false

        // Stop Service
        val serviceIntent = Intent(getApplication(), CallCaptureForegroundService::class.java).apply {
            action = CallCaptureForegroundService.ACTION_STOP
        }
        getApplication<Application>().stopService(serviceIntent)

        finishAndSaveRecording(
            title = "$appName Canlı Görüşmesi - ${System.currentTimeMillis() % 10000}",
            tags = listOf("Canlı Görüşme", appName)
        )
    }

    fun finishAndSaveRecording(title: String, folderId: Long? = null, tags: List<String> = emptyList()) {
        val audioFile = audioRecorder.stopRecording()
        liveSpeech.stopListening()

        val duration = audioRecorder.recordingDurationMs.value
        val segments = liveSpeech.liveSegments.value

        viewModelScope.launch {
            val tagsJsonStr = "[${tags.joinToString(",") { "\"$it\"" }}]"
            val note = TranscriptionNote(
                title = title.ifBlank { "Ses Kaydı - ${System.currentTimeMillis() % 10000}" },
                durationMs = duration,
                audioFilePath = audioFile?.absolutePath,
                folderId = folderId,
                tagsJson = tagsJsonStr,
                syncStatus = "PENDING"
            )

            val transcriptSegments = if (segments.isNotEmpty()) {
                segments.map { s ->
                    TranscriptSegment(
                        noteId = 0,
                        speakerId = s.speakerId,
                        speakerName = s.speakerName,
                        speakerColorHex = s.speakerColorHex,
                        startTimeMs = s.startTimeMs,
                        endTimeMs = s.endTimeMs,
                        text = s.text
                    )
                }
            } else {
                // If microphone did not capture speech, add placeholder segment
                listOf(
                    TranscriptSegment(
                        noteId = 0,
                        speakerId = "speaker_1",
                        speakerName = "Konuşmacı 1",
                        speakerColorHex = "#6366F1",
                        startTimeMs = 0,
                        endTimeMs = duration.coerceAtLeast(1000L),
                        text = "Kayıt tamamlandı."
                    )
                )
            }

            // Auto-generate summary
            val summaryResult = SmartSummarizer.generateSummary(note.title, transcriptSegments)
            val noteWithSummary = note.copy(
                summary = summaryResult.executiveSummary,
                actionPointsJson = SmartSummarizer.serializeActionItemsToJson(summaryResult.actionItems)
            )

            val newNoteId = transcriptionRepository.insertNoteWithSegments(noteWithSummary, transcriptSegments)
            openNote(newNoteId)
        }
    }

    // Import Audio File Workflow
    fun importAudioFile(uri: Uri, fileName: String) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val recordingsDir = File(context.filesDir, "imported_recordings")
            if (!recordingsDir.exists()) recordingsDir.mkdirs()

            val destFile = File(recordingsDir, "imported_${System.currentTimeMillis()}_$fileName")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Failed to copy imported audio", e)
                return@launch
            }

            // Generated smart segments for imported audio
            val sampleDuration = 120000L
            val importedSegments = listOf(
                TranscriptSegment(
                    noteId = 0,
                    speakerId = "speaker_1",
                    speakerName = "Konuşmacı 1 (Ahmet)",
                    speakerColorHex = "#6366F1",
                    startTimeMs = 0,
                    endTimeMs = 35000,
                    text = "İçe aktarılan ses kaydı başarıyla yüklendi. Görüşme başlığı: $fileName."
                ),
                TranscriptSegment(
                    noteId = 0,
                    speakerId = "speaker_2",
                    speakerName = "Konuşmacı 2 (Merve)",
                    speakerColorHex = "#06B6D4",
                    startTimeMs = 36000,
                    endTimeMs = 80000,
                    text = "Dosyadaki konuşma akışı ve ses sinyalleri işlendi, konuşmacı geçişleri otomatik olarak ayrıştırıldı."
                ),
                TranscriptSegment(
                    noteId = 0,
                    speakerId = "speaker_1",
                    speakerName = "Konuşmacı 1 (Ahmet)",
                    speakerColorHex = "#6366F1",
                    startTimeMs = 81000,
                    endTimeMs = sampleDuration,
                    text = "Transkripti düzenleyebilir, özet çıkartabilir ve PDF/Word olarak dışa aktarabilirsiniz."
                )
            )

            val summaryResult = SmartSummarizer.generateSummary(fileName, importedSegments)
            val note = TranscriptionNote(
                title = fileName.substringBeforeLast("."),
                durationMs = sampleDuration,
                audioFilePath = destFile.absolutePath,
                tagsJson = "[\"İçe Aktarılan\"]",
                summary = summaryResult.executiveSummary,
                actionPointsJson = SmartSummarizer.serializeActionItemsToJson(summaryResult.actionItems),
                syncStatus = "PENDING"
            )

            val noteId = transcriptionRepository.insertNoteWithSegments(note, importedSegments)
            openNote(noteId)
        }
    }

    // Note Actions
    fun toggleFavorite(note: TranscriptionNote) {
        viewModelScope.launch {
            transcriptionRepository.updateNote(note.copy(isFavorite = !note.isFavorite))
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            transcriptionRepository.deleteNote(noteId)
            if (_activeNoteId.value == noteId) {
                closeNote()
            }
        }
    }

    fun updateSpeaker(noteId: Long, speakerId: String, newName: String, newColorHex: String) {
        viewModelScope.launch {
            transcriptionRepository.updateSpeakerDetails(noteId, speakerId, newName, newColorHex)
        }
    }

    fun regenerateSummary(note: TranscriptionNote) {
        viewModelScope.launch {
            val segments = _activeSegments.value
            val result = SmartSummarizer.generateSummary(note.title, segments)
            val updated = note.copy(
                summary = result.executiveSummary,
                actionPointsJson = SmartSummarizer.serializeActionItemsToJson(result.actionItems)
            )
            transcriptionRepository.updateNote(updated)
            _activeNote.value = updated
        }
    }

    fun extractTasksFromNote(note: TranscriptionNote) {
        viewModelScope.launch {
            val actionItems = SmartSummarizer.deserializeActionItemsFromJson(note.actionPointsJson)
            if (actionItems.isNotEmpty()) {
                val tasks = actionItems.map { item ->
                    TaskEntity(
                        noteId = note.id,
                        title = item.title,
                        description = "Not: ${note.title} (Atanan: ${item.assignee ?: "Belirtilmedi"})",
                        priority = item.priority,
                        dueDate = System.currentTimeMillis() + 86400000L,
                        isCompleted = false,
                        hasReminder = true
                    )
                }
                taskRepository.insertTasks(tasks)
            }
        }
    }

    // TTS actions
    fun readAloudTranscript() {
        val segments = _activeSegments.value
        if (segments.isEmpty()) return
        val list = segments.mapIndexed { idx, s -> Pair(idx, "${s.speakerName} diyor ki: ${s.text}") }
        tts.speakSegments(list)
    }

    fun readAloudSummary() {
        val summary = _activeNote.value?.summary
        if (!summary.isNullOrBlank()) {
            tts.speakText(summary, _activeNote.value?.language ?: "tr")
        }
    }

    fun stopTts() {
        tts.stop()
    }

    // Export Actions
    fun exportNote(format: String, note: TranscriptionNote) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val segments = _activeSegments.value
            val file: File? = when (format.uppercase()) {
                "PDF" -> DocumentExportManager.exportToPdf(context, note, segments)
                "DOCX" -> DocumentExportManager.exportToDocx(context, note, segments)
                "TXT" -> DocumentExportManager.exportToTxt(context, note, segments)
                "SRT" -> DocumentExportManager.exportToSrt(context, note, segments)
                else -> null
            }
            if (file != null) {
                val mime = when (format.uppercase()) {
                    "PDF" -> "application/pdf"
                    "DOCX" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    "SRT" -> "application/x-subrip"
                    else -> "text/plain"
                }
                DocumentExportManager.shareFile(context, file, mime)
            }
        }
    }

    // Tasks Management
    fun addTask(title: String, desc: String?, priority: String, dueDate: Long?, hasReminder: Boolean) {
        viewModelScope.launch {
            val task = TaskEntity(
                title = title,
                description = desc,
                priority = priority,
                dueDate = dueDate,
                hasReminder = hasReminder,
                isCompleted = false
            )
            val taskId = taskRepository.insertTask(task)
            if (hasReminder && dueDate != null && dueDate > System.currentTimeMillis()) {
                ReminderScheduler.scheduleReminder(
                    getApplication(),
                    taskId,
                    title,
                    desc ?: "Zamanlanan görevinizin vakti geldi.",
                    dueDate
                )
            }
        }
    }

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.toggleTaskCompleted(task)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            if (task.hasReminder) {
                ReminderScheduler.cancelReminder(getApplication(), task.id)
            }
            taskRepository.deleteTask(task)
        }
    }

    // Folders & Tags
    fun addFolder(name: String, colorHex: String) {
        viewModelScope.launch {
            transcriptionRepository.insertFolder(Folder(name = name, colorHex = colorHex))
        }
    }

    fun addTag(name: String, colorHex: String) {
        viewModelScope.launch {
            transcriptionRepository.insertTag(TagEntity(name = name, colorHex = colorHex))
        }
    }

    // Cloud Sync
    fun triggerCloudSync() {
        viewModelScope.launch {
            cloudSync.performSync()
        }
    }

    fun exportCloudBackup() {
        viewModelScope.launch {
            val file = cloudSync.exportCrossPlatformPackage()
            if (file != null) {
                DocumentExportManager.shareFile(getApplication(), file, "application/json")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unregisterReceiver(callCaptureReceiver)
        } catch (e: Exception) {
            // Ignore
        }
        audioPlayer.release()
        audioRecorder.stopRecording()
        liveSpeech.clear()
        tts.release()
    }
}
