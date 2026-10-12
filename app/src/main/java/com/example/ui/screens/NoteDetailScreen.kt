package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.SmartSummarizer
import com.example.data.model.TranscriptSegment
import com.example.export.DocumentExportManager
import com.example.ui.AppViewModel
import com.example.ui.components.AudioPlayerBar
import com.example.ui.components.ExportDialog
import com.example.ui.components.SpeakerChip
import com.example.ui.components.SpeakerEditDialog
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val note by viewModel.activeNote.collectAsState()
    val segments by viewModel.activeSegments.collectAsState()

    // Player state
    val isPlaying by viewModel.audioPlayer.isPlaying.collectAsState()
    val currentPosMs by viewModel.audioPlayer.currentPositionMs.collectAsState()
    val durationMs by viewModel.audioPlayer.durationMs.collectAsState()
    val playbackSpeed by viewModel.audioPlayer.playbackSpeed.collectAsState()

    // TTS state
    val isTtsSpeaking by viewModel.tts.isSpeaking.collectAsState()
    val spokenSegmentIndex by viewModel.tts.currentSpokenSegmentIndex.collectAsState()

    val context = LocalContext.current
    var selectedTabIndex by remember { mutableStateOf(0) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var editingSpeakerSegment by remember { mutableStateOf<TranscriptSegment?>(null) }
    var showTaskExtractedBanner by remember { mutableStateOf(false) }
    var savedPdfFile by remember { mutableStateOf<File?>(null) }
    var showPdfSavedDialog by remember { mutableStateOf(false) }

    if (note == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Not yükleniyor...")
        }
        return
    }

    val currentNote = note!!

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentNote.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    // TTS Read Aloud Button
                    IconButton(
                        onClick = {
                            if (isTtsSpeaking) {
                                viewModel.stopTts()
                            } else {
                                viewModel.readAloudTranscript()
                            }
                        },
                        modifier = Modifier.testTag("tts_play_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = if (isTtsSpeaking) "Durdur" else "Sesli Dinle",
                            tint = if (isTtsSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Export Button
                    IconButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.testTag("export_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Dışa Aktar")
                    }

                    // Favorite Toggle
                    IconButton(onClick = { viewModel.toggleFavorite(currentNote) }) {
                        Icon(
                            imageVector = if (currentNote.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favori",
                            tint = if (currentNote.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Delete
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Sil",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        },
        bottomBar = {
            // Persistent Audio Player Bar if audio exists
            if (!currentNote.audioFilePath.isNullOrBlank()) {
                AudioPlayerBar(
                    isPlaying = isPlaying,
                    currentPosMs = currentPosMs,
                    durationMs = if (durationMs > 0) durationMs else currentNote.durationMs,
                    playbackSpeed = playbackSpeed,
                    onPlayPauseClick = {
                        viewModel.audioPlayer.playOrPause(currentNote.audioFilePath)
                    },
                    onSeekTo = { pos -> viewModel.audioPlayer.seekTo(pos) },
                    onJumpBackward = { viewModel.audioPlayer.jumpBackward5s() },
                    onJumpForward = { viewModel.audioPlayer.jumpForward5s() },
                    onSpeedChange = { speed -> viewModel.audioPlayer.setSpeed(speed) }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // TTS Speaking Notification Pill
            if (isTtsSpeaking) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Metin sesli okunuyor...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(onClick = { viewModel.stopTts() }) {
                        Text("Durdur", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // PDF Actions Row (Cihaza PDF Kaydet & WhatsApp Paylaş)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.saveNotePdfToDevice(currentNote) { file ->
                            if (file != null) {
                                savedPdfFile = file
                                showPdfSavedDialog = true
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("detail_save_pdf_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cihaza PDF Kaydet", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = { viewModel.shareNotePdf(currentNote) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("detail_share_pdf_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PDF Paylaş", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // Tabs: Transkripsiyon vs. Yapay Zeka Özeti
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Transkripsiyon (${segments.size})") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Yapay Zeka Özeti")
                        }
                    }
                )
            }

            if (selectedTabIndex == 0) {
                // Tab 0: Transcripts
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(segments) { segment ->
                        val isHighlightedTts = isTtsSpeaking && spokenSegmentIndex == segments.indexOf(segment)
                        val isAudioActive = currentPosMs in segment.startTimeMs..segment.endTimeMs && isPlaying

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // Seek player to this segment start
                                    if (!currentNote.audioFilePath.isNullOrBlank()) {
                                        viewModel.audioPlayer.playOrPause(currentNote.audioFilePath)
                                        viewModel.audioPlayer.seekTo(segment.startTimeMs)
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isHighlightedTts -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    isAudioActive -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            ),
                            border = if (isHighlightedTts || isAudioActive) {
                                androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Clickable speaker chip to rename
                                    SpeakerChip(
                                        name = segment.speakerName,
                                        colorHex = segment.speakerColorHex,
                                        onClick = { editingSpeakerSegment = segment }
                                    )

                                    val min = (segment.startTimeMs / 1000) / 60
                                    val sec = (segment.startTimeMs / 1000) % 60
                                    Text(
                                        text = String.format("%02d:%02d", min, sec),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = segment.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            } else {
                // Tab 1: AI Summary & Task Extraction
                val actionItems = remember(currentNote.actionPointsJson) {
                    SmartSummarizer.deserializeActionItemsFromJson(currentNote.actionPointsJson)
                }
                val speakerGroup = remember(segments) { segments.groupBy { it.speakerName } }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Summary Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Yönetici Özeti",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.regenerateSummary(currentNote) },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Yenile", fontSize = 11.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = currentNote.summary ?: "Özet bulunmuyor.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Speaker Speaking Time Share
                    if (speakerGroup.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.PieChart,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Konuşmacı Katılım Payı",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    speakerGroup.forEach { (speakerName, sSegments) ->
                                        val totalSpeakerWords = sSegments.sumOf { it.text.split(" ").size }
                                        val totalWords = segments.sumOf { it.text.split(" ").size }.coerceAtLeast(1)
                                        val pct = (totalSpeakerWords * 100) / totalWords

                                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(text = speakerName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                                Text(text = "%$pct ($totalSpeakerWords kelime)", style = MaterialTheme.typography.bodySmall)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(pct / 100f)
                                                        .height(8.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(MaterialTheme.colorScheme.primary)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Extracted Action Items & Task Manager Bridge
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.AssignmentTurnedIn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Çıkarılan Görevler (${actionItems.size})",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.extractTasksFromNote(currentNote)
                                            showTaskExtractedBanner = true
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("extract_tasks_btn")
                                    ) {
                                        Text("Görev Yöneticisine Aktar", fontSize = 11.sp)
                                    }
                                }

                                if (showTaskExtractedBanner) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.secondaryContainer)
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Görevler başarıyla Görev Yöneticisi sekmesine eklendi!",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                actionItems.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (item.priority == "HIGH") MaterialTheme.colorScheme.error
                                                    else MaterialTheme.colorScheme.primary
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (!item.assignee.isNullOrBlank()) {
                                                Text(
                                                    text = "Sorumlu: ${item.assignee}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
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

    // Speaker Edit Dialog
    editingSpeakerSegment?.let { segment ->
        SpeakerEditDialog(
            currentName = segment.speakerName,
            currentColorHex = segment.speakerColorHex,
            onDismiss = { editingSpeakerSegment = null },
            onConfirm = { newName, newColor ->
                viewModel.updateSpeaker(
                    noteId = segment.noteId,
                    speakerId = segment.speakerId,
                    newName = newName,
                    newColorHex = newColor
                )
                editingSpeakerSegment = null
            }
        )
    }

    // Export Dialog
    if (showExportDialog) {
        ExportDialog(
            noteTitle = currentNote.title,
            onDismiss = { showExportDialog = false },
            onExportFormat = { format ->
                viewModel.exportNote(format, currentNote)
                showExportDialog = false
            }
        )
    }

    // PDF Saved Dialog
    if (showPdfSavedDialog && savedPdfFile != null) {
        val file = savedPdfFile!!
        AlertDialog(
            onDismissRequest = { showPdfSavedDialog = false },
            icon = {
                Icon(
                    Icons.Default.FileDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = { Text("PDF Cihaza Kaydedildi", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Transkript metni başarıyla PDF olarak cihazınıza kaydedildi.")
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Text(
                        text = "Konum: İndirilenler (Downloads) klasörü",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPdfSavedDialog = false
                        DocumentExportManager.openPdfFile(context, file)
                    }
                ) {
                    Text("PDF'i Aç", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPdfSavedDialog = false }) {
                    Text("Tamam")
                }
            }
        )
    }

    // Delete Confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Kaydı Sil") },
            text = { Text("\"${currentNote.title}\" başlıklı kayıt ve tüm transkripsiyonu kalıcı olarak silinsin mi?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteNote(currentNote.id)
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("İptal")
                }
            }
        )
    }
}
