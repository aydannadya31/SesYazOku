package com.example.export

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.data.RecordingEntity
import com.example.data.TranscriptSegmentEntity
import com.example.summary.MeetingSummaryGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ShareManager {

    fun shareQuickCard(
        context: Context,
        recording: RecordingEntity,
        segments: List<TranscriptSegmentEntity>
    ) {
        try {
            val dateStr = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.forLanguageTag("tr")).format(Date(recording.timestamp))
            val durationMin = (recording.durationMs / 1000) / 60
            val durationSec = (recording.durationMs / 1000) % 60
            val durStr = String.format(Locale.getDefault(), "%02d:%02d", durationMin, durationSec)

            val speakers = segments.map { it.speakerLabel.trim() }.filter { it.isNotBlank() }.distinct()
            val speakersStr = if (speakers.isNotEmpty()) speakers.joinToString(", ") else "${recording.speakerCount} Konuşmacı"

            val summaryData = MeetingSummaryGenerator.generateSummary(recording, segments)

            val shareMessage = buildString {
                appendLine("🎙️ *${recording.title}*")
                appendLine("📅 Tarih: $dateStr  •  ⏱️ Süre: $durStr")
                appendLine("👥 Katılımcılar: $speakersStr")
                appendLine("🏷️ Kategori: ${recording.category} (${recording.projectName})")
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine("🎯 *HIZLI TOPLANTI ÖZETİ:*")
                appendLine(summaryData.overview)
                if (summaryData.keyDecisions.isNotEmpty()) {
                    appendLine("\n📌 *Alınan Kararlar:*")
                    summaryData.keyDecisions.take(3).forEach { appendLine("• $it") }
                }
                if (summaryData.actionItems.isNotEmpty()) {
                    appendLine("\n✅ *Aksiyon Listesi:*")
                    summaryData.actionItems.take(3).forEach { appendLine("• $it") }
                }
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine("💬 *Öne Çıkan Konuşmalar:*")
                segments.take(4).forEach { seg ->
                    val cleanText = seg.text.take(80) + if (seg.text.length > 80) "..." else ""
                    appendLine("*${seg.speakerLabel}:* \"$cleanText\"")
                }
                if (segments.size > 4) {
                    appendLine("*(... ve ${segments.size - 4} konuşma parçası daha)*")
                }
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine("📲 SesYazOku Android ile hazırlanmıştır.")
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "${recording.title} - Toplantı Özeti")
                putExtra(Intent.EXTRA_TEXT, shareMessage)
            }
            context.startActivity(Intent.createChooser(intent, "Hızlı Paylaşım Kartı (WhatsApp, Mail vb.)"))
        } catch (e: Exception) {
            Toast.makeText(context, "Paylaşım başlatılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
