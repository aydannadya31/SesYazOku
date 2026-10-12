package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TranscriptSegment
import kotlinx.coroutines.flow.Flow

@Dao
interface SegmentDao {
    @Query("SELECT * FROM transcript_segments WHERE noteId = :noteId ORDER BY startTimeMs ASC, id ASC")
    fun getSegmentsForNote(noteId: Long): Flow<List<TranscriptSegment>>

    @Query("SELECT * FROM transcript_segments WHERE noteId = :noteId ORDER BY startTimeMs ASC, id ASC")
    suspend fun getSegmentsForNoteDirect(noteId: Long): List<TranscriptSegment>

    @Query("SELECT * FROM transcript_segments WHERE text LIKE '%' || :query || '%'")
    suspend fun searchSegments(query: String): List<TranscriptSegment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegment(segment: TranscriptSegment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegments(segments: List<TranscriptSegment>)

    @Update
    suspend fun updateSegment(segment: TranscriptSegment)

    @Delete
    suspend fun deleteSegment(segment: TranscriptSegment)

    @Query("DELETE FROM transcript_segments WHERE noteId = :noteId")
    suspend fun deleteSegmentsForNote(noteId: Long)

    @Query("UPDATE transcript_segments SET speakerName = :newName, speakerColorHex = :newColor WHERE noteId = :noteId AND speakerId = :speakerId")
    suspend fun updateSpeakerDetails(noteId: Long, speakerId: String, newName: String, newColor: String)
}
