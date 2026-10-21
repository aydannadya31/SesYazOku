package com.example.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * AudioMonoConverter
 *
 * Kural 3: Ses dosyası yükleyip metne dönüştürme istendiğinde
 * ses dosyası stereo veya çoklu kanallı ise mono ses'e (1 kanal)
 * çevrilip ondan sonra metne dönüştürülmesini sağlar.
 */
object AudioMonoConverter {

    private const val TAG = "AudioMonoConverter"
    private const val TIMEOUT_US = 10000L

    /**
     * Converts any input audio file (stereo, multi-channel MP3, M4A, WAV, AAC, etc.)
     * into a clean 1-channel Mono WAV audio file.
     * If the input is already 1-channel mono, returns the original file.
     */
    suspend fun convertToMonoIfNeeded(context: Context, inputFile: File): File = withContext(Dispatchers.IO) {
        if (!inputFile.exists() || inputFile.length() == 0L) {
            return@withContext inputFile
        }

        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(inputFile.absolutePath)
            // Check channel count if available
            val channelsStr = try {
                // Not standard metadata key in older APIs, check via MediaExtractor
                null
            } catch (_: Exception) { null }
            retriever.release()

            val extractor = MediaExtractor()
            extractor.setDataSource(inputFile.absolutePath)

            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || audioFormat == null) {
                extractor.release()
                Log.w(TAG, "No audio track found in file ${inputFile.name}")
                return@withContext inputFile
            }

            val channelCount = if (audioFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else {
                2
            }

            val sampleRate = if (audioFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else {
                16000
            }

            val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: "audio/mp4a-latm"

            // If already mono (1 channel), no conversion needed
            if (channelCount == 1) {
                Log.i(TAG, "File ${inputFile.name} is already mono (1 channel).")
                extractor.release()
                return@withContext inputFile
            }

            Log.i(TAG, "Converting ${inputFile.name} ($channelCount channels, $sampleRate Hz) to mono...")

            extractor.selectTrack(audioTrackIndex)

            val decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(audioFormat, null, null, 0)
            decoder.start()

            val outputDir = File(context.filesDir, "recordings").apply { if (!exists()) mkdirs() }
            val cleanBaseName = inputFile.nameWithoutExtension.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val monoWavFile = File(outputDir, "${cleanBaseName}_mono.wav")

            val fos = FileOutputStream(monoWavFile)
            // Write placeholder 44-byte WAV header
            val headerPlaceholder = ByteArray(44)
            fos.write(headerPlaceholder)

            var totalPcmBytes = 0L
            val bufferInfo = MediaCodec.BufferInfo()
            var isEos = false

            while (!isEos) {
                val inIndex = decoder.dequeueInputBuffer(TIMEOUT_US)
                if (inIndex >= 0) {
                    val inputBuffer = decoder.getInputBuffer(inIndex)
                    if (inputBuffer != null) {
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        } else {
                            decoder.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                var outIndex = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                while (outIndex >= 0) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isEos = true
                    }

                    val outputBuffer = decoder.getOutputBuffer(outIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)

                        val pcmBytes = ByteArray(bufferInfo.size)
                        outputBuffer.get(pcmBytes)

                        // Convert decoded PCM (stereo / multi-channel) to mono
                        val monoBytes = downmixPcmToMono(pcmBytes, channelCount)
                        fos.write(monoBytes)
                        totalPcmBytes += monoBytes.size
                    }

                    decoder.releaseOutputBuffer(outIndex, false)
                    if (isEos) break
                    outIndex = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                }
            }

            fos.flush()
            fos.close()

            decoder.stop()
            decoder.release()
            extractor.release()

            // Update WAV header with exact data size
            writeWavHeader(monoWavFile, totalPcmBytes, sampleRate, 1, 16)
            Log.i(TAG, "Successfully converted to mono: ${monoWavFile.absolutePath} (${monoWavFile.length()} bytes)")

            monoWavFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to downmix audio to mono, falling back to original", e)
            inputFile
        }
    }

    /**
     * Downmixes 16-bit interleaved PCM from N channels to 1 channel (mono)
     */
    private fun downmixPcmToMono(inputPcm: ByteArray, channelCount: Int): ByteArray {
        if (channelCount <= 1) return inputPcm

        val bytesPerSample = 2 // 16-bit PCM
        val frameSize = channelCount * bytesPerSample
        val numFrames = inputPcm.size / frameSize

        val monoPcm = ByteArray(numFrames * bytesPerSample)

        val inBuf = ByteBuffer.wrap(inputPcm).order(ByteOrder.LITTLE_ENDIAN)
        val outBuf = ByteBuffer.wrap(monoPcm).order(ByteOrder.LITTLE_ENDIAN)

        for (f in 0 until numFrames) {
            var sum = 0
            for (ch in 0 until channelCount) {
                sum += inBuf.short.toInt()
            }
            val monoSample = (sum / channelCount).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            outBuf.putShort(monoSample)
        }

        return monoPcm
    }

    /**
     * Overwrites the standard 44-byte WAV header at the beginning of the file.
     */
    private fun writeWavHeader(
        file: File,
        totalAudioLen: Long,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ) {
        val totalDataLen = totalAudioLen + 36
        val byteRate = (sampleRate * channels * bitsPerSample / 8).toLong()
        val blockAlign = (channels * bitsPerSample / 8).toShort()

        val header = ByteArray(44)
        val buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF chunk
        buf.put('R'.code.toByte())
        buf.put('I'.code.toByte())
        buf.put('F'.code.toByte())
        buf.put('F'.code.toByte())
        buf.putInt(totalDataLen.toInt())
        buf.put('W'.code.toByte())
        buf.put('A'.code.toByte())
        buf.put('V'.code.toByte())
        buf.put('E'.code.toByte())

        // fmt chunk
        buf.put('f'.code.toByte())
        buf.put('m'.code.toByte())
        buf.put('t'.code.toByte())
        buf.put(' '.code.toByte())
        buf.putInt(16) // SubChunk1Size (16 for PCM)
        buf.putShort(1.toShort()) // AudioFormat (1 for PCM)
        buf.putShort(channels.toShort())
        buf.putInt(sampleRate)
        buf.putInt(byteRate.toInt())
        buf.putShort(blockAlign)
        buf.putShort(bitsPerSample.toShort())

        // data chunk
        buf.put('d'.code.toByte())
        buf.put('a'.code.toByte())
        buf.put('t'.code.toByte())
        buf.put('a'.code.toByte())
        buf.putInt(totalAudioLen.toInt())

        val raf = RandomAccessFile(file, "rw")
        raf.seek(0)
        raf.write(header)
        raf.close()
    }
}
