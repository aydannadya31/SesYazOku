package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val durationMs: Long,
    val fileSize: Long,
    val mimeType: String,
    val sourceType: String, // "LIVE", "APP_CAPTURE", "IMPORTED"
    val timestamp: Long,
    val thumbnailUri: String? = null,
    val isTranscribed: Boolean = false,
    val transcriptFullText: String = "",
    val speakerCount: Int = 1,
    val isBackedUpToDrive: Boolean = false,
    val folderName: String = "Genel",
    val projectName: String = "Genel Kayıtlar",
    val category: String = "Genel"
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String = "PROJECT", // "PROJECT", "CATEGORY"
    val colorHex: String = "#2563EB",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transcript_segments")
data class TranscriptSegmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recordingId: Long,
    val speakerLabel: String, // "Kişi 1", "Kişi 2", "Kişi 3"
    val speakerIndex: Int,
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val confidence: Float = 0.95f
)

@Entity(tableName = "feedbacks")
data class FeedbackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subject: String,
    val category: String, // "Hata Bildirimi", "Öneri / İstek", "İşleyiş Geri Bildirimi", "Diğer"
    val message: String,
    val timestamp: Long,
    val userEmail: String,
    val status: String = "İletildi",
    val audioUri: String? = null,
    val imageUri: String? = null,
    val pdfUri: String? = null
)
