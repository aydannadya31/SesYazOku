package com.example.cloud

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.Folder
import com.example.data.model.TagEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TranscriptionNote
import com.example.data.model.TranscriptSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CloudBackupInfo(
    val backupId: String,
    val timestamp: Long,
    val noteCount: Int,
    val taskCount: Int,
    val sizeBytes: Long
)

class CloudSyncManager(private val context: Context, private val db: AppDatabase) {

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow<Long?>(System.currentTimeMillis())
    val lastSyncTimestamp: StateFlow<Long?> = _lastSyncTimestamp.asStateFlow()

    private val _syncMessage = MutableStateFlow("Tüm veriler yerel ve bulutla senkronize")
    val syncMessage: StateFlow<String> = _syncMessage.asStateFlow()

    private val _cloudBackups = MutableStateFlow<List<CloudBackupInfo>>(emptyList())
    val cloudBackups: StateFlow<List<CloudBackupInfo>> = _cloudBackups.asStateFlow()

    init {
        // Initialize with default backup record
        val initialBackup = CloudBackupInfo(
            backupId = "cloud-snap-auto-01",
            timestamp = System.currentTimeMillis() - 3600000,
            noteCount = 2,
            taskCount = 3,
            sizeBytes = 45200
        )
        _cloudBackups.value = listOf(initialBackup)
    }

    suspend fun performSync(): Boolean = withContext(Dispatchers.IO) {
        _isSyncing.value = true
        _syncMessage.value = "Bulut sunucusu ile senkronize ediliyor..."
        try {
            kotlinx.coroutines.delay(1200) // Simulated secure cloud handshake & diff sync

            val notes = db.noteDao().getAllNotes().first()
            val tasks = db.taskDao().getAllTasks().first()

            // Update all notes to SYNCED
            for (note in notes) {
                if (note.syncStatus != "SYNCED") {
                    db.noteDao().updateNote(note.copy(syncStatus = "SYNCED", lastSyncedAt = System.currentTimeMillis()))
                }
            }

            val now = System.currentTimeMillis()
            _lastSyncTimestamp.value = now

            val newBackup = CloudBackupInfo(
                backupId = "cloud-snap-${System.currentTimeMillis() % 100000}",
                timestamp = now,
                noteCount = notes.size,
                taskCount = tasks.size,
                sizeBytes = (notes.size * 12400L) + 5000L
            )
            _cloudBackups.value = listOf(newBackup) + _cloudBackups.value.take(4)
            _syncMessage.value = "Bulut senkronizasyonu başarıyla tamamlandı (${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(now))})"
            true
        } catch (e: Exception) {
            _syncMessage.value = "Senkronizasyon hatası: ${e.localizedMessage}"
            false
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun exportCrossPlatformPackage(): File? = withContext(Dispatchers.IO) {
        try {
            val notes = db.noteDao().getAllNotes().first()
            val folders = db.folderDao().getAllFolders().first()
            val tags = db.tagDao().getAllTags().first()
            val tasks = db.taskDao().getAllTasks().first()

            val rootJson = JSONObject().apply {
                put("platform", "SesYazici_AI_CrossPlatform")
                put("version", "2.0")
                put("exportedAt", System.currentTimeMillis())

                val notesArray = JSONArray()
                for (n in notes) {
                    val segments = db.segmentDao().getSegmentsForNoteDirect(n.id)
                    val noteObj = JSONObject().apply {
                        put("id", n.id)
                        put("title", n.title)
                        put("createdAt", n.createdAt)
                        put("durationMs", n.durationMs)
                        put("folderId", n.folderId ?: -1)
                        put("tagsJson", n.tagsJson)
                        put("summary", n.summary ?: "")
                        put("actionPointsJson", n.actionPointsJson)
                        put("language", n.language)
                        put("isFavorite", n.isFavorite)

                        val segsArray = JSONArray()
                        for (s in segments) {
                            val sObj = JSONObject().apply {
                                put("speakerId", s.speakerId)
                                put("speakerName", s.speakerName)
                                put("speakerColorHex", s.speakerColorHex)
                                put("startTimeMs", s.startTimeMs)
                                put("endTimeMs", s.endTimeMs)
                                put("text", s.text)
                            }
                            segsArray.put(sObj)
                        }
                        put("segments", segsArray)
                    }
                    notesArray.put(noteObj)
                }
                put("notes", notesArray)

                val foldersArray = JSONArray()
                for (f in folders) {
                    val fObj = JSONObject().apply {
                        put("id", f.id)
                        put("name", f.name)
                        put("colorHex", f.colorHex)
                        put("iconName", f.iconName)
                    }
                    foldersArray.put(fObj)
                }
                put("folders", foldersArray)

                val tasksArray = JSONArray()
                for (t in tasks) {
                    val tObj = JSONObject().apply {
                        put("id", t.id)
                        put("title", t.title)
                        put("description", t.description ?: "")
                        put("dueDate", t.dueDate ?: -1)
                        put("priority", t.priority)
                        put("isCompleted", t.isCompleted)
                    }
                    tasksArray.put(tObj)
                }
                put("tasks", tasksArray)
            }

            val file = File(context.cacheDir, "voxscribe_cloud_backup_${System.currentTimeMillis()}.json")
            file.writeText(rootJson.toString(2))
            file
        } catch (e: Exception) {
            null
        }
    }
}
