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
    assertEquals("SesYazıcı", appName)
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
}
