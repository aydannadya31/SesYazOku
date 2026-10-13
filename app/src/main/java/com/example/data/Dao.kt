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

    @Query("UPDATE recordings SET thumbnailUri = :thumbnailUri WHERE id = :id")
    suspend fun updateThumbnail(id: Long, thumbnailUri: String?)

    @Query("UPDATE recordings SET isBackedUpToDrive = :isBackedUp WHERE id = :id")
    suspend fun updateDriveBackupStatus(id: Long, isBackedUp: Boolean)

    @Query("UPDATE recordings SET isTranscribed = 1, transcriptFullText = :fullText, speakerCount = :speakerCount WHERE id = :id")
    suspend fun updateTranscriptionResult(id: Long, fullText: String, speakerCount: Int)
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
}

@Dao
interface FeedbackDao {
    @Query("SELECT * FROM feedbacks ORDER BY timestamp DESC")
    fun getAllFeedbacks(): Flow<List<FeedbackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: FeedbackEntity): Long
}
