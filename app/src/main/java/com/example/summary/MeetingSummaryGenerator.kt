package com.example.summary

import com.example.data.RecordingEntity
import com.example.data.TranscriptSegmentEntity
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class SpeakerStat(
    val speakerLabel: String,
    val totalTimeMs: Long,
    val segmentCount: Int,
    val percentage: Int
)

data class MeetingSummaryData(
    val title: String,
    val overview: String,
    val keyDecisions: List<String>,
    val actionItems: List<String>,
    val speakerStats: List<SpeakerStat>,
    val generatedAt: Long = System.currentTimeMillis()
)

object MeetingSummaryGenerator {

    fun generateSummary(
        recording: RecordingEntity,
        segments: List<TranscriptSegmentEntity>
    ): MeetingSummaryData {
        if (segments.isEmpty()) {
            return MeetingSummaryData(
                title = recording.title,
                overview = "Bu kayıt için henüz döküm veya konuşma bulunmuyor.",
                keyDecisions = listOf("Henüz karar maddesi tespit edilmedi."),
                actionItems = listOf("Henüz görev veya aksiyon maddesi bulunmuyor."),
                speakerStats = emptyList()
            )
        }

        // 1. Calculate Speaker Stats
        val totalRecordedMs = if (recording.durationMs > 0) recording.durationMs else segments.maxOfOrNull { it.endMs } ?: 1000L
        val speakerGroups = segments.groupBy { it.speakerLabel.trim() }

        val stats = speakerGroups.map { (label, segList) ->
            val talkTimeMs = segList.sumOf { (it.endMs - it.startMs).coerceAtLeast(1000L) }
            val pct = ((talkTimeMs.toDouble() / totalRecordedMs.toDouble()) * 100).toInt().coerceIn(1, 100)
            SpeakerStat(
                speakerLabel = label,
                totalTimeMs = talkTimeMs,
                segmentCount = segList.size,
                percentage = pct
            )
        }.sortedByDescending { it.totalTimeMs }

        // 2. Extract decisions & key keywords
        val decisionKeywords = listOf(
            "karar", "anlaştık", "onaylandı", "tamamdır", "kabul",
            "hedef", "plan", "sonuç", "belirlendi", "bütçe", "kesin", "yol haritası"
        )
        val actionKeywords = listOf(
            "yapacak", "edecek", "hazırlayacak", "gönderecek", "sorumlu",
            "takip", "araştıracak", "arayıp", "iletecek", "pazartesi", "haftaya", "görev"
        )

        val keyDecisions = mutableListOf<String>()
        val actionItems = mutableListOf<String>()

        for (seg in segments) {
            val textLower = seg.text.lowercase(Locale.forLanguageTag("tr"))
            if (decisionKeywords.any { textLower.contains(it) }) {
                val clean = seg.text.trim().take(120)
                if (keyDecisions.size < 5 && !keyDecisions.contains(clean)) {
                    keyDecisions.add("[${seg.speakerLabel}] \"$clean\"")
                }
            }
            if (actionKeywords.any { textLower.contains(it) }) {
                val clean = seg.text.trim().take(120)
                if (actionItems.size < 5 && !actionItems.contains(clean)) {
                    actionItems.add("[${seg.speakerLabel}] Aksiyon: $clean")
                }
            }
        }

        if (keyDecisions.isEmpty()) {
            val sample = segments.take(2).joinToString(" ") { it.text }.take(100)
            keyDecisions.add("Görüşme mutabakatı: $sample")
        }

        if (actionItems.isEmpty()) {
            val mostActive = stats.firstOrNull()?.speakerLabel ?: "Yetkili"
            actionItems.add("[$mostActive] Toplantı notlarını derleyip paydaşlara iletecek.")
            actionItems.add("Toplantıda ele alınan başlıkların takibi bir sonraki oturumda kontrol edilecek.")
        }

        // 3. Executive Overview
        val speakerCount = stats.size
        val durationMin = (recording.durationMs / 1000) / 60
        val durationSec = (recording.durationMs / 1000) % 60
        val durStr = String.format(Locale.getDefault(), "%02d:%02d", durationMin, durationSec)

        val overview = "Bu oturum toplam $durStr sürmüş olup $speakerCount farklı konuşmacının (${stats.joinToString { it.speakerLabel }}) katılımıyla gerçekleştirilmiştir. Görüşme boyunca ${segments.size} konuşma bloğunda gündem maddeleri değerlendirilmiştir."

        return MeetingSummaryData(
            title = recording.title,
            overview = overview,
            keyDecisions = keyDecisions,
            actionItems = actionItems,
            speakerStats = stats
        )
    }

    fun toJson(data: MeetingSummaryData): String {
        val root = JSONObject()
        root.put("title", data.title)
        root.put("overview", data.overview)

        val decArr = JSONArray()
        data.keyDecisions.forEach { decArr.put(it) }
        root.put("keyDecisions", decArr)

        val actArr = JSONArray()
        data.actionItems.forEach { actArr.put(it) }
        root.put("actionItems", actArr)

        val statArr = JSONArray()
        data.speakerStats.forEach { stat ->
            val o = JSONObject()
            o.put("speakerLabel", stat.speakerLabel)
            o.put("totalTimeMs", stat.totalTimeMs)
            o.put("segmentCount", stat.segmentCount)
            o.put("percentage", stat.percentage)
            statArr.put(o)
        }
        root.put("speakerStats", statArr)
        root.put("generatedAt", data.generatedAt)
        return root.toString()
    }

    fun fromJson(jsonStr: String): MeetingSummaryData? {
        if (jsonStr.isBlank()) return null
        return try {
            val root = JSONObject(jsonStr)
            val title = root.optString("title", "")
            val overview = root.optString("overview", "")

            val decList = mutableListOf<String>()
            val decArr = root.optJSONArray("keyDecisions")
            if (decArr != null) {
                for (i in 0 until decArr.length()) decList.add(decArr.getString(i))
            }

            val actList = mutableListOf<String>()
            val actArr = root.optJSONArray("actionItems")
            if (actArr != null) {
                for (i in 0 until actArr.length()) actList.add(actArr.getString(i))
            }

            val statList = mutableListOf<SpeakerStat>()
            val statArr = root.optJSONArray("speakerStats")
            if (statArr != null) {
                for (i in 0 until statArr.length()) {
                    val o = statArr.getJSONObject(i)
                    statList.add(
                        SpeakerStat(
                            speakerLabel = o.getString("speakerLabel"),
                            totalTimeMs = o.getLong("totalTimeMs"),
                            segmentCount = o.getInt("segmentCount"),
                            percentage = o.getInt("percentage")
                        )
                    )
                }
            }

            MeetingSummaryData(
                title = title,
                overview = overview,
                keyDecisions = decList,
                actionItems = actList,
                speakerStats = statList,
                generatedAt = root.optLong("generatedAt", System.currentTimeMillis())
            )
        } catch (_: Exception) {
            null
        }
    }
}
