package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.RecordingEntity
import com.example.data.TranscriptSegmentEntity
import com.example.summary.MeetingSummaryGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PdfTemplate(val label: String, val desc: String) {
    STANDARD("Standart Transkript", "Renkli konuşmacı ayrımı ve zaman damgaları"),
    OFFICIAL_MINUTES("Resmi Toplantı Tutanağı", "Katılımcı yoklaması, kararlar ve resmi imza çizgileri"),
    LECTURE_NOTES("Ders & Seminer Notu", "Akademik özet, anahtar kavramlar ve not alma alanı"),
    INTERVIEW("Röportaj & Mülakat", "Soru-Cevap stili ve görüşmeci profil düzeni")
}

object PdfManager {

    suspend fun generatePdf(
        context: Context,
        recording: RecordingEntity,
        segments: List<TranscriptSegmentEntity>,
        template: PdfTemplate = PdfTemplate.STANDARD
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

            val dateStr = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.forLanguageTag("tr")).format(Date(recording.timestamp))
            val durationMin = (recording.durationMs / 1000) / 60
            val durationSec = (recording.durationMs / 1000) % 60
            val durStr = String.format(Locale.getDefault(), "%02d:%02d", durationMin, durationSec)

            // Header Background Bar based on template
            val headerColor = when (template) {
                PdfTemplate.STANDARD -> Color.rgb(30, 58, 138) // Deep Blue
                PdfTemplate.OFFICIAL_MINUTES -> Color.rgb(30, 41, 59) // Corporate Slate Navy
                PdfTemplate.LECTURE_NOTES -> Color.rgb(15, 118, 110) // Academic Teal
                PdfTemplate.INTERVIEW -> Color.rgb(109, 40, 217) // Editorial Purple
            }

            val headerTitle = when (template) {
                PdfTemplate.STANDARD -> "Scribe Craft Global - Transkript Raporu"
                PdfTemplate.OFFICIAL_MINUTES -> "RESMİ TOPLANTI VE KARAR TUTANAĞI"
                PdfTemplate.LECTURE_NOTES -> "DERS, SEMİNER VE KONFERANS DÖKÜMÜ"
                PdfTemplate.INTERVIEW -> "RÖPORTAJ VE GÖRÜŞME METNİ"
            }

