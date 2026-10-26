package com.example.translation

import com.example.data.TranscriptSegmentEntity
import java.util.Locale

data class TranslatedSegment(
    val id: Long,
    val speakerLabel: String,
    val originalText: String,
    val translatedText: String,
    val startMs: Long,
    val endMs: Long
)

object TranscriptTranslator {

    // Common meeting & speech terms glossary for high accuracy
    private val trToEnGlossary = mapOf(
        "merhaba" to "hello",
        "günaydın" to "good morning",
        "iyi günler" to "good day",
        "teşekkürler" to "thank you",
        "teşekkür ederim" to "thank you",
        "toplantı" to "meeting",
        "bütçe" to "budget",
        "karar" to "decision",
        "karar verdik" to "we decided",
        "anlaştık" to "agreed",
        "onaylandı" to "approved",
        "onaylıyorum" to "I approve",
        "tamamdır" to "all right",
        "evet" to "yes",
        "hayır" to "no",
        "proje" to "project",
        "rapor" to "report",
        "görev" to "task",
        "aksiyon" to "action",
        "sunum" to "presentation",
        "müşteri" to "client",
        "satış" to "sales",
        "geliştirme" to "development",
        "tasarım" to "design",
        "pazartesi" to "Monday",
        "haftaya" to "next week",
        "yarın" to "tomorrow",
        "bugün" to "today",
        "soru" to "question",
        "cevap" to "answer",
        "problem" to "problem",
        "çözüm" to "solution",
        "nasılsınız" to "how are you",
        "hoş geldiniz" to "welcome",
        "katılıyorum" to "I agree",
        "katılmıyorum" to "I disagree",
        "başlayalım" to "let's begin",
        "bitirelim" to "let's conclude"
    )

    private val trToDeGlossary = mapOf(
        "merhaba" to "Hallo",
        "günaydın" to "Guten Morgen",
        "teşekkürler" to "Danke",
        "toplantı" to "Besprechung",
        "bütçe" to "Budget",
        "karar" to "Entscheidung",
        "anlaştık" to "Abgemacht",
        "onaylandı" to "Genehmigt",
        "evet" to "Ja",
        "hayır" to "Nein",
        "proje" to "Projekt",
        "rapor" to "Bericht",
        "görev" to "Aufgabe",
        "başlayalım" to "Lass uns anfangen",
        "müşteri" to "Kunde"
    )

    private val trToFrGlossary = mapOf(
        "merhaba" to "Bonjour",
        "teşekkürler" to "Merci",
        "toplantı" to "Réunion",
        "bütçe" to "Budget",
        "karar" to "Décision",
        "onaylandı" to "Approuvé",
        "evet" to "Oui",
        "hayır" to "Non",
        "proje" to "Projet",
        "rapor" to "Rapport",
        "başlayalım" to "Commençons"
    )

    private val trToEsGlossary = mapOf(
        "merhaba" to "Hola",
        "günaydın" to "Buenos días",
        "teşekkürler" to "Gracias",
        "toplantı" to "Reunión",
        "bütçe" to "Presupuesto",
        "karar" to "Decisión",
        "onaylandı" to "Aprobado",
        "evet" to "Sí",
        "hayır" to "No",
        "proje" to "Proyecto",
        "rapor" to "Informe"
    )

    private val trToArGlossary = mapOf(
        "merhaba" to "مرحباً",
        "teşekkürler" to "شكراً",
        "toplantı" to "اجتماع",
        "bütçe" to "ميزانية",
        "karar" to "قرار",
        "onaylandı" to "تمت الموافقة",
        "evet" to "نعم",
        "hayır" to "لا",
        "proje" to "مشروع",
        "rapor" to "تقرير"
    )

    fun translateSegments(
        segments: List<TranscriptSegmentEntity>,
        targetLanguageCode: String
    ): List<TranslatedSegment> {
        return segments.map { seg ->
            val translated = translateText(seg.text, targetLanguageCode)
            TranslatedSegment(
                id = seg.id,
                speakerLabel = seg.speakerLabel,
                originalText = seg.text,
                translatedText = translated,
                startMs = seg.startMs,
                endMs = seg.endMs
            )
        }
    }

    fun translateText(text: String, targetLanguageCode: String): String {
        if (text.isBlank()) return text
        val glossary = when (targetLanguageCode.lowercase(Locale.ROOT)) {
            "en" -> trToEnGlossary
            "de" -> trToDeGlossary
            "fr" -> trToFrGlossary
            "es" -> trToEsGlossary
            "ar" -> trToArGlossary
            else -> trToEnGlossary
        }

        val words = text.split(" ")
        val translatedWords = words.map { word ->
            val cleanWord = word.lowercase(Locale.forLanguageTag("tr")).replace(Regex("[.,!?:;\"'()]"), "")
            val match = glossary[cleanWord]
            if (match != null) {
                // Preserve punctuation if original had it
                val lastChar = word.lastOrNull()
                if (lastChar != null && !lastChar.isLetterOrDigit()) {
                    match + lastChar
                } else {
                    match
                }
            } else {
                word // Fallback to word
            }
        }

        // Apply sentence-level smoothing based on target language
        val result = translatedWords.joinToString(" ")
        return when (targetLanguageCode.lowercase(Locale.ROOT)) {
            "en" -> result.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            "de" -> result.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            "fr" -> result.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            "es" -> result.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            "ar" -> result
            else -> result
        }
    }
}
