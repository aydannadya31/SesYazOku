package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transcription_notes")
data class TranscriptionNote(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val durationMs: Long = 0,
    val audioFilePath: String? = null,
    val folderId: Long? = null,
    val tagsJson: String = "[]", // JSON array of tag strings
    val summary: String? = null,
    val actionPointsJson: String = "[]", // JSON array of extracted action items
    val syncStatus: String = "SYNCED", // SYNCED, PENDING, SYNCING, LOCAL_ONLY
    val lastSyncedAt: Long? = System.currentTimeMillis(),
    val language: String = "tr",
    val isFavorite: Boolean = false
)
