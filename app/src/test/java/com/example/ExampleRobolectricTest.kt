package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.SmartSummarizer
import com.example.data.model.TranscriptSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SesYazıcı AI", appName)
  }

  @Test
  fun `test smart summarizer logic`() {
    val sampleSegments = listOf(
        TranscriptSegment(
            noteId = 1,
            speakerId = "speaker_1",
            speakerName = "Konuşmacı 1",
            text = "Merhaba, bugün toplantıda yeni özellikleri kararlaştırdık.",
            startTimeMs = 0,
            endTimeMs = 5000
        ),
        TranscriptSegment(
            noteId = 1,
            speakerId = "speaker_2",
            speakerName = "Konuşmacı 2",
            text = "Yarın raporu tamamla ve acil olarak ekibe gönder.",
            startTimeMs = 6000,
            endTimeMs = 12000
        )
    )

    val summary = SmartSummarizer.generateSummary("Test Toplantı", sampleSegments)
    assertTrue(summary.executiveSummary.isNotEmpty())
    assertTrue(summary.speakerInsights.containsKey("Konuşmacı 1"))
    assertTrue(summary.speakerInsights.containsKey("Konuşmacı 2"))
  }

  @Test
  fun `test export to txt and segments diarization`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val note = com.example.data.model.TranscriptionNote(
        id = 10,
        title = "Görüşme Notu",
        summary = "Test toplantı özeti"
    )
    val segments = listOf(
        TranscriptSegment(
            noteId = 10,
            speakerId = "speaker_1",
            speakerName = "Ahmet",
            speakerColorHex = "#6366F1",
            startTimeMs = 0,
            endTimeMs = 3000,
            text = "Merhaba nasılsınız?"
        ),
        TranscriptSegment(
            noteId = 10,
            speakerId = "speaker_2",
            speakerName = "Mehmet",
            speakerColorHex = "#06B6D4",
            startTimeMs = 4000,
            endTimeMs = 8000,
            text = "İyiyim teşekkürler, projeyi konuşalım."
        )
    )

    val txtFile = com.example.export.DocumentExportManager.exportToTxt(context, note, segments)
    assertTrue(txtFile != null && txtFile.exists() && txtFile.length() > 0)
    val content = txtFile!!.readText()
    assertTrue(content.contains("Ahmet"))
    assertTrue(content.contains("Mehmet"))
    assertTrue(content.contains("Merhaba nasılsınız?"))
  }
}
