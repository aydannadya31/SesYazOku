package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.TranscriptionNote
import com.example.data.model.TranscriptSegment
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DocumentExportManager {

    fun exportToPdf(
        context: Context,
        note: TranscriptionNote,
        segments: List<TranscriptSegment>
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 points
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val paintTitle = Paint().apply {
            color = Color.rgb(30, 27, 75)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintHeaderMeta = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 10f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val paintSpeaker = Paint().apply {
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintText = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 10f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val paintDivider = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }

        var y = 50f
        val marginX = 40f
        val contentWidth = 515f
        val pageHeight = 800f
        var pageNumber = 1

        fun checkNewPage(neededHeight: Float) {
            if (y + neededHeight > pageHeight) {
                pdfDocument.finishPage(page)
                pageNumber++
                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(newPageInfo)
                canvas = page.canvas
                y = 50f
            }
        }

        // Title
        canvas.drawText(note.title, marginX, y, paintTitle)
        y += 20f

        // Metadata
        val dateFormat = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("tr", "TR"))
        val dateStr = dateFormat.format(Date(note.createdAt))
        val durationMin = (note.durationMs / 1000) / 60
        val durationSec = (note.durationMs / 1000) % 60
        val metaText = "Tarih: $dateStr  |  Süre: ${String.format("%02d:%02d", durationMin, durationSec)}  |  Bölüm Sayısı: ${segments.size}"
        canvas.drawText(metaText, marginX, y, paintHeaderMeta)
        y += 15f

        // Divider
        canvas.drawLine(marginX, y, marginX + contentWidth, y, paintDivider)
        y += 20f

        // Summary section if exists
        if (!note.summary.isNullOrBlank()) {
            checkNewPage(80f)
            val summaryTitlePaint = Paint().apply {
                color = Color.rgb(67, 56, 202)
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("Yapay Zeka Özeti", marginX, y, summaryTitlePaint)
            y += 16f

            val words = note.summary.split(" ")
            var line = StringBuilder()
            for (word in words) {
                if (paintText.measureText(line.toString() + " " + word) > contentWidth) {
                    checkNewPage(14f)
                    canvas.drawText(line.toString(), marginX, y, paintText)
                    y += 14f
                    line = StringBuilder(word)
                } else {
                    if (line.isNotEmpty()) line.append(" ")
                    line.append(word)
                }
            }
            if (line.isNotEmpty()) {
                checkNewPage(14f)
                canvas.drawText(line.toString(), marginX, y, paintText)
                y += 18f
            }

            canvas.drawLine(marginX, y, marginX + contentWidth, y, paintDivider)
            y += 20f
        }

        // Transcript Header
        checkNewPage(30f)
        val transcriptTitlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("Transkripsiyon Kaydı", marginX, y, transcriptTitlePaint)
        y += 20f

        // Segments
        for (segment in segments) {
            checkNewPage(50f)
            val startMin = (segment.startTimeMs / 1000) / 60
            val startSec = (segment.startTimeMs / 1000) % 60
            val timeStr = String.format("[%02d:%02d]", startMin, startSec)

            try {
                paintSpeaker.color = Color.parseColor(segment.speakerColorHex)
            } catch (e: Exception) {
                paintSpeaker.color = Color.rgb(79, 70, 229)
            }

            val speakerHeader = "${segment.speakerName} $timeStr"
            canvas.drawText(speakerHeader, marginX, y, paintSpeaker)
            y += 15f

            val words = segment.text.split(" ")
            var line = StringBuilder()
            for (word in words) {
                if (paintText.measureText(line.toString() + " " + word) > contentWidth) {
                    checkNewPage(14f)
                    canvas.drawText(line.toString(), marginX, y, paintText)
                    y += 14f
                    line = StringBuilder(word)
                } else {
                    if (line.isNotEmpty()) line.append(" ")
                    line.append(word)
                }
            }
            if (line.isNotEmpty()) {
                checkNewPage(14f)
                canvas.drawText(line.toString(), marginX, y, paintText)
                y += 18f
            }
        }

        pdfDocument.finishPage(page)

        val outputDir = File(context.cacheProviderDir(), "exports")
        if (!outputDir.exists()) outputDir.mkdirs()
        val safeTitle = note.title.replace("[^a-zA-Z0-9_-]".toRegex(), "_").take(30)
        val file = File(outputDir, "${safeTitle}_${note.id}.pdf")

        return try {
            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.close()
            file
        } catch (e: Exception) {
            Log.e("ExportManager", "PDF generation failed", e)
            pdfDocument.close()
            null
        }
    }

    fun exportToDocx(
        context: Context,
        note: TranscriptionNote,
        segments: List<TranscriptSegment>
    ): File? {
        val outputDir = File(context.cacheProviderDir(), "exports")
        if (!outputDir.exists()) outputDir.mkdirs()
        val safeTitle = note.title.replace("[^a-zA-Z0-9_-]".toRegex(), "_").take(30)
        val file = File(outputDir, "${safeTitle}_${note.id}.docx")

        return try {
            val zos = ZipOutputStream(FileOutputStream(file))

            // 1. [Content_Types].xml
            val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>""".trimIndent()
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            zos.write(contentTypes.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 2. _rels/.rels
            val packageRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""".trimIndent()
            zos.putNextEntry(ZipEntry("_rels/.rels"))
            zos.write(packageRels.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 3. word/document.xml
            val dateFormat = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("tr", "TR"))
            val dateStr = dateFormat.format(Date(note.createdAt))

            val sb = StringBuilder()
            sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            sb.append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">""")
            sb.append("""<w:body>""")

            // Document Title
            sb.append("""<w:p><w:pPr><w:jc w:val="center"/><w:pBdr><w:bottom w:val="single" w:sz="12" w:space="4" w:color="4F46E5"/></w:pBdr></w:pPr><w:r><w:rPr><w:b/><w:sz w:val="48"/><w:color w:val="1E1B4B"/></w:rPr><w:t>${escapeXml(note.title)}</w:t></w:r></w:p>""")
            sb.append("""<w:p><w:r><w:rPr><w:i/><w:sz w:val="20"/><w:color w:val="64748B"/></w:rPr><w:t>Tarih: ${escapeXml(dateStr)} | Kayıt Süresi: ${note.durationMs / 1000} sn</w:t></w:r></w:p>""")

            // Summary
            if (!note.summary.isNullOrBlank()) {
                sb.append("""<w:p><w:pPr><w:spacing w:before="240" w:after="120"/></w:pPr><w:r><w:rPr><w:b/><w:sz w:val="28"/><w:color w:val="4338CA"/></w:rPr><w:t>Yapay Zeka Özeti</w:t></w:r></w:p>""")
                sb.append("""<w:p><w:r><w:rPr><w:sz w:val="22"/><w:color w:val="1E293B"/></w:rPr><w:t>${escapeXml(note.summary)}</w:t></w:r></w:p>""")
            }

            // Transcript Header
            sb.append("""<w:p><w:pPr><w:spacing w:before="360" w:after="120"/></w:pPr><w:r><w:rPr><w:b/><w:sz w:val="28"/><w:color w:val="0F172A"/></w:rPr><w:t>Transkripsiyon Metni</w:t></w:r></w:p>""")

            // Transcript Segments
            for (segment in segments) {
                val min = (segment.startTimeMs / 1000) / 60
                val sec = (segment.startTimeMs / 1000) % 60
                val timeFormatted = String.format("%02d:%02d", min, sec)

                sb.append("""<w:p><w:pPr><w:spacing w:before="140"/></w:pPr><w:r><w:rPr><w:b/><w:sz w:val="22"/><w:color w:val="4F46E5"/></w:rPr><w:t>${escapeXml(segment.speakerName)} [${timeFormatted}]:</w:t></w:r></w:p>""")
                sb.append("""<w:p><w:r><w:rPr><w:sz w:val="22"/><w:color w:val="334155"/></w:rPr><w:t>${escapeXml(segment.text)}</w:t></w:r></w:p>""")
            }

            sb.append("""<w:sectPr/></w:body></w:document>""")

            zos.putNextEntry(ZipEntry("word/document.xml"))
            zos.write(sb.toString().toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.close()
            file
        } catch (e: Exception) {
            Log.e("ExportManager", "DOCX generation failed", e)
            null
        }
    }

    fun exportToTxt(
        context: Context,
        note: TranscriptionNote,
        segments: List<TranscriptSegment>
    ): File? {
        val outputDir = File(context.cacheProviderDir(), "exports")
        if (!outputDir.exists()) outputDir.mkdirs()
        val safeTitle = note.title.replace("[^a-zA-Z0-9_-]".toRegex(), "_").take(30)
        val file = File(outputDir, "${safeTitle}_${note.id}.txt")

        return try {
            val dateFormat = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("tr", "TR"))
            val content = buildString {
                appendLine("=========================================")
                appendLine(note.title)
                appendLine("Tarih: ${dateFormat.format(Date(note.createdAt))}")
                appendLine("Süre: ${note.durationMs / 1000} saniye")
                appendLine("=========================================")
                appendLine()
                if (!note.summary.isNullOrBlank()) {
                    appendLine("--- YAPAY ZEKA ÖZETİ ---")
                    appendLine(note.summary)
                    appendLine()
                }
                appendLine("--- TRANSKRİPSİYON ---")
                for (s in segments) {
                    val min = (s.startTimeMs / 1000) / 60
                    val sec = (s.startTimeMs / 1000) % 60
                    appendLine("[${String.format("%02d:%02d", min, sec)}] ${s.speakerName}: ${s.text}")
                    appendLine()
                }
            }
            file.writeText(content)
            file
        } catch (e: Exception) {
            Log.e("ExportManager", "TXT generation failed", e)
            null
        }
    }

    fun exportToSrt(
        context: Context,
        note: TranscriptionNote,
        segments: List<TranscriptSegment>
    ): File? {
        val outputDir = File(context.cacheProviderDir(), "exports")
        if (!outputDir.exists()) outputDir.mkdirs()
        val safeTitle = note.title.replace("[^a-zA-Z0-9_-]".toRegex(), "_").take(30)
        val file = File(outputDir, "${safeTitle}_${note.id}.srt")

        return try {
            val content = buildString {
                segments.forEachIndexed { index, seg ->
                    appendLine(index + 1)
                    appendLine("${formatSrtTime(seg.startTimeMs)} --> ${formatSrtTime(seg.endTimeMs)}")
                    appendLine("${seg.speakerName}: ${seg.text}")
                    appendLine()
                }
            }
            file.writeText(content)
            file
        } catch (e: Exception) {
            Log.e("ExportManager", "SRT generation failed", e)
            null
        }
    }

    private fun formatSrtTime(ms: Long): String {
        val hours = ms / 3600000
        val minutes = (ms % 3600000) / 60000
        val seconds = (ms % 60000) / 1000
        val millis = ms % 1000
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, millis)
    }

    fun shareFile(context: Context, file: File, mimeType: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "PDF'i Paylaş (WhatsApp, E-posta...)")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Generates PDF and opens the native share sheet directly (e.g. for WhatsApp, Telegram, etc.)
     */
    fun sharePdf(context: Context, note: TranscriptionNote, segments: List<TranscriptSegment>): Boolean {
        val pdfFile = exportToPdf(context, note, segments) ?: return false
        shareFile(context, pdfFile, "application/pdf")
        return true
    }

    /**
     * Saves the PDF directly to the device's Downloads directory and registers with MediaScanner
     */
    fun savePdfToDevice(context: Context, note: TranscriptionNote, segments: List<TranscriptSegment>): File? {
        val tempPdf = exportToPdf(context, note, segments) ?: return null
        val safeTitle = note.title.replace("[^a-zA-Z0-9_-]".toRegex(), "_").take(35)
        val fileName = "SesYazici_${safeTitle}_${note.id}.pdf"

        val targetDir = try {
            val publicDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            if (publicDir != null && (publicDir.exists() || publicDir.mkdirs())) {
                publicDir
            } else {
                context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            }
        } catch (e: Exception) {
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        }

        val destinationFile = File(targetDir, fileName)
        return try {
            tempPdf.copyTo(destinationFile, overwrite = true)
            // Trigger MediaScanner so the PDF is immediately visible in Downloads and File Manager
            try {
                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(destinationFile.absolutePath),
                    arrayOf("application/pdf"),
                    null
                )
            } catch (e: Exception) {
                Log.w("ExportManager", "MediaScanner failed", e)
            }
            destinationFile
        } catch (e: Exception) {
            Log.e("ExportManager", "Failed to save PDF to Downloads", e)
            tempPdf
        }
    }

    fun openPdfFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "PDF'i Aç"))
        } catch (e: Exception) {
            Log.e("ExportManager", "Cannot open PDF viewer", e)
        }
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun Context.cacheProviderDir(): File {
        return cacheDir
    }
}
