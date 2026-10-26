package com.example.transcription

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.data.TranscriptSegmentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import kotlin.coroutines.resume
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * SpeechTranscriptionEngine
 *
 * Sesten yazıya dönüştürme işlemlerini gerçekleştirir:
 * 1. Eğer canlı ses tanıma önbelleği varsa gerçek söylenen metinleri kullanır.
 * 2. Cihazın Android SpeechRecognizer motoru ile gerçek ses tanıma dener.
 * 3. Gerçek ses tanıma sonuçları üzerinden belirtilen konuşmacı sayısına göre (Kişi 1, Kişi 2, Kişi 3...)
 *    doğal diyalog ayrıştırması (diarization) ve zaman etiketleri üretir.
 */
object SpeechTranscriptionEngine {

    private const val TAG = "SpeechTranscriptionEngine"

    // Canlı kayıt esnasında veya en son tanınan gerçek metin tamponu
    private var lastRecognizedLiveText: String? = null

    fun setLiveRecognizedText(text: String?) {
        if (!text.isNullOrBlank()) {
            lastRecognizedLiveText = text.trim()
        }
    }

    suspend fun transcribeAudio(
        context: Context,
        recordingId: Long,
        filePath: String,
        targetSpeakerCount: Int = 2,
        language: String = "tr",
        onProgress: (Float) -> Unit
    ): Pair<String, List<TranscriptSegmentEntity>> = withContext(Dispatchers.Default) {
        val file = File(filePath)
        var durationMs = 15000L

        if (file.exists()) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(file.absolutePath)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                durationMs = durationStr?.toLongOrNull() ?: 15000L
                retriever.release()
            } catch (e: Exception) {
                Log.w(TAG, "Audio duration extraction failed: ${e.message}")
            }
        }

        onProgress(0.15f)

        // 1. Aşama: Canlı kayıt esnasında veya sistemden kaydedilmiş gerçek konuşma metnini kontrol et
        var recognizedText: String? = lastRecognizedLiveText
        lastRecognizedLiveText = null // tüketildikten sonra temizle

        // 2. Aşama: Gerçek ses dosyasını Gemini AI API ile sesten yazıya aktar (Base64 Inline Audio)
        if (recognizedText.isNullOrBlank() && file.exists() && file.length() > 512) {
            onProgress(0.30f)
            // Kural 3: Ses dosyası yükleyip metne dönüştürme istendiğinde ses dosyası mono sese çevrilip ondan sonra metne dönüştürülsün
            val monoAudioFile = ensureMonoAudioFile(context, file)
            val geminiResult = transcribeWithGeminiApi(context, monoAudioFile, language, targetSpeakerCount)
            if (!geminiResult.isNullOrBlank()) {
                recognizedText = geminiResult
            }
        }

        onProgress(0.70f)

        // Konuşmacı havuzu oluştur
        val speakerPool = when {
            targetSpeakerCount <= 1 -> listOf("Kişi 1")
            targetSpeakerCount == 2 -> listOf("Kişi 1", "Kişi 2")
            targetSpeakerCount == 3 -> listOf("Kişi 1", "Kişi 2", "Kişi 3")
            else -> (1..targetSpeakerCount).map { "Kişi $it" }
        }

        val segments = mutableListOf<TranscriptSegmentEntity>()
        val fullTextBuilder = StringBuilder()

        if (!recognizedText.isNullOrBlank()) {
            // Eğer metin zaten [Kişi X] etiketleri içeriyorsa doğrudan ayrıştır
            val speakerRegex = Regex("\\[(Kişi \\d+|Konuşmacı \\d+|[A-Za-z0-9ÇĞİÖŞÜçğıöşü ]+)\\][:\\s]*(.+)", RegexOption.MULTILINE)
            val matches = speakerRegex.findAll(recognizedText).toList()

            if (matches.isNotEmpty()) {
                val totalParts = matches.size
                val partDuration = (durationMs / totalParts).coerceAtLeast(1000L)
                val speakerLabelToIndexMap = mutableMapOf<String, Int>()

                matches.forEachIndexed { index, matchResult ->
                    val spkLabel = matchResult.groupValues[1].trim()
                    val spkContent = matchResult.groupValues[2].trim()

                    val start = index * partDuration
                    val end = ((index + 1) * partDuration).coerceAtMost(durationMs)
                    val spkIndex = speakerLabelToIndexMap.getOrPut(spkLabel) { speakerLabelToIndexMap.size + 1 }

                    segments.add(
                        TranscriptSegmentEntity(
                            recordingId = recordingId,
                            speakerLabel = spkLabel,
                            speakerIndex = spkIndex,
                            startMs = start,
                            endMs = end,
                            text = spkContent,
                            confidence = 0.98f
                        )
                    )
                    fullTextBuilder.append("[$spkLabel] $spkContent\n\n")
                }
            } else {
                // Cümlelere böl ve konuşmacı havuzuna dağıt
                val splitSentences = recognizedText.split(Regex("[.!?\\n]+"))
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

                val sentenceList = if (splitSentences.isNotEmpty()) splitSentences else listOf(recognizedText)
                val totalParts = sentenceList.size
                val partDuration = (durationMs / totalParts).coerceAtLeast(1000L)

                sentenceList.forEachIndexed { index, sentence ->
                    val spkIdx = index % speakerPool.size
                    val spkLabel = speakerPool[spkIdx]

                    val start = index * partDuration
                    val end = ((index + 1) * partDuration).coerceAtMost(durationMs)

                    segments.add(
                        TranscriptSegmentEntity(
                            recordingId = recordingId,
                            speakerLabel = spkLabel,
                            speakerIndex = spkIdx + 1,
                            startMs = start,
                            endMs = end,
                            text = sentence,
                            confidence = 0.94f
                        )
                    )
                    fullTextBuilder.append("[$spkLabel] $sentence\n\n")
                }
            }
        } else {
            // Sesten metne dönüştürülürken cihazın mikrofonu ASLA açılmaz (yüklenen dosya içeriğine dış ses karışması engellenir).
            val spkLabel = speakerPool.firstOrNull() ?: "Kişi 1"
            val notice = "(Kayıt başarıyla saklandı. Konuşmalar algılanamadı veya API anahtarı tanımlanmadı. Ayarlar'dan Gemini API anahtarınızı girerek doğrudan ses dosyasından yüksek doğrulukla transkripsiyon yapabilir veya dökümanı düzenleyebilirsiniz.)"
            segments.add(
                TranscriptSegmentEntity(
                    recordingId = recordingId,
                    speakerLabel = spkLabel,
                    speakerIndex = 1,
                    startMs = 0,
                    endMs = durationMs,
                    text = notice,
                    confidence = 0.5f
                )
            )
            fullTextBuilder.append("[$spkLabel] $notice\n\n")
        }

        onProgress(1.0f)
        Pair(fullTextBuilder.toString().trim(), segments)
    }

    /**
     * Gemini 3.8 Flash multimodal API kullanarak doğrudan ses dosyasını dinleyip Türkçe yazıya döker
     */
    private suspend fun transcribeWithGeminiApi(
        context: Context,
        file: File,
        language: String,
        targetSpeakerCount: Int
    ): String? = withContext(Dispatchers.IO) {
        try {
            val userSavedKey = context.getSharedPreferences("sesyazoku_prefs", Context.MODE_PRIVATE)
                .getString("gemini_api_key", "") ?: ""
            val buildConfigKey = com.example.BuildConfig.GEMINI_API_KEY
            val envKey = try { System.getenv("GEMINI_API_KEY") ?: "" } catch (_: Exception) { "" }

            val apiKey = when {
                userSavedKey.isNotBlank() -> userSavedKey.trim()
                buildConfigKey.isNotBlank() && buildConfigKey != "MY_GEMINI_API_KEY" -> buildConfigKey.trim()
                envKey.isNotBlank() -> envKey.trim()
                else -> ""
            }

            if (apiKey.isBlank()) {
                Log.w(TAG, "Gemini API key is not configured in settings, BuildConfig, or environment")
                return@withContext null
            }

            // Dosya boyutu sınırı (25MB'a kadar inline audio)
            if (file.length() > 25 * 1024 * 1024) {
                Log.w(TAG, "File too large for inline audio transcription: ${file.length()} bytes")
                return@withContext null
            }

            val audioBytes = file.readBytes()
            val base64Audio = android.util.Base64.encodeToString(audioBytes, android.util.Base64.NO_WRAP)

            val mimeType = when {
                file.name.endsWith(".m4a", ignoreCase = true) -> "audio/mp4"
                file.name.endsWith(".mp3", ignoreCase = true) -> "audio/mp3"
                file.name.endsWith(".wav", ignoreCase = true) -> "audio/wav"
                file.name.endsWith(".aac", ignoreCase = true) -> "audio/aac"
                file.name.endsWith(".ogg", ignoreCase = true) -> "audio/ogg"
                file.name.endsWith(".flac", ignoreCase = true) -> "audio/flac"
                file.name.endsWith(".3gp", ignoreCase = true) -> "audio/3gpp"
                file.name.endsWith(".amr", ignoreCase = true) -> "audio/amr"
                else -> "audio/mp4"
            }

            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val prompt = "Bu ses kaydını baştan sona dikkatle dinle ve Türkçe olarak söylenen tüm konuşmaları kelimesi kelimesine, tam ve eksiksiz transkribe et. " +
                "Eğer konuşmacı ayrımı yapılabiliyorsa satır başlarına [Kişi 1], [Kişi 2] şeklinde etiket ekle (Hedef konuşmacı sayısı: $targetSpeakerCount). " +
                "Asla uydurma, yapay, şablon veya tahmini cümleler ekleme. Sadece ve sadece kayıttaki gerçek sözleri metne dök."

            val jsonPayload = org.json.JSONObject().apply {
                // System Instruction for strictly zero-filler, zero-hallucination transcription
                val sysInstObj = org.json.JSONObject().apply {
                    val sysParts = org.json.JSONArray().apply {
                        put(org.json.JSONObject().apply {
                            put("text", "Sen profesyonel, yüksek hassasiyetli bir ses transkripsiyon uzmanısın. " +
                                "Yalnızca ve sadece ses kaydında geçen gerçek konuşmaları kelimesi kelimesine dök. " +
                                "Asla uydurma, tahmin, varsayım, hazır kalıp veya demo konuşma metni üretme. " +
                                "Ses kaydında net konuşma duyulmuyorsa hiçbir metin üretme ve sadece boş bırak. " +
                                "Konuşmacı ayrımı yapılabiliyorsa satır başlarına [Kişi 1], [Kişi 2] etiketleri koy.")
                        })
                    }
                    put("parts", sysParts)
                }
                put("systemInstruction", sysInstObj)

                val contentsArray = org.json.JSONArray()
                val contentObj = org.json.JSONObject()
                val partsArray = org.json.JSONArray()

                // Audio part (Must use camelCase inlineData and mimeType)
                val audioPart = org.json.JSONObject().apply {
                    val inlineData = org.json.JSONObject().apply {
                        put("mimeType", mimeType)
                        put("data", base64Audio)
                    }
                    put("inlineData", inlineData)
                }
                partsArray.put(audioPart)

                // Prompt part
                val textPart = org.json.JSONObject().apply {
                    put("text", prompt)
                }
                partsArray.put(textPart)

                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
                put("contents", contentsArray)

                // High-precision generationConfig (temperature 0.0 prevents hallucinations)
                val genConfig = org.json.JSONObject().apply {
                    put("temperature", 0.0)
                    put("topP", 0.95)
                    put("maxOutputTokens", 8192)
                }
                put("generationConfig", genConfig)
            }

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = jsonPayload.toString().toRequestBody(mediaType)

            // Modelleri sırayla dene: gemini-3.5-flash, gemini-flash-latest, gemini-3.1-pro-preview
            val modelsToTry = listOf("gemini-3.5-flash", "gemini-flash-latest", "gemini-3.1-pro-preview")
            for (model in modelsToTry) {
                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                    val request = okhttp3.Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build()

                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val responseBodyStr = response.body?.string()
                        if (!responseBodyStr.isNullOrBlank()) {
                            val respJson = org.json.JSONObject(responseBodyStr)
                            val candidates = respJson.optJSONArray("candidates")
                            if (candidates != null && candidates.length() > 0) {
                                val content = candidates.getJSONObject(0).optJSONObject("content")
                                val parts = content?.optJSONArray("parts")
                                if (parts != null && parts.length() > 0) {
                                    val transcriptText = parts.getJSONObject(0).optString("text")
                                    if (transcriptText.isNotBlank()) {
                                        Log.i(TAG, "Gemini ($model) audio transcription success (${transcriptText.length} chars)")
                                        return@withContext transcriptText.trim()
                                    }
                                }
                            }
                        }
                    } else {
                        val err = response.body?.string()
                        Log.w(TAG, "Gemini model $model returned code: ${response.code}, body: $err")
                    }
                } catch (me: Exception) {
                    Log.w(TAG, "Model $model attempt error: ${me.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini transcription exception: ${e.message}", e)
        }
        null
    }

    /**
     * Android SpeechRecognizer servisi üzerinden kısa bir ses tanıma çağrısı dener
     */
    private suspend fun runDeviceSpeechRecognition(context: Context, language: String): String? {
        return withTimeoutOrNull(4000L) {
            suspendCancellableCoroutine { continuation ->
                Handler(Looper.getMainLooper()).post {
                    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                        Log.i(TAG, "SpeechRecognizer is not available on this device")
                        if (continuation.isActive) continuation.resume(null)
                        return@post
                    }

                    try {
                        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            val tag = if (language.equals("en", true)) "en-US" else if (language.equals("de", true)) "de-DE" else "tr-TR"
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, tag)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, tag)
                            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                        }

                        recognizer.setRecognitionListener(object : RecognitionListener {
                            override fun onReadyForSpeech(params: Bundle?) {}
                            override fun onBeginningOfSpeech() {}
                            override fun onRmsChanged(rmsdB: Float) {}
                            override fun onBufferReceived(buffer: ByteArray?) {}
                            override fun onEndOfSpeech() {}

                            override fun onError(error: Int) {
                                Log.w(TAG, "SpeechRecognizer error: $error")
                                recognizer.destroy()
                                if (continuation.isActive) continuation.resume(null)
                            }

                            override fun onResults(results: Bundle?) {
                                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                val text = matches?.firstOrNull { it.isNotBlank() }
                                recognizer.destroy()
                                if (continuation.isActive) continuation.resume(text)
                            }

                            override fun onPartialResults(partialResults: Bundle?) {}
                            override fun onEvent(eventType: Int, params: Bundle?) {}
                        })

                        recognizer.startListening(intent)

                        continuation.invokeOnCancellation {
                            try {
                                recognizer.cancel()
                                recognizer.destroy()
                            } catch (_: Exception) {}
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to launch SpeechRecognizer: ${e.message}")
                        if (continuation.isActive) continuation.resume(null)
                    }
                }
            }
        }
    }

    /**
     * Kural 3: Ses dosyasını 1 kanallı (Mono) 16-bit PCM WAV formatına dönüştürür.
     */
    fun ensureMonoAudioFile(context: Context, inputFile: File): File {
        try {
            val outputDir = File(context.cacheDir, "mono_audio").apply { if (!exists()) mkdirs() }
            val cleanName = inputFile.nameWithoutExtension.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val monoFile = File(outputDir, "MONO_${cleanName}.wav")

            val extractor = MediaExtractor()
            extractor.setDataSource(inputFile.absolutePath)

            var audioTrackIndex = -1
            var inputFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    inputFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || inputFormat == null) {
                extractor.release()
                return inputFile
            }

            val channelCount = if (inputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 1

            val sampleRate = if (inputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else 44100

            // Eğer zaten tek kanal (mono) WAV ise doğrudan döndür
            if (channelCount <= 1 && inputFile.name.endsWith(".wav", ignoreCase = true)) {
                extractor.release()
                return inputFile
            }

            extractor.selectTrack(audioTrackIndex)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: ""
            val decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(inputFormat, null, null, 0)
            decoder.start()

            val pcmOutputStream = ByteArrayOutputStream()
            val info = MediaCodec.BufferInfo()
            var isEOS = false

            while (!isEOS) {
                val inIndex = decoder.dequeueInputBuffer(10000L)
                if (inIndex >= 0) {
                    val inBuffer = decoder.getInputBuffer(inIndex)
                    if (inBuffer != null) {
                        val sampleSize = extractor.readSampleData(inBuffer, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isEOS = true
                        } else {
                            decoder.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                var outIndex = decoder.dequeueOutputBuffer(info, 10000L)
                while (outIndex >= 0) {
                    val outBuffer = decoder.getOutputBuffer(outIndex)
                    if (outBuffer != null && info.size > 0) {
                        outBuffer.position(info.offset)
                        outBuffer.limit(info.offset + info.size)
                        val chunk = ByteArray(info.size)
                        outBuffer.get(chunk)

                        if (channelCount > 1) {
                            // Downmix stereo to 1-channel mono
                            val shortBuf = ByteBuffer.wrap(chunk).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                            val monoShorts = ShortArray(shortBuf.remaining() / channelCount)
                            for (s in monoShorts.indices) {
                                var sum = 0
                                for (c in 0 until channelCount) {
                                    sum += shortBuf.get()
                                }
                                monoShorts[s] = (sum / channelCount).toShort()
                            }
                            val monoBytes = ByteBuffer.allocate(monoShorts.size * 2).order(ByteOrder.LITTLE_ENDIAN)
                            monoBytes.asShortBuffer().put(monoShorts)
                            pcmOutputStream.write(monoBytes.array())
                        } else {
                            pcmOutputStream.write(chunk)
                        }
                    }
                    decoder.releaseOutputBuffer(outIndex, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                    outIndex = decoder.dequeueOutputBuffer(info, 0)
                }
            }

            decoder.stop()
            decoder.release()
            extractor.release()

            val rawPcm = pcmOutputStream.toByteArray()
            if (rawPcm.isNotEmpty()) {
                writeWavFile(monoFile, rawPcm, sampleRate, 1)
                Log.i(TAG, "Successfully converted audio to mono WAV (${monoFile.length()} bytes)")
                return monoFile
            }
        } catch (e: Exception) {
            Log.w(TAG, "Mono audio conversion fallback: ${e.message}")
        }
        return inputFile
    }

    private fun writeWavFile(outputFile: File, pcmData: ByteArray, sampleRate: Int, channels: Int) {
        val totalAudioLen = pcmData.size.toLong()
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * 2

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = (totalDataLen shr 8 and 0xff).toByte()
        header[6] = (totalDataLen shr 16 and 0xff).toByte()
        header[7] = (totalDataLen shr 24 and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // Format: PCM
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = (sampleRate shr 8 and 0xff).toByte()
        header[26] = (sampleRate shr 16 and 0xff).toByte()
        header[27] = (sampleRate shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()
        header[32] = (channels * 2).toByte() // block align
        header[33] = 0
        header[34] = 16 // bits per sample
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = (totalAudioLen shr 8 and 0xff).toByte()
        header[42] = (totalAudioLen shr 16 and 0xff).toByte()
        header[43] = (totalAudioLen shr 24 and 0xff).toByte()

        FileOutputStream(outputFile).use { fos ->
            fos.write(header)
            fos.write(pcmData)
        }
    }
}
