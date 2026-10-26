package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RecordingEntity
import com.example.data.TranscriptSegmentEntity
import com.example.export.PdfExportDialog
import com.example.export.PdfManager
import com.example.export.ShareManager
import com.example.export.SubtitleExporter
import com.example.summary.MeetingSummaryDialog
import com.example.translation.TranslationDialog
import com.example.ui.MainViewModel
import com.example.ui.components.ColorAndCategoryDialog
import com.example.ui.components.InteractiveWaveformBar
import com.example.ui.components.SpeakerAndTextEditDialog
import com.example.ui.components.UndoSnackbarBanner
import com.example.ui.util.AdaptiveButtonText
import com.example.ui.util.Localization
import org.json.JSONArray
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
    val context = LocalContext.current
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
    val skipSilenceEnabled by playerManager.skipSilenceEnabled.collectAsState()
    val vocalBoostEnabled by playerManager.vocalBoostEnabled.collectAsState()

    val language by viewModel.language.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsState()
    val activeUndo by viewModel.activeUndo.collectAsState()

    var speakerCount by remember { mutableIntStateOf(2) }
    var editingSegment by remember { mutableStateOf<TranscriptSegmentEntity?>(null) }
    var summaryDialogRecording by remember { mutableStateOf<RecordingEntity?>(null) }
    var translateDialogRecording by remember { mutableStateOf<RecordingEntity?>(null) }
    var exportDialogRecording by remember { mutableStateOf<RecordingEntity?>(null) }
    var colorTagDialogRecording by remember { mutableStateOf<RecordingEntity?>(null) }

    // Filter recordings by search and category (Items 6 & 11)
    val categories = listOf("Tümü", "Genel", "Toplantı", "Ders", "Röportaj", "Sesli Not", "Önemli")
    val filteredRecordings = recordings.filter { item ->
        val matchesCat = selectedCategoryFilter == "Tümü" || item.category.equals(selectedCategoryFilter, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.transcriptFullText.contains(searchQuery, ignoreCase = true) ||
                item.category.contains(searchQuery, ignoreCase = true) ||
                item.projectName.contains(searchQuery, ignoreCase = true)
        matchesCat && matchesSearch
    }

    Box(modifier = Modifier.fillMaxSize()) {
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

            Spacer(modifier = Modifier.height(12.dp))

            // Item 6: Global Full-Text Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("global_search_input"),
                placeholder = { Text("Tüm sesler ve konuşmalarda ara...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Ara", tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Temizle")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Item 11: Category Filter Chips Row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategoryFilter == cat,
                        onClick = { viewModel.setSelectedCategoryFilter(cat) },
                        label = { Text(cat, fontSize = 11.sp, fontWeight = if (selectedCategoryFilter == cat) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }
            }

            if (filteredRecordings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedCategoryFilter != "Tümü") "Aramanızla eşleşen kayıt bulunamadı" else "Henüz bir ses kaydı bulunmuyor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Kayıt sekmesinden yeni ses kaydedebilir veya dosya içe aktarabilirsiniz.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredRecordings, key = { it.id }) { item ->
                        val isSelected = selectedId == item.id
                        val isAudioPlayingThis = isPlaying && playingPath == item.filePath

                        // Pass segment boundaries for Skip Silence Mode (Item 5)
                        if (isAudioPlayingThis && isSelected && segments.isNotEmpty()) {
                            playerManager.setSegmentBoundaries(segments.map { it.startMs to it.endMs })
                        }

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
                                // Item Header
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
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // Item 11: Color tag circle
                                            val colorParsed = try {
                                                Color(android.graphics.Color.parseColor(item.colorTag))
                                            } catch (_: Exception) {
                                                MaterialTheme.colorScheme.primary
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(colorParsed, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        val dateStr = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.forLanguageTag("tr")).format(Date(item.timestamp))
                                        Text(
                                            text = "$dateStr • ${item.category} • ${if (item.isTranscribed) "${item.speakerCount} Konuşmacı Metni Hazır" else "Metne Dönüştürme Bekliyor"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Play / Pause Audio Icon
                                    IconButton(
                                        onClick = {
                                            viewModel.selectRecordingForView(item.id)
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

                                // Item 1: Interactive Waveform Scrubber & Player Controls
                                if (isAudioPlayingThis) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    InteractiveWaveformBar(
                                        currentPositionMs = currentPositionMs,
                                        durationMs = if (durationMs > 0) durationMs else item.durationMs,
                                        isPlaying = isPlaying,
                                        segments = if (isSelected) segments else emptyList(),
                                        onSeek = { playerManager.seekTo(it) }
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Additional Audio Controls: Skip Silence (Item 5), Vocal Boost (Item 10), Speeds
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Item 5: Skip Silence Toggle
                                        FilterChip(
                                            selected = skipSilenceEnabled,
                                            onClick = { playerManager.toggleSkipSilence() },
                                            label = { Text("🔇 Sessizliği Atla", fontSize = 11.sp, fontWeight = if (skipSilenceEnabled) FontWeight.Bold else FontWeight.Normal) },
                                            modifier = Modifier.height(28.dp)
                                        )

                                        // Item 10: Vocal Boost Toggle
                                        FilterChip(
                                            selected = vocalBoostEnabled,
                                            onClick = { playerManager.toggleVocalBoost() },
                                            label = { Text("🎙️ Sesi Netleştir", fontSize = 11.sp, fontWeight = if (vocalBoostEnabled) FontWeight.Bold else FontWeight.Normal) },
                                            modifier = Modifier.height(28.dp)
                                        )

                                        // Speed chips
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            listOf(0.5f to "0.5x", 1.0f to "1x", 1.5f to "1.5x", 2.0f to "2x").forEach { (spd, lbl) ->
                                                FilterChip(
                                                    selected = playbackSpeed == spd,
                                                    onClick = { playerManager.setPlaybackSpeed(spd) },
                                                    label = { Text(lbl, fontSize = 11.sp, fontWeight = if (playbackSpeed == spd) FontWeight.Bold else FontWeight.Normal) },
                                                    modifier = Modifier.height(28.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Item 4: Bookmarks / Markers Display
                                if (item.markersJson.isNotBlank()) {
                                    val markers = remember(item.markersJson) {
                                        val list = mutableListOf<Pair<Long, String>>()
                                        try {
                                            val arr = JSONArray(item.markersJson)
                                            for (i in 0 until arr.length()) {
                                                val o = arr.getJSONObject(i)
                                                list.add(o.getLong("timestampMs") to o.getString("label"))
                                            }
                                        } catch (_: Exception) {}
                                        list
                                    }

                                    if (markers.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("📌 İşaretli Anlar (Yer İmleri):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            items(markers) { (timeMs, label) ->
                                                val sec = timeMs / 1000
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                                    modifier = Modifier.clickable {
                                                        viewModel.selectRecordingForView(item.id)
                                                        playerManager.playFile(item.filePath, timeMs)
                                                    }
                                                ) {
                                                    Text(
                                                        text = String.format(Locale.getDefault(), "▶️ %02d:%02d (%s)", sec / 60, sec % 60, label),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Medium,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // If not transcribed yet
                                if (!item.isTranscribed) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "ℹ️ Ses kaydı henüz metne dönüştürülmedi. Konuşmacı sayısı (Kişi 1, Kişi 2...), metne dönüştürme tamamlandığında otomatik olarak tespit edilir.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = {
                                            viewModel.selectRecordingForView(item.id)
                                            viewModel.runTranscription(item)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        AdaptiveButtonText(text = Localization.get("trans_transcribe_btn", language))
                                    }
                                } else {
                                    // Detected speakers banner
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Group,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "🎯 Tespit Edilen Kişi Sayısı: ${item.speakerCount} Kişi",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Preview snippet
                                    Text(
                                        text = item.transcriptFullText.take(160) + if (item.transcriptFullText.length > 160) "..." else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 3
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Quick action buttons row: Summary (Item 2), Translate (Item 8), WhatsApp (Item 12), PDF/Subtitles (Items 3 & 7)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Item 2: Meeting Summary
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.selectRecordingForView(item.id)
                                                summaryDialogRecording = item
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Özet", fontSize = 11.sp)
                                        }

                                        // Item 8: Translation
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.selectRecordingForView(item.id)
                                                translateDialogRecording = item
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Çeviri", fontSize = 11.sp)
                                        }

                                        // Item 12: Quick WhatsApp / Mail Card
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.selectRecordingForView(item.id)
                                                viewModel.getSegmentsForRecording(item.id) { segs ->
                                                    ShareManager.shareQuickCard(context, item, segs)
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.weight(1.2f)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Paylaş", fontSize = 11.sp)
                                        }

                                        // Item 3 & 7: PDF / Subtitle Exporter
                                        Button(
                                            onClick = {
                                                viewModel.selectRecordingForView(item.id)
                                                exportDialogRecording = item
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.weight(1.3f)
                                        ) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Dışa Aktar", fontSize = 11.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Secondary row: Edit in Editor, Color Tag (Item 11)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.selectRecordingForView(item.id)
                                                colorTagDialogRecording = item
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Label, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Etiket / Renk", fontSize = 11.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                viewModel.selectRecordingForView(item.id)
                                                onNavigateToEditor?.invoke() ?: onNavigateToDocuments()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Metni Düzenle", fontSize = 11.sp)
                                        }
                                    }
                                }

                                // Diarized segments view with Live Karaoke (Item 1)
                                if (isSelected && segments.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Ayrılmış Konuşma Diyalogları",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                if (isAudioPlayingThis) {
                                                    Text(
                                                        text = "🎙️ Canlı Karaoke Aktif",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            segments.forEach { seg ->
                                                val isThisSegmentSpeaking = isAudioPlayingThis && currentPositionMs in seg.startMs..seg.endMs
                                                SpeakerSegmentBubble(
                                                    segment = seg,
                                                    isSpeaking = isThisSegmentSpeaking,
                                                    onPlayFromHere = {
                                                        playerManager.playFile(item.filePath, seg.startMs)
                                                    },
                                                    onEdit = { editingSegment = seg }
                                                )
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

        // Item 9: 10-Second Undo Banner at screen bottom
        UndoSnackbarBanner(
            activeUndo = activeUndo,
            onUndo = { viewModel.executeActiveUndo() },
            onDismiss = { viewModel.dismissActiveUndo() },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Modal Dialogs
    editingSegment?.let { seg ->
        val activeRec = recordings.firstOrNull { it.id == seg.recordingId } ?: recordings.firstOrNull { it.id == selectedId }
        activeRec?.let { rec ->
            SpeakerAndTextEditDialog(
                segment = seg,
                recordingId = rec.id,
                onDismiss = { editingSegment = null },
                onUpdateExactSpeakerName = { oldName, newName ->
                    viewModel.updateSpeakerLabelByExactName(rec.id, oldName, newName)
                },
                onUpdateSingleSpeaker = { segId, newName ->
                    viewModel.updateSingleSegmentSpeaker(segId, rec.id, newName)
                },
                onUpdateSegmentText = { segId, newText ->
                    viewModel.updateSegmentText(segId, rec.id, newText)
                }
            )
        }
    }

    summaryDialogRecording?.let { rec ->
        MeetingSummaryDialog(
            recording = rec,
            segments = segments,
            onDismiss = { summaryDialogRecording = null },
            onShareSummary = { summaryText ->
                PdfManager.shareFullText(context, "${rec.title} - Toplantı Özeti", summaryText)
            }
        )
    }

    translateDialogRecording?.let { rec ->
        TranslationDialog(
            recording = rec,
            segments = segments,
            onDismiss = { translateDialogRecording = null },
            onShareTranslated = { translatedText ->
                PdfManager.shareFullText(context, "${rec.title} - Çeviri", translatedText)
            }
        )
    }

    exportDialogRecording?.let { rec ->
        PdfExportDialog(
            recording = rec,
            segments = segments,
            onDismiss = { exportDialogRecording = null },
            onGeneratePdf = { tmpl, isShare ->
                exportDialogRecording = null
                if (isShare) {
                    viewModel.exportAndSharePdfWithTemplate(context, rec, tmpl)
                } else {
                    viewModel.savePdfToStorageWithTemplate(context, rec, tmpl)
                }
            },
            onExportSubtitle = { isVtt ->
                exportDialogRecording = null
                if (isVtt) {
                    val file = SubtitleExporter.exportAsVtt(context, rec, segments)
                    if (file != null) SubtitleExporter.shareSubtitleFile(context, file, ".VTT")
                } else {
                    val file = SubtitleExporter.exportAsSrt(context, rec, segments)
                    if (file != null) SubtitleExporter.shareSubtitleFile(context, file, ".SRT")
                }
            }
        )
    }

    colorTagDialogRecording?.let { rec ->
        ColorAndCategoryDialog(
            recording = rec,
            onDismiss = { colorTagDialogRecording = null },
            onSave = { cat, colorHex ->
                colorTagDialogRecording = null
                viewModel.updateRecordingCategory(rec.id, cat)
                viewModel.updateRecordingColorTag(rec.id, colorHex)
            }
        )
    }
}

@Composable
fun SpeakerSegmentBubble(
    segment: TranscriptSegmentEntity,
    isSpeaking: Boolean = false,
    onPlayFromHere: () -> Unit = {},
    onEdit: () -> Unit
) {
    val badgeColor = when (segment.speakerIndex % 4) {
        1 -> Color(0xFF2563EB) // Blue
        2 -> Color(0xFF059669) // Emerald
        3 -> Color(0xFFD97706) // Amber
        else -> Color(0xFF7C3AED) // Purple
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSpeaking) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSpeaking) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor,
                        modifier = Modifier.clickable { onEdit() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = segment.speakerLabel,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    if (isSpeaking) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "🔊 Canlı Karaoke",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Play from here button (Item 1)
                    IconButton(
                        onClick = onPlayFromHere,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Buradan Dinle",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("Değiştir", fontSize = 10.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = segment.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSpeaking) FontWeight.SemiBold else FontWeight.Normal
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
