package com.example.transcription

import android.content.Context
import android.media.MediaMetadataRetriever
import com.example.data.TranscriptSegmentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

object SpeechTranscriptionEngine {

    suspend fun transcribeAudio(
        context: Context,
        recordingId: Long,
        filePath: String,
        targetSpeakerCount: Int = 2,
        onProgress: (Float) -> Unit
    ): Pair<String, List<TranscriptSegmentEntity>> = withContext(Dispatchers.Default) {
        val file = File(filePath)
        var durationMs = 15000L

        if (file.exists()) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(file.absolutePath)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                durationMs = durationStr?.toLongOrNull() ?: 15000L
                retriever.release()
            } catch (_: Exception) {
            }
        }

        // Simulate multi-phase speech analysis and speaker acoustic diarization
        for (step in 1..5) {
            delay(150)
            onProgress(step / 5f)
        }

        val speakerPool = when {
            targetSpeakerCount <= 1 -> listOf("Kişi 1")
            targetSpeakerCount == 2 -> listOf("Kişi 1", "Kişi 2")
            targetSpeakerCount == 3 -> listOf("Kişi 1", "Kişi 2", "Kişi 3")
            else -> (1..targetSpeakerCount).map { "Kişi $it" }
        }

        val speechLibrary = listOf(
            "Merhaba, toplantıya ve ses kaydına hoş geldiniz. Konuları madde madde inceleyelim.",
            "Merhabalar, evet notları hazırladım. Özellikle proje takvimi ve teslim süresi üzerinde duralım.",
            "Ben de bu konuya ekleme yapmak istiyorum. Dökümantasyon ve metin çıktıları tamamlandı.",
            "Harika! Ses kayıtlarının metne dökülmesi ve konuşmacıların ayrılması süreci çok verimli oldu.",
            "Kesinlikle katılıyorum. PDF çıktısı alıp toplantı tutanağını hemen paylaşabiliriz.",
            "Görüşme ve konferans kayıtlarını arşivimize aldık, Drive yedeklemesini de kontrol edeceğim.",
            "Tüm maddeler netleştiğine göre bir sonraki aşamaya geçebiliriz. Herkese iyi çalışmalar."
        )

        val segmentCount = (durationMs / 3500L).toInt().coerceIn(3, 12)
        val segmentDuration = (durationMs / segmentCount).coerceAtLeast(1500L)

        val segments = mutableListOf<TranscriptSegmentEntity>()
        val fullTextBuilder = StringBuilder()

        for (i in 0 until segmentCount) {
            val speakerIdx = i % speakerPool.size
            val speaker = speakerPool[speakerIdx]
            val textSample = speechLibrary[i % speechLibrary.size]

            val start = i * segmentDuration
            val end = (start + segmentDuration).coerceAtMost(durationMs)

            val segment = TranscriptSegmentEntity(
                recordingId = recordingId,
                speakerLabel = speaker,
                speakerIndex = speakerIdx + 1,
                startMs = start,
                endMs = end,
                text = textSample,
                confidence = 0.94f + (i % 5) * 0.01f
            )
            segments.add(segment)
            fullTextBuilder.append("[$speaker] ${textSample}\n\n")
        }

        Pair(fullTextBuilder.toString().trim(), segments)
    }
}
