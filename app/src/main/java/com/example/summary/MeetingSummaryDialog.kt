package com.example.summary

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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RecordingEntity
import com.example.data.TranscriptSegmentEntity
import java.util.Locale

@Composable
fun MeetingSummaryDialog(
    recording: RecordingEntity,
    segments: List<TranscriptSegmentEntity>,
    onDismiss: () -> Unit,
    onShareSummary: (String) -> Unit
) {
    val context = LocalContext.current
    val summaryData = remember(recording.id, segments.size) {
        if (recording.summaryJson.isNotBlank()) {
            MeetingSummaryGenerator.fromJson(recording.summaryJson) ?: MeetingSummaryGenerator.generateSummary(recording, segments)
        } else {
            MeetingSummaryGenerator.generateSummary(recording, segments)
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("📋 Özet & Kararlar", "✅ Görevler", "📊 Katılım %")

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
                            Icon(Icons.Default.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Toplantı Özeti & Görevler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 12.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(modifier = Modifier.height(320.dp)) {
                    when (selectedTab) {
                        0 -> {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("📌 Genel Değerlendirme", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(summaryData.overview, style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp)
                                        }
                                    }
                                }

                                item {
                                    Text("🎯 Alınan Kararlar ve Çıkarımlar", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }

                                items(summaryData.keyDecisions) { decision ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(decision, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                        1 -> {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    Text("Aksiyon Maddeleri ve Sorumlular", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                                items(summaryData.actionItems) { action ->
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(action, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                item {
                                    Text("Konuşmacı Katılım ve Konuşma Oranları", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                                items(summaryData.speakerStats) { stat ->
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(stat.speakerLabel, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text("%${stat.percentage} (${stat.totalTimeMs / 1000} sn • ${stat.segmentCount} konuşma)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            LinearProgressIndicator(
                                                progress = { stat.percentage / 100f },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                            )
                                        }
                                    }
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
                    val fullSummaryText = buildString {
                        appendLine("📋 *TOPLANTI ÖZETİ VE AKSİYON RAPORU*")
                        appendLine("📁 Başlık: ${recording.title}")
                        appendLine("--------------------------------")
                        appendLine("📌 *Genel Özet:*")
                        appendLine(summaryData.overview)
                        appendLine("\n🎯 *Alınan Kararlar:*")
                        summaryData.keyDecisions.forEach { appendLine("• $it") }
                        appendLine("\n✅ *Aksiyon Listesi:*")
                        summaryData.actionItems.forEach { appendLine("• $it") }
                        appendLine("\n📊 *Konuşmacı Katılımları:*")
                        summaryData.speakerStats.forEach { appendLine("• ${it.speakerLabel}: %${it.percentage}") }
                        appendLine("--------------------------------")
                        appendLine("📲 SesYazOku Toplantı Asistanı")
                    }
                    onShareSummary(fullSummaryText)
                }
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Özeti Paylaş")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    val fullSummaryText = buildString {
                        appendLine("📋 TOPLANTI ÖZETİ: ${recording.title}")
                        appendLine(summaryData.overview)
                        appendLine("\nKARARLAR:")
                        summaryData.keyDecisions.forEach { appendLine("- $it") }
                        appendLine("\nGÖREVLER:")
                        summaryData.actionItems.forEach { appendLine("- $it") }
                    }
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("Toplantı Özeti", fullSummaryText))
                    Toast.makeText(context, "Özet panoya kopyalandı!", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Kopyala")
            }
        }
    )
}
