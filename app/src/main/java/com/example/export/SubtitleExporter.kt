package com.example.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.RecordingEntity
import com.example.data.TranscriptSegmentEntity
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object SubtitleExporter {

    fun exportAsSrt(
        context: Context,
        recording: RecordingEntity,
        segments: List<TranscriptSegmentEntity>
    ): File? {
        try {
            val srtContent = buildString {
                segments.forEachIndexed { index, seg ->
                    appendLine(index + 1)
                    val startStr = formatSrtTimestamp(seg.startMs)
                    val endStr = formatSrtTimestamp(seg.endMs.coerceAtLeast(seg.startMs + 1000L))
                    appendLine("$startStr --> $endStr")
                    appendLine("[${seg.speakerLabel}]: ${seg.text.trim()}")
                    appendLine()
                }
            }

            val cleanName = recording.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val subDir = File(context.filesDir, "subtitles").apply { if (!exists()) mkdirs() }
            val srtFile = File(subDir, "${cleanName}.srt")

            FileOutputStream(srtFile).use { out ->
                out.write(srtContent.toByteArray(Charsets.UTF_8))
            }
            return srtFile
        } catch (e: Exception) {
            Log.e("SubtitleExporter", "Failed to export SRT", e)
            return null
        }
    }

    fun exportAsVtt(
        context: Context,
        recording: RecordingEntity,
        segments: List<TranscriptSegmentEntity>
    ): File? {
        try {
            val vttContent = buildString {
                appendLine("WEBVTT")
                appendLine("Kind: captions")
                appendLine("Language: tr")
                appendLine()

                segments.forEachIndexed { index, seg ->
                    appendLine(index + 1)
                    val startStr = formatVttTimestamp(seg.startMs)
                    val endStr = formatVttTimestamp(seg.endMs.coerceAtLeast(seg.startMs + 1000L))
                    appendLine("$startStr --> $endStr")
                    appendLine("<v ${seg.speakerLabel}>${seg.text.trim()}")
                    appendLine()
                }
            }

            val cleanName = recording.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val subDir = File(context.filesDir, "subtitles").apply { if (!exists()) mkdirs() }
            val vttFile = File(subDir, "${cleanName}.vtt")

            FileOutputStream(vttFile).use { out ->
                out.write(vttContent.toByteArray(Charsets.UTF_8))
            }
            return vttFile
        } catch (e: Exception) {
            Log.e("SubtitleExporter", "Failed to export VTT", e)
            return null
        }
    }

    fun shareSubtitleFile(context: Context, file: File, formatName: String = "Altyazı") {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                putExtra(Intent.EXTRA_TEXT, "SesYazOku ile oluşturulmuş $formatName dosyası ekte yer almaktadır.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "$formatName Paylaş"))
        } catch (e: Exception) {
            Log.e("SubtitleExporter", "Failed to share subtitle", e)
            Toast.makeText(context, "$formatName paylaşılırken hata: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatSrtTimestamp(ms: Long): String {
        val totalSec = ms / 1000
        val millis = ms % 1000
        val hours = totalSec / 3600
        val mins = (totalSec % 3600) / 60
        val secs = totalSec % 60
        return String.format(Locale.US, "%02d:%02d:%02d,%03d", hours, mins, secs, millis)
    }

    private fun formatVttTimestamp(ms: Long): String {
        val totalSec = ms / 1000
        val millis = ms % 1000
        val hours = totalSec / 3600
        val mins = (totalSec % 3600) / 60
        val secs = totalSec % 60
        return String.format(Locale.US, "%02d:%02d:%02d.%03d", hours, mins, secs, millis)
    }
}
