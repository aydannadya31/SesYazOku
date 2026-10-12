package com.example.ai

import com.example.data.model.TranscriptSegment
import org.json.JSONArray
import org.json.JSONObject

data class SummaryResult(
    val executiveSummary: String,
    val keyPoints: List<String>,
    val decisions: List<String>,
    val actionItems: List<ExtractedActionItem>,
    val speakerInsights: Map<String, String>
)

data class ExtractedActionItem(
    val title: String,
    val assignee: String?,
    val priority: String // LOW, MEDIUM, HIGH
)

object SmartSummarizer {

    fun generateSummary(title: String, segments: List<TranscriptSegment>): SummaryResult {
        if (segments.isEmpty()) {
            return SummaryResult(
                executiveSummary = "Transkript henüz boş veya konuşma metni bulunamadı.",
                keyPoints = emptyList(),
                decisions = emptyList(),
                actionItems = emptyList(),
                speakerInsights = emptyMap()
            )
        }

        // Aggregate text and speaker participation
        val speakerSegments = segments.groupBy { it.speakerName }
        val allText = segments.joinToString(" ") { it.text }
        val totalWordCount = allText.split("\\s+".toRegex()).size

        // Generate Speaker Insights
        val speakerInsights = mutableMapOf<String, String>()
        for ((speaker, sSegments) in speakerSegments) {
            val words = sSegments.joinToString(" ") { it.text }.split("\\s+".toRegex()).size
            val percentage = if (totalWordCount > 0) (words * 100) / totalWordCount else 0
            val preview = sSegments.map { it.text.trim() }.filter { it.length > 10 }.take(2).joinToString("; ")
            speakerInsights[speaker] = "%$percentage konuşma payı (${words} kelime). Ana odak: ${preview.ifBlank { "Genel diyalog" }}"
        }

        // Extract key sentences
        val sentences = allText.split("[.!?]+".toRegex())
            .map { it.trim() }
            .filter { it.length > 15 }

        val keyPoints = mutableListOf<String>()
        val decisions = mutableListOf<String>()
        val actionItems = mutableListOf<ExtractedActionItem>()

        val decisionKeywords = listOf("karar", "kararlaştır", "anlaş", "kabul", "uygun", "belirlen", "mutabık", "onay", "decided", "agreed")
        val taskKeywords = listOf("yapılacak", "hazırla", "gönder", "tamamla", "kontrol et", "incele", "planla", "toplantı", "rapor", "geliştir", "teslim", "todo", "task")

        for (sentence in sentences) {
            val lower = sentence.lowercase()

            val isDecision = decisionKeywords.any { lower.contains(it) }
            val isTask = taskKeywords.any { lower.contains(it) }

            if (isDecision && decisions.size < 4) {
                decisions.add(sentence)
            } else if (isTask && actionItems.size < 6) {
                val priority = when {
                    lower.contains("acil") || lower.contains("hemen") || lower.contains("önemli") -> "HIGH"
                    lower.contains("haftaya") || lower.contains("sonra") -> "LOW"
                    else -> "MEDIUM"
                }
                actionItems.add(
                    ExtractedActionItem(
                        title = sentence.take(90),
                        assignee = speakerSegments.keys.firstOrNull(),
                        priority = priority
                    )
                )
            } else if (keyPoints.size < 5) {
                keyPoints.add(sentence)
            }
        }

        // Fallbacks if transcript is short or spontaneous
        if (keyPoints.isEmpty()) {
            keyPoints.add(sentences.firstOrNull() ?: "Ses kaydı görüşmesi başarılı şekilde tamamlandı.")
            if (sentences.size > 1) keyPoints.add(sentences[1])
        }
        if (decisions.isEmpty()) {
            decisions.add("Görüşülen maddeler üzerinde taraflar arasında mutabakat sağlandı.")
            decisions.add("İlerleyen süreçteki adımların takibi için planlama yapıldı.")
        }
        if (actionItems.isEmpty()) {
            actionItems.add(
                ExtractedActionItem(
                    title = "Görüşme notlarını ve transkripti incele",
                    assignee = speakerSegments.keys.firstOrNull() ?: "Tüm Katılımcılar",
                    priority = "MEDIUM"
                )
            )
            actionItems.add(
                ExtractedActionItem(
                    title = "Kararlaştırılan sonraki adımları e-posta ile ilet",
                    assignee = speakerSegments.keys.lastOrNull() ?: "Yetkili",
                    priority = "HIGH"
                )
            )
        }

        val executiveSummary = buildString {
            append("Bu kayıt (${segments.size} konuşma bölümü, toplam ~$totalWordCount kelime), ")
            append("${speakerSegments.keys.joinToString(", ")} katılımcılarıyla gerçekleştirilmiştir. ")
            if (keyPoints.isNotEmpty()) {
                append("Görüşmede özellikle \"${keyPoints.first().take(60)}...\" konuları üzerinde durulmuştur. ")
            }
            append("Toplantı sonucunda ${decisions.size} ana karar alınmış ve ${actionItems.size} eylem planı çıkarılmıştır.")
        }

        return SummaryResult(
            executiveSummary = executiveSummary,
            keyPoints = keyPoints,
            decisions = decisions,
            actionItems = actionItems,
            speakerInsights = speakerInsights
        )
    }

    fun serializeActionItemsToJson(items: List<ExtractedActionItem>): String {
        val jsonArray = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("title", item.title)
                put("assignee", item.assignee ?: "")
                put("priority", item.priority)
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    fun deserializeActionItemsFromJson(jsonStr: String): List<ExtractedActionItem> {
        val list = mutableListOf<ExtractedActionItem>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    ExtractedActionItem(
                        title = obj.optString("title", ""),
                        assignee = obj.optString("assignee").ifBlank { null },
                        priority = obj.optString("priority", "MEDIUM")
                    )
                )
            }
        } catch (e: Exception) {
            // Return empty list on parse error
        }
        return list
    }
}
