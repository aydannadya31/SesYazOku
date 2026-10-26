package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY timestamp DESC")
    fun getAllRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE id = :id LIMIT 1")
    suspend fun getRecordingById(id: Long): RecordingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: RecordingEntity): Long

    @Update
    suspend fun updateRecording(recording: RecordingEntity)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteRecording(id: Long)

    @Query("UPDATE recordings SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String)

    @Query("UPDATE recordings SET filePath = :filePath WHERE id = :id")
    suspend fun updateFilePath(id: Long, filePath: String)

    @Query("UPDATE recordings SET thumbnailUri = :thumbnailUri WHERE id = :id")
    suspend fun updateThumbnail(id: Long, thumbnailUri: String?)

    @Query("UPDATE recordings SET isBackedUpToDrive = :isBackedUp WHERE id = :id")
    suspend fun updateDriveBackupStatus(id: Long, isBackedUp: Boolean)

    @Query("UPDATE recordings SET isTranscribed = 1, transcriptFullText = :fullText, speakerCount = :speakerCount WHERE id = :id")
    suspend fun updateTranscriptionResult(id: Long, fullText: String, speakerCount: Int)

    @Query("UPDATE recordings SET folderName = :folderName, projectName = :projectName WHERE id = :id")
    suspend fun updateFolderAndProject(id: Long, folderName: String, projectName: String)

    @Query("UPDATE recordings SET folderName = :folderName WHERE id = :id")
    suspend fun updateFolder(id: Long, folderName: String)

    @Query("UPDATE recordings SET projectName = :projectName WHERE id = :id")
    suspend fun updateProject(id: Long, projectName: String)

    @Query("UPDATE recordings SET category = :category WHERE id = :id")
    suspend fun updateCategory(id: Long, category: String)

    @Query("UPDATE recordings SET folderName = :newFolderName WHERE folderName = :oldFolderName")
    suspend fun renameFolderInRecordings(oldFolderName: String, newFolderName: String)

    @Query("UPDATE recordings SET projectName = :newProjectName WHERE projectName = :oldProjectName")
    suspend fun renameProjectInRecordings(oldProjectName: String, newProjectName: String)

    @Query("UPDATE recordings SET category = :newCategory WHERE category = :oldCategory")
    suspend fun renameCategoryInRecordings(oldCategory: String, newCategory: String)

    @Query("DELETE FROM recordings WHERE id IN (:ids)")
    suspend fun deleteRecordingsByIds(ids: List<Long>)
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolder(id: Long)

    @Query("UPDATE folders SET name = :name WHERE id = :id")
    suspend fun updateFolderName(id: Long, name: String)
}

@Dao
interface TranscriptSegmentDao {
    @Query("SELECT * FROM transcript_segments WHERE recordingId = :recordingId ORDER BY startMs ASC")
    fun getSegmentsForRecording(recordingId: Long): Flow<List<TranscriptSegmentEntity>>

    @Query("SELECT * FROM transcript_segments WHERE recordingId = :recordingId ORDER BY startMs ASC")
    suspend fun getSegmentsForRecordingDirect(recordingId: Long): List<TranscriptSegmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegments(segments: List<TranscriptSegmentEntity>)

    @Query("DELETE FROM transcript_segments WHERE recordingId = :recordingId")
    suspend fun deleteSegmentsForRecording(recordingId: Long)

    @Query("UPDATE transcript_segments SET speakerLabel = :newLabel WHERE recordingId = :recordingId AND speakerIndex = :speakerIndex")
    suspend fun updateSpeakerLabel(recordingId: Long, speakerIndex: Int, newLabel: String)

    @Query("UPDATE transcript_segments SET speakerLabel = :newLabel WHERE recordingId = :recordingId AND speakerLabel = :oldLabel")
    suspend fun updateSpeakerLabelByExactName(recordingId: Long, oldLabel: String, newLabel: String)

    @Query("UPDATE transcript_segments SET speakerLabel = :newLabel WHERE id = :segmentId")
    suspend fun updateSingleSegmentSpeaker(segmentId: Long, newLabel: String)

    @Query("UPDATE transcript_segments SET text = :newText WHERE id = :segmentId")
    suspend fun updateSegmentText(segmentId: Long, newText: String)
}

@Dao
interface FeedbackDao {
    @Query("SELECT * FROM feedbacks ORDER BY timestamp DESC")
    fun getAllFeedbacks(): Flow<List<FeedbackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: FeedbackEntity): Long

    @Query("DELETE FROM feedbacks WHERE id = :id")
    suspend fun deleteFeedback(id: Long)
}
