package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.example.ui.util.AdaptiveButtonText
import com.example.ui.util.Localization
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RecordingEntity
import com.example.data.TranscriptSegmentEntity
import com.example.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptsScreen(
    viewModel: MainViewModel,
    onNavigateToDocuments: () -> Unit,
    onNavigateToEditor: (() -> Unit)? = null
) {
    val recordings by viewModel.recordings.collectAsState()
    val isTranscribing by viewModel.isTranscribing.collectAsState()
    val transcribeProgress by viewModel.transcribeProgress.collectAsState()
    val selectedId by viewModel.selectedRecordingId.collectAsState()
    val segments by viewModel.selectedSegments.collectAsState()

    val playerManager = viewModel.playerManager
    val playbackSpeed by playerManager.playbackSpeed.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val playingPath by playerManager.currentPlayingPath.collectAsState()
    val currentPositionMs by playerManager.currentPositionMs.collectAsState()
    val durationMs by playerManager.durationMs.collectAsState()
    val language by viewModel.language.collectAsState()

    var speakerCount by remember { mutableIntStateOf(2) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("transcripts_screen")
    ) {
        // Header
        Text(
            text = "Transkriptor",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Kayıtları sesten yazıya dökün ve konuşmacıları (Kişi 1, Kişi 2...) etiketleyin",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isTranscribing) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Konuşmacılar Ayrıştırılıyor ve Metne Dönüştürülüyor...",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { transcribeProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (recordings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Henüz kaydedilmiş bir ses yok",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Kayıt sekmesinden canlı kayıt yapabilir veya ses dosyası yükleyebilirsiniz.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recordings, key = { it.id }) { item ->
                    val isSelected = selectedId == item.id
                    val isAudioPlayingThis = isPlaying && playingPath == item.filePath

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectRecordingForView(item.id)
                            }
                            .testTag("transcript_item_${item.id}"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (item.isTranscribed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (item.isTranscribed) Icons.Default.CheckCircle else Icons.Default.RecordVoiceOver,
                                            contentDescription = null,
                                            tint = if (item.isTranscribed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val dateStr = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.forLanguageTag("tr")).format(Date(item.timestamp))
                                    Text(
                                        text = "$dateStr • ${if (item.isTranscribed) "${item.speakerCount} Konuşmacı Metni Hazır" else "Transkripsiyon Bekliyor"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Play / Pause Audio Icon
                                IconButton(
                                    onClick = {
                                        playerManager.playOrPause(item.filePath)
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isAudioPlayingThis) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Dinle",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Audio progress indicator & Speed Selector when playing
                            if (isAudioPlayingThis) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        if (durationMs > 0) {
                                            LinearProgressIndicator(
                                                progress = { (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val currentSec = currentPositionMs / 1000
                                            val totalSec = if (durationMs > 0) durationMs / 1000 else item.durationMs / 1000
                                            Text(
                                                text = String.format(
                                                    Locale.getDefault(),
                                                    "%02d:%02d / %02d:%02d (%s)",
                                                    currentSec / 60, currentSec % 60,
                                                    totalSec / 60, totalSec % 60,
                                                    if (playbackSpeed == 1.0f) "1x" else "${playbackSpeed}x"
                                                ),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )

                                            // Fast listening speed selector chips (0.5x, 1x, 1.5x, 2x)
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                listOf(
                                                    0.5f to "0.5x",
                                                    1.0f to "1x",
                                                    1.5f to "1.5x",
                                                    2.0f to "2x"
                                                ).forEach { (spd, lbl) ->
                                                    FilterChip(
                                                        selected = playbackSpeed == spd,
                                                        onClick = { playerManager.setPlaybackSpeed(spd) },
                                                        label = {
                                                            Text(
                                                                text = lbl,
                                                                fontSize = 11.sp,
                                                                fontWeight = if (playbackSpeed == spd) FontWeight.Bold else FontWeight.Normal
                                                            )
                                                        },
                                                        modifier = Modifier.height(28.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action Bar inside item
                            if (!item.isTranscribed) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Kişi Sayısı: ${item.speakerCount}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = {
                                            viewModel.selectRecordingForView(item.id)
                                            viewModel.runTranscription(item, speakerCount = item.speakerCount.coerceAtLeast(1))
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(42.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        AdaptiveButtonText(text = Localization.get("trans_transcribe_btn", language))
                                    }
                                }
                            } else {
                                Text(
                                    text = "Kişi Sayısı: ${item.speakerCount}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                // If already transcribed, show preview snippet & view details button
                                Text(
                                    text = item.transcriptFullText.take(160) + if (item.transcriptFullText.length > 160) "..." else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.selectRecordingForView(item.id)
                                            viewModel.runTranscription(item, speakerCount = item.speakerCount.coerceAtLeast(1))
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        AdaptiveButtonText(text = if (language == "en") "Transcribe Again" else "Yeniden Dönüştür", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.selectRecordingForView(item.id)
                                            onNavigateToEditor?.invoke() ?: onNavigateToDocuments()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Düzenle", fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.selectRecordingForView(item.id)
                                            onNavigateToDocuments()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1.2f)
                                    ) {
                                        Text("Döküman & PDF", fontSize = 11.sp)
                                    }
                                }
                            }

                            // If this recording is currently selected and has segments, show diarized view
                            if (isSelected && segments.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Ayrılmış Konuşma Diyalogları",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        segments.forEach { seg ->
                                            SpeakerSegmentBubble(seg)
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpeakerSegmentBubble(segment: TranscriptSegmentEntity) {
    val badgeColor = when (segment.speakerIndex % 3) {
        1 -> Color(0xFF2563EB) // Blue
        2 -> Color(0xFF059669) // Emerald
        else -> Color(0xFFD97706) // Amber
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = badgeColor,
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Text(
                text = segment.speakerLabel,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = segment.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            val startSec = segment.startMs / 1000
            val endSec = segment.endMs / 1000
            Text(
                text = String.format(Locale.getDefault(), "%02d:%02d - %02d:%02d", startSec / 60, startSec % 60, endSec / 60, endSec % 60),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
