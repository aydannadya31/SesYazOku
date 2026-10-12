package com.example.data.repository

import com.example.data.local.FolderDao
import com.example.data.local.NoteDao
import com.example.data.local.SegmentDao
import com.example.data.local.TagDao
import com.example.data.model.Folder
import com.example.data.model.TagEntity
import com.example.data.model.TranscriptionNote
import com.example.data.model.TranscriptSegment
import kotlinx.coroutines.flow.Flow

class TranscriptionRepository(
    private val noteDao: NoteDao,
    private val segmentDao: SegmentDao,
    private val folderDao: FolderDao,
    private val tagDao: TagDao
) {
    val allNotes: Flow<List<TranscriptionNote>> = noteDao.getAllNotes()
    val allFolders: Flow<List<Folder>> = folderDao.getAllFolders()
    val allTags: Flow<List<TagEntity>> = tagDao.getAllTags()

    fun getNoteById(id: Long): Flow<TranscriptionNote?> = noteDao.getNoteById(id)
    suspend fun getNoteByIdDirect(id: Long): TranscriptionNote? = noteDao.getNoteByIdDirect(id)

    fun getSegmentsForNote(noteId: Long): Flow<List<TranscriptSegment>> = segmentDao.getSegmentsForNote(noteId)
    suspend fun getSegmentsForNoteDirect(noteId: Long): List<TranscriptSegment> = segmentDao.getSegmentsForNoteDirect(noteId)

    fun getNotesByFolder(folderId: Long): Flow<List<TranscriptionNote>> = noteDao.getNotesByFolder(folderId)
    fun getFavoriteNotes(): Flow<List<TranscriptionNote>> = noteDao.getFavoriteNotes()
    fun searchNotes(query: String): Flow<List<TranscriptionNote>> = noteDao.searchNotes(query)

    suspend fun insertNoteWithSegments(note: TranscriptionNote, segments: List<TranscriptSegment>): Long {
        val noteId = noteDao.insertNote(note)
        val segmentsWithNoteId = segments.map { it.copy(noteId = noteId) }
        segmentDao.insertSegments(segmentsWithNoteId)
        return noteId
    }

    suspend fun updateNote(note: TranscriptionNote) = noteDao.updateNote(note)
    suspend fun deleteNote(id: Long) = noteDao.deleteNoteById(id)

    suspend fun updateSpeakerDetails(noteId: Long, speakerId: String, newName: String, newColor: String) {
        segmentDao.updateSpeakerDetails(noteId, speakerId, newName, newColor)
    }

    suspend fun insertFolder(folder: Folder): Long = folderDao.insertFolder(folder)
    suspend fun deleteFolder(id: Long) = folderDao.deleteFolderById(id)

    suspend fun insertTag(tag: TagEntity): Long = tagDao.insertTag(tag)
    suspend fun deleteTag(tag: TagEntity) = tagDao.deleteTag(tag)

    suspend fun getAllFoldersDirect(): List<Folder> = folderDao.getAllFoldersDirect()
    suspend fun getAllTagsDirect(): List<TagEntity> = tagDao.getAllTagsDirect()
}
