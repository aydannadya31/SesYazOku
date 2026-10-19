package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.RecordingEntity
import com.example.data.TranscriptSegmentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfManager {

    suspend fun generatePdf(
        context: Context,
        recording: RecordingEntity,
        segments: List<TranscriptSegmentEntity>
    ): File? = withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // Standard A4 width in points
            val pageHeight = 842 // Standard A4 height in points
            var pageNumber = 1

            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val paint = Paint().apply {
                isAntiAlias = true
            }

            // Header Background Bar
            paint.color = Color.rgb(30, 58, 138) // Deep Blue
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 90f, paint)

            // Header Title
            paint.color = Color.WHITE
            paint.textSize = 22f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("SesYazOku - Transkript Raporu", 36f, 48f, paint)

            paint.textSize = 12f
            paint.typeface = Typeface.DEFAULT
            val dateStr = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.forLanguageTag("tr")).format(Date(recording.timestamp))
            canvas.drawText("Tarih: $dateStr", 36f, 72f, paint)

            var yPos = 120f

            // Document Meta Card
            paint.color = Color.rgb(241, 245, 249)
            canvas.drawRoundRect(36f, yPos, (pageWidth - 36).toFloat(), yPos + 60f, 12f, 12f, paint)

            paint.color = Color.rgb(15, 23, 42)
            paint.textSize = 16f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Başlık: ${recording.title}", 50f, yPos + 26f, paint)

            paint.textSize = 11f
            paint.typeface = Typeface.DEFAULT
            paint.color = Color.rgb(71, 85, 105)
            val durationMin = (recording.durationMs / 1000) / 60
            val durationSec = (recording.durationMs / 1000) % 60
            val durStr = String.format(Locale.getDefault(), "%02d:%02d", durationMin, durationSec)
            canvas.drawText("Kayıt Süresi: $durStr | Konuşmacı Sayısı: ${recording.speakerCount} Kişi", 50f, yPos + 48f, paint)

            yPos += 85f

            // Content Segments
            for (segment in segments) {
                // If nearing bottom of page, start a new page
                if (yPos > pageHeight - 100) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPos = 50f
                }

                // Speaker badge color
                val badgeColor = when (segment.speakerIndex % 3) {
                    1 -> Color.rgb(37, 99, 235) // Blue
                    2 -> Color.rgb(16, 185, 129) // Emerald
                    else -> Color.rgb(217, 119, 6) // Amber
                }

                paint.color = badgeColor
                canvas.drawRoundRect(36f, yPos, 140f, yPos + 22f, 8f, 8f, paint)

                paint.color = Color.WHITE
                paint.textSize = 11f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(segment.speakerLabel, 46f, yPos + 15f, paint)

                // Timestamp
                val startSec = segment.startMs / 1000
                val endSec = segment.endMs / 1000
                paint.color = Color.rgb(100, 116, 139)
                paint.textSize = 10f
                paint.typeface = Typeface.DEFAULT
                val timeSpan = String.format(Locale.getDefault(), "[%02d:%02d - %02d:%02d]", startSec / 60, startSec % 60, endSec / 60, endSec % 60)
                canvas.drawText(timeSpan, 150f, yPos + 15f, paint)

                yPos += 32f

                // Segment Text lines
                paint.color = Color.rgb(30, 41, 59)
                paint.textSize = 12f
                paint.typeface = Typeface.DEFAULT

                val maxTextWidth = (pageWidth - 72).toFloat()
                val words = segment.text.split(" ")
                var currentLine = ""

                for (word in words) {
                    val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                    if (paint.measureText(testLine) > maxTextWidth) {
                        canvas.drawText(currentLine, 36f, yPos, paint)
                        yPos += 16f
                        currentLine = word
                    } else {
                        currentLine = testLine
                    }
                }
                if (currentLine.isNotEmpty()) {
                    canvas.drawText(currentLine, 36f, yPos, paint)
                    yPos += 16f
                }

                yPos += 14f // Space before next segment
            }

            // Footer
            paint.color = Color.rgb(148, 163, 184)
            paint.textSize = 9f
            canvas.drawText("SesYazOku Mobil Uygulaması tarafından üretilmiştir.", 36f, (pageHeight - 24).toFloat(), paint)

            pdfDocument.finishPage(page)

            val cleanName = recording.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val pdfDir = File(context.filesDir, "pdfs").apply { if (!exists()) mkdirs() }
            val pdfFile = File(pdfDir, "${cleanName}_Transkript.pdf")

            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            pdfFile
        } catch (e: Exception) {
            Log.e("PdfManager", "Failed to generate PDF", e)
            null
        }
    }

    fun sharePdf(context: Context, pdfFile: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, pdfFile.nameWithoutExtension)
                putExtra(Intent.EXTRA_TEXT, "SesYazOku ile oluşturulmuş transkript PDF dökümanı ekte yer almaktadır.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "PDF'i Paylaş (WhatsApp, E-posta vs.)"))
        } catch (e: Exception) {
            Log.e("PdfManager", "Failed to share PDF", e)
            Toast.makeText(context, "PDF paylaşılırken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareFullText(context: Context, title: String, text: String) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "$title\n\n$text\n\n-- SesYazOku")
            }
            context.startActivity(Intent.createChooser(shareIntent, "Metni Paylaş (WhatsApp, Mesajlar)"))
        } catch (e: Exception) {
            Log.e("PdfManager", "Failed to share text", e)
            Toast.makeText(context, "Metin paylaşılırken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun backupToDrive(context: Context, recording: RecordingEntity, pdfFile: File?) {
        try {
            // Initiate standard Android share/upload intent specifically targeting Drive or cloud storage
            val uri: Uri? = pdfFile?.let {
                FileProvider.getUriForFile(context, "${context.packageName}.provider", it)
            }

            val driveIntent = Intent(Intent.ACTION_SEND).apply {
                type = if (uri != null) "application/pdf" else "text/plain"
                if (uri != null) {
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    putExtra(Intent.EXTRA_TEXT, recording.transcriptFullText)
                }
                putExtra(Intent.EXTRA_TITLE, "${recording.title} - Drive Yedek")
                `package` = "com.google.android.apps.docs"
            }

            try {
                context.startActivity(driveIntent)
            } catch (_: Exception) {
                // If Drive app not installed directly, open general chooser
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = if (uri != null) "application/pdf" else "text/plain"
                    if (uri != null) {
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    putExtra(Intent.EXTRA_SUBJECT, "${recording.title} - Bulut / Drive Yedeği")
                }
                context.startActivity(Intent.createChooser(fallbackIntent, "Google Drive'a Yedekle"))
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Drive yedekleme işlemi başlatılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
