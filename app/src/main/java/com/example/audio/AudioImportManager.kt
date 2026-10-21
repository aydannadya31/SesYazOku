package com.example.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.data.RecordingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object AudioImportManager {

    suspend fun importAudioFile(context: Context, uri: Uri): RecordingEntity? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            var fileName = "Imported_Audio_${System.currentTimeMillis()}"
            var fileSize = 0L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        cursor.getString(nameIndex)?.let { fileName = it }
                    }
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }

            val extension = fileName.substringAfterLast('.', "m4a")
            val outputDir = File(context.filesDir, "recordings").apply { if (!exists()) mkdirs() }
            val destinationFile = File(outputDir, "IMP_${System.currentTimeMillis()}.$extension")

            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return@withContext null

            // Kural 3: Ses dosyası yüklendiğinde mono sese çevrilip ondan sonra kaydedilsin/dönüştürülsün
            val monoFile = AudioMonoConverter.convertToMonoIfNeeded(context, destinationFile)
            val finalFile = if (monoFile.exists() && monoFile.length() > 0) monoFile else destinationFile

            if (fileSize == 0L || finalFile != destinationFile) {
                fileSize = finalFile.length()
            }

            var durationMs = 0L
            var mimeType = if (finalFile.extension.equals("wav", ignoreCase = true)) "audio/wav" else "audio/*"
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(finalFile.absolutePath)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                durationMs = durationStr?.toLongOrNull() ?: 0L
                val mimeStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                if (mimeStr != null) mimeType = mimeStr
                retriever.release()
            } catch (e: Exception) {
                Log.w("AudioImportManager", "Could not extract metadata", e)
            }

            val cleanTitle = fileName.substringBeforeLast('.')

            RecordingEntity(
                title = cleanTitle,
                filePath = finalFile.absolutePath,
                durationMs = durationMs,
                fileSize = fileSize,
                mimeType = mimeType,
                sourceType = "IMPORTED",
                timestamp = System.currentTimeMillis(),
                isTranscribed = false,
                transcriptFullText = "",
                speakerCount = 1
            )
        } catch (e: Exception) {
            Log.e("AudioImportManager", "Failed to import audio file", e)
            null
        }
    }
}
