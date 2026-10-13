package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.TranscriptSegmentEntity
import org.junit.Assert.assertEquals
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
    assertEquals("SesYazOku", appName)
  }

  @Test
  fun `transcript segment entity holds diarization data`() {
    val segment = TranscriptSegmentEntity(
        recordingId = 1,
        speakerLabel = "Kişi 1",
        speakerIndex = 0,
        startMs = 0,
        endMs = 3000,
        text = "Merhaba nasılsınız?"
    )
    assertEquals("Kişi 1", segment.speakerLabel)
    assertEquals(0L, segment.startMs)
    assertEquals(3000L, segment.endMs)
    assertEquals("Merhaba nasılsınız?", segment.text)
  }
}
