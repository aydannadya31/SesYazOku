package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TranscriptionNote
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM transcription_notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<TranscriptionNote>>

    @Query("SELECT * FROM transcription_notes WHERE id = :id")
    fun getNoteById(id: Long): Flow<TranscriptionNote?>

    @Query("SELECT * FROM transcription_notes WHERE id = :id")
    suspend fun getNoteByIdDirect(id: Long): TranscriptionNote?

    @Query("SELECT * FROM transcription_notes WHERE folderId = :folderId ORDER BY createdAt DESC")
    fun getNotesByFolder(folderId: Long): Flow<List<TranscriptionNote>>

    @Query("SELECT * FROM transcription_notes WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteNotes(): Flow<List<TranscriptionNote>>

    @Query("""
        SELECT * FROM transcription_notes 
        WHERE title LIKE '%' || :query || '%' 
        OR summary LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun searchNotes(query: String): Flow<List<TranscriptionNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: TranscriptionNote): Long

    @Update
    suspend fun updateNote(note: TranscriptionNote)

    @Delete
    suspend fun deleteNote(note: TranscriptionNote)

    @Query("DELETE FROM transcription_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("SELECT COUNT(*) FROM transcription_notes")
    fun getNoteCount(): Flow<Int>
}
