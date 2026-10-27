package com.example.translation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RecordingEntity
import com.example.data.TranscriptSegmentEntity

@Composable
fun TranslationDialog(
    recording: RecordingEntity,
    segments: List<TranscriptSegmentEntity>,
    onDismiss: () -> Unit,
    onShareTranslated: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedLang by remember { mutableStateOf("en") }
    var isDualView by remember { mutableStateOf(true) }

    val languages = listOf(
        "en" to "İngilizce (EN)",
        "de" to "Almanca (DE)",
        "fr" to "Fransızca (FR)",
        "es" to "İspanyolca (ES)",
        "ar" to "Arapça (AR)"
    )

    val translatedSegments = remember(selectedLang, segments.size) {
        TranscriptTranslator.translateSegments(segments, selectedLang)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Çoklu Dil Çevirisi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(recording.title.take(30), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Kapat")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Language Chips
                Text("Hedef Dil:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    languages.forEach { (code, label) ->
                        FilterChip(
                            selected = selectedLang == code,
                            onClick = { selectedLang = code },
                            label = { Text(code.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dual View toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isDualView) "Görünüm: Çift Dilli (Yan Yana)" else "Görünüm: Sadece Çeviri",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedButton(
                        onClick = { isDualView = !isDualView },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isDualView) "Tek Dile Geç" else "Çift Dile Geç", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Segments List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(translatedSegments) { item ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(item.speakerLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                if (isDualView) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("🇹🇷 " + item.originalText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("🌐 " + item.translatedText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                } else {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(item.translatedText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fullText = buildString {
                        appendLine("🌐 *ÇEVİRİ RAPORU (${selectedLang.uppercase()})*")
                        appendLine("📁 Başlık: ${recording.title}")
                        appendLine("-------------------------------")
                        translatedSegments.forEach { seg ->
                            if (isDualView) {
                                appendLine("[${seg.speakerLabel}] (TR): ${seg.originalText}")
                                appendLine("[${seg.speakerLabel}] (${selectedLang.uppercase()}): ${seg.translatedText}\n")
                            } else {
                                appendLine("[${seg.speakerLabel}]: ${seg.translatedText}\n")
                            }
                        }
                        appendLine("-------------------------------")
                        appendLine("📲 Scribe Craft Global Akıllı Çeviri")
                    }
                    onShareTranslated(fullText)
                }
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Çeviriyi Paylaş")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    val fullText = translatedSegments.joinToString("\n\n") { "[${it.speakerLabel}]: ${it.translatedText}" }
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("Çeviri Metni", fullText))
                    Toast.makeText(context, "Çeviri metni kopyalandı!", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Kopyala")
            }
        }
    )
}