            paint.color = headerColor
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 95f, paint)

            // Header Title
            paint.color = Color.WHITE
            paint.textSize = 20f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(headerTitle, 36f, 48f, paint)

            paint.textSize = 11f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("Tarih: $dateStr  |  Şablon: ${template.label}", 36f, 74f, paint)

            var yPos = 120f

            // Document Meta Card
            paint.color = Color.rgb(241, 245, 249)
            canvas.drawRoundRect(36f, yPos, (pageWidth - 36).toFloat(), yPos + 64f, 10f, 10f, paint)

            paint.color = Color.rgb(15, 23, 42)
            paint.textSize = 15f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Başlık: ${recording.title}", 50f, yPos + 26f, paint)

            paint.textSize = 11f
            paint.typeface = Typeface.DEFAULT
            paint.color = Color.rgb(71, 85, 105)
            val speakersList = segments.map { it.speakerLabel.trim() }.filter { it.isNotBlank() }.distinct()
            val speakersStr = if (speakersList.isNotEmpty()) speakersList.joinToString(", ") else "${recording.speakerCount} Konuşmacı"
            canvas.drawText("Kayıt Süresi: $durStr  |  Katılımcılar: $speakersStr  |  Kategori: ${recording.category}", 50f, yPos + 50f, paint)

            yPos += 80f

            // Template Specific Section (Executive Summary / Decisions / Goals)
            if (template == PdfTemplate.OFFICIAL_MINUTES || template == PdfTemplate.LECTURE_NOTES) {
                val summaryData = MeetingSummaryGenerator.generateSummary(recording, segments)

                paint.color = if (template == PdfTemplate.OFFICIAL_MINUTES) Color.rgb(238, 242, 255) else Color.rgb(240, 253, 250)
                canvas.drawRoundRect(36f, yPos, (pageWidth - 36).toFloat(), yPos + 75f, 8f, 8f, paint)

                paint.color = if (template == PdfTemplate.OFFICIAL_MINUTES) Color.rgb(49, 46, 129) else Color.rgb(19, 78, 74)
                paint.textSize = 12f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val boxTitle = if (template == PdfTemplate.OFFICIAL_MINUTES) "📋 TOPLANTI ÖZETİ VE KARARLAR" else "💡 AKADEMİK ÖZET VE ANA FİKİRLER"
                canvas.drawText(boxTitle, 48f, yPos + 22f, paint)

                paint.textSize = 10f
                paint.typeface = Typeface.DEFAULT
                paint.color = Color.rgb(30, 41, 59)
                val summarySnippet = summaryData.overview.take(160) + if (summaryData.overview.length > 160) "..." else ""
                canvas.drawText(summarySnippet, 48f, yPos + 42f, paint)

                val decisionSnippet = if (summaryData.keyDecisions.isNotEmpty()) summaryData.keyDecisions.first().take(90) else "Görüşme mutabakatla tamamlanmıştır."
                canvas.drawText("• Karar: $decisionSnippet", 48f, yPos + 60f, paint)

                yPos += 95f
            }

            // Content Segments
            for ((index, segment) in segments.withIndex()) {
                // Page overflow check
                if (yPos > pageHeight - 110) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPos = 50f
                }

                // Speaker badge color
                val badgeColor = when (segment.speakerIndex % 4) {
                    1 -> Color.rgb(37, 99, 235) // Blue
                    2 -> Color.rgb(16, 185, 129) // Emerald
                    3 -> Color.rgb(217, 119, 6) // Amber
                    else -> Color.rgb(124, 58, 237) // Purple
                }

                paint.color = badgeColor
                canvas.drawRoundRect(36f, yPos, 140f, yPos + 20f, 6f, 6f, paint)

                paint.color = Color.WHITE
                paint.textSize = 10f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val labelText = if (template == PdfTemplate.INTERVIEW) {
                    if (segment.speakerIndex % 2 == 1) "Soru (${segment.speakerLabel})" else "Cevap (${segment.speakerLabel})"
                } else {
                    segment.speakerLabel
                }
                canvas.drawText(labelText, 44f, yPos + 14f, paint)

                // Timestamp
                val startSec = segment.startMs / 1000
                val endSec = segment.endMs / 1000
                paint.color = Color.rgb(100, 116, 139)
                paint.textSize = 9.5f
                paint.typeface = Typeface.DEFAULT
                val timeSpan = String.format(Locale.getDefault(), "[%02d:%02d - %02d:%02d]", startSec / 60, startSec % 60, endSec / 60, endSec % 60)
                canvas.drawText(timeSpan, 150f, yPos + 14f, paint)

                yPos += 28f

                // Segment Text lines
                paint.color = Color.rgb(30, 41, 59)
                paint.textSize = 11.5f
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

                yPos += 12f
            }

            // OFFICIAL_MINUTES Signature Blocks on the last page
            if (template == PdfTemplate.OFFICIAL_MINUTES) {
                if (yPos > pageHeight - 130) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPos = 60f
                }

                yPos += 20f
                paint.color = Color.rgb(100, 116, 139)
                paint.strokeWidth = 1f
                canvas.drawLine(36f, yPos, (pageWidth - 36).toFloat(), yPos, paint)

                yPos += 30f
                paint.textSize = 10f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.color = Color.rgb(15, 23, 42)

                // 3 Signature Columns
                val col1X = 60f
                val col2X = 240f
                val col3X = 420f

                canvas.drawText("Toplantı Başkanı", col1X, yPos, paint)
                canvas.drawText("Yazman / Raportör", col2X, yPos, paint)
                canvas.drawText("Katılımcı / Onay", col3X, yPos, paint)

                paint.typeface = Typeface.DEFAULT
                paint.color = Color.rgb(148, 163, 184)
                canvas.drawText("İmza: ________________", col1X, yPos + 35f, paint)
                canvas.drawText("İmza: ________________", col2X, yPos + 35f, paint)
                canvas.drawText("İmza: ________________", col3X, yPos + 35f, paint)
            }

            // Footer
            paint.color = Color.rgb(148, 163, 184)
            paint.textSize = 9f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("Scribe Craft Global Android Profesyonel Raporlama Modülü ile oluşturulmuştur. (Sayfa $pageNumber)", 36f, (pageHeight - 20).toFloat(), paint)

            pdfDocument.finishPage(page)

            val cleanName = recording.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val pdfDir = File(context.filesDir, "pdfs").apply { if (!exists()) mkdirs() }
            val templateSuffix = when (template) {
                PdfTemplate.STANDARD -> "Transkript"
                PdfTemplate.OFFICIAL_MINUTES -> "Tutanak"
                PdfTemplate.LECTURE_NOTES -> "DersNotu"
                PdfTemplate.INTERVIEW -> "Mulakat"
            }
            val pdfFile = File(pdfDir, "${cleanName}_$templateSuffix.pdf")

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
                putExtra(Intent.EXTRA_TEXT, "Scribe Craft Global ile oluşturulmuş transkript PDF dökümanı ekte yer almaktadır.")
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
                putExtra(Intent.EXTRA_TEXT, "$title\n\n$text\n\n-- Scribe Craft Global")
            }
            context.startActivity(Intent.createChooser(shareIntent, "Metni Paylaş (WhatsApp, Mesajlar)"))
        } catch (e: Exception) {
            Log.e("PdfManager", "Failed to share text", e)
            Toast.makeText(context, "Metin paylaşılırken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun backupToDrive(context: Context, recording: RecordingEntity, pdfFile: File?) {
        try {
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
