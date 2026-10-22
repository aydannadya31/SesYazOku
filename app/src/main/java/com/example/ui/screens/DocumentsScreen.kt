package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.ui.util.AdaptiveButtonText
import com.example.ui.util.Localization
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.RecordingEntity
import com.example.drive.DriveBackupStatus
import com.example.drive.GoogleDriveBackupManager
import com.example.ui.MainViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DocumentsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val recordings by viewModel.recordings.collectAsState()
    val driveBackupState by viewModel.driveBackupUiState.collectAsState()
    val userProfile by viewModel.authManager.userProfile.collectAsState()
    val language by viewModel.language.collectAsState()

    var activeViewingDocId by remember { mutableStateOf<Long?>(null) }
    var documentsSubTab by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var isDocSelectorExpanded by remember { mutableStateOf(false) }

    val playerManager = viewModel.playerManager
    val isPlaying by playerManager.isPlaying.collectAsState()
    val currentPositionMs by playerManager.currentPositionMs.collectAsState()
    val durationMs by playerManager.durationMs.collectAsState()
    val playbackSpeed by playerManager.playbackSpeed.collectAsState()
    val currentPlayingPath by playerManager.currentPlayingPath.collectAsState()

    val segments by viewModel.selectedSegments.collectAsState()

    val activeDoc = remember(recordings, activeViewingDocId) {
        recordings.firstOrNull { it.id == activeViewingDocId }
            ?: recordings.firstOrNull { it.isTranscribed || it.transcriptFullText.isNotBlank() }
            ?: recordings.firstOrNull()
    }

    androidx.compose.runtime.LaunchedEffect(activeDoc?.id) {
        activeDoc?.let {
            viewModel.selectRecordingForView(it.id)
        }
    }

    var conflictTargetItem by remember { mutableStateOf<RecordingEntity?>(null) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var renamingItem by remember { mutableStateOf<RecordingEntity?>(null) }
    var newTitleText by remember { mutableStateOf("") }

    var showAccountDialog by remember { mutableStateOf(false) }
    var customAccountEmail by remember { mutableStateOf("") }
    var customAccountName by remember { mutableStateOf("") }

    var deletingItem by remember { mutableStateOf<RecordingEntity?>(null) }
    var photoTargetItem by remember { mutableStateOf<RecordingEntity?>(null) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }

    // Gallery Picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        photoTargetItem?.let { target ->
            uri?.let {
                viewModel.updateThumbnail(target.id, it.toString())
            }
        }
    }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        photoTargetItem?.let { target ->
            bitmap?.let { bmp ->
                try {
                    val file = File(context.filesDir, "thumb_${target.id}_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(file).use { out ->
                        bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    viewModel.updateThumbnail(target.id, Uri.fromFile(file).toString())
                } catch (_: Exception) {}
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("documents_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Döküman Yönetimi",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Google Drive yedekleme, PDF dökümanlar ve transkripsiyonlar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Kural 3: Dosyalar sekmesi görünüm modları
        TabRow(
            selectedTabIndex = documentsSubTab,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .testTag("documents_mode_tabs")
        ) {
            Tab(
                selected = documentsSubTab == 0,
                onClick = { documentsSubTab = 0 },
                text = {
                    Text(
                        text = "📝 Metin & Ses Oynatıcı",
                        fontWeight = if (documentsSubTab == 0) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                }
            )
            Tab(
                selected = documentsSubTab == 1,
                onClick = { documentsSubTab = 1 },
                text = {
                    Text(
                        text = "☁️ Google Drive & Liste (${recordings.size})",
                        fontWeight = if (documentsSubTab == 1) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (recordings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Henüz oluşturulmuş döküman bulunmuyor",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Kayıt sekmesinden yeni ses alıp transkripsiyondan döküman üretebilirsiniz.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else if (documentsSubTab == 0) {
            // Kural 3: "Dosyalar segmesinde Metin üstte görünsün ve burdan metin içinde düzenleme yapılabilsin,
            // metnin en altında ses kaydı dinlemek için buton ve sesi istenilen süreye çekmek için sürükleme barı ve yönetim tuşları olsun.
            // Her ses durağı yeni metin başlangıcı olsun, metin içinde bu noktalarda butonlar olsun ve oralara dokununca ses kaydında o alana gidilebilsin."
            if (activeDoc == null) {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Lütfen incelenecek dökümanı seçiniz.")
                }
            } else {
                // Aktif döküman seçici başlık barı
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = activeDoc.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box {
                                    IconButton(
                                        onClick = { isDocSelectorExpanded = true },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isDocSelectorExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                            contentDescription = "Döküman Seç",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = isDocSelectorExpanded,
                                        onDismissRequest = { isDocSelectorExpanded = false }
                                    ) {
                                        recordings.forEach { doc ->
                                            DropdownMenuItem(
                                                text = { Text(doc.title, fontWeight = if (doc.id == activeDoc.id) FontWeight.Bold else FontWeight.Normal) },
                                                onClick = {
                                                    activeViewingDocId = doc.id
                                                    isDocSelectorExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                text = "${activeDoc.speakerCount} Konuşmacı • ${formatTimestamp(activeDoc.durationMs)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilledTonalButton(
                                onClick = { viewModel.exportAndSharePdf(context, activeDoc) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Paylaş", fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Üst Alan: Scroll edilebilir ve düzenlenebilir metin blokları (Her ses durağı / segment yeni metin başlangıcı)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (segments.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Bu dökümana ait transkript metni henüz ayrıştırılmamış.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { viewModel.runTranscription(activeDoc) }
                                    ) {
                                        Text("Transkripsiyonu Başlat")
                                    }
                                }
                            }
                        }
                    } else {
                        items(segments, key = { it.id }) { seg ->
                            val isPlayingThisSeg = isPlaying && currentPlayingPath == activeDoc.filePath && (currentPositionMs in seg.startMs..seg.endMs)

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isPlayingThisSeg) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                    else MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (isPlayingThisSeg) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                        else Modifier
                                    )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // Ses durağı / Konuşmacı atlama butonu: Dokununca ses kaydında o noktaya gider
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Button(
                                            onClick = {
                                                if (!isPlaying || currentPlayingPath != activeDoc.filePath) {
                                                    playerManager.playOrPause(activeDoc.filePath)
                                                }
                                                playerManager.seekTo(seg.startMs)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isPlayingThisSeg) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = if (isPlayingThisSeg) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                                            ),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.testTag("jump_to_segment_${seg.id}")
                                        ) {
                                            Icon(
                                                imageVector = if (isPlayingThisSeg) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = "Bu Durağa Git",
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${formatTimestamp(seg.startMs)} • ${seg.speakerLabel}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Text(
                                            text = "${formatTimestamp(seg.startMs)} - ${formatTimestamp(seg.endMs)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Metin içinde doğrudan düzenleme yapılabilen metin alanı
                                    OutlinedTextField(
                                        value = seg.text,
                                        onValueChange = { newText ->
                                            viewModel.updateSegmentText(seg.id, activeDoc.id, newText)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("segment_text_field_${seg.id}"),
                                        textStyle = MaterialTheme.typography.bodyMedium,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Alt Alan: "metnin en altında ses kaydı dinlemek için buton ve sesi istenilen süreye çekmek için sürükleme barı ve yönetim tuşları olsun."
                val effectiveTotalDur = if (durationMs > 0) durationMs else activeDoc.durationMs.coerceAtLeast(1000L)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bottom_audio_player_bar"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${formatTimestamp(currentPositionMs)} / ${formatTimestamp(effectiveTotalDur)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${if (playbackSpeed == 1f) "1" else playbackSpeed.toString()}x Hız",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Sürükleme Barı (Seekbar Slider)
                        Slider(
                            value = currentPositionMs.coerceIn(0L, effectiveTotalDur).toFloat(),
                            onValueChange = { targetPos ->
                                playerManager.seekTo(targetPos.toLong())
                            },
                            valueRange = 0f..effectiveTotalDur.toFloat(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .testTag("audio_player_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        // Yönetim Tuşları (Geri 5s, Oynat/Durdur, İleri 5s, Hız Değiştir)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Geri 5 saniye
                            IconButton(
                                onClick = {
                                    val newPos = (currentPositionMs - 5000L).coerceAtLeast(0L)
                                    playerManager.seekTo(newPos)
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.FastRewind, contentDescription = "Geri 5 sn", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            // Oynat / Duraklat Butonu
                            Button(
                                onClick = {
                                    playerManager.playOrPause(activeDoc.filePath)
                                },
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(52.dp)
                                    .testTag("player_play_pause_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = if (isPlaying && currentPlayingPath == activeDoc.filePath) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Oynat/Durdur",
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // İleri 5 saniye
                            IconButton(
                                onClick = {
                                    val newPos = (currentPositionMs + 5000L).coerceAtMost(effectiveTotalDur)
                                    playerManager.seekTo(newPos)
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.FastForward, contentDescription = "İleri 5 sn", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            // Hız Değiştirme Döngüsü
                            FilledTonalButton(
                                onClick = {
                                    val nextSpeed = when (playbackSpeed) {
                                        1.0f -> 1.25f
                                        1.25f -> 1.5f
                                        1.5f -> 2.0f
                                        2.0f -> 0.75f
                                        else -> 1.0f
                                    }
                                    playerManager.setPlaybackSpeed(nextSpeed)
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${playbackSpeed}x", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            // Tab 1: Google Drive Yedekleme & Döküman Listesi
            // Google Drive Dedicated Backup Control Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("drive_backup_panel"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = "Google Drive",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Google Drive Yedekleme",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Hedef Klasör: ${GoogleDriveBackupManager.DEDICATED_FOLDER_NAME}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Select all / deselect toggle
                        TextButton(
                            onClick = {
                                selectedIds = if (selectedIds.size == recordings.size) {
                                    emptySet()
                                } else {
                                    recordings.map { it.id }.toSet()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (selectedIds.size == recordings.size) Icons.Default.Deselect else Icons.Default.SelectAll,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (selectedIds.size == recordings.size) "Temizle" else "Tümünü Seç",
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // User Google Account Info Banner
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "Yedeklenecek Google Hesabı:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = userProfile.email.ifBlank { "kullanici@gmail.com" },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            TextButton(
                                onClick = {
                                    customAccountEmail = userProfile.email
                                    customAccountName = userProfile.name
                                    showAccountDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Değiştir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedIds.isEmpty()) {
                                "Yedeklenecek dökümanları seçin (${recordings.size} adet hazır)"
                            } else {
                                "${selectedIds.size} döküman seçildi"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = {
                                val targets = if (selectedIds.isEmpty()) {
                                    recordings
                                } else {
                                    recordings.filter { it.id in selectedIds }
                                }

                                // Kural 11: Drive yedekleme öncesi aynı isimde dosya kontrolü
                                val conflict = targets.firstOrNull { t ->
                                    val safeUserFolder = (userProfile.email.ifBlank { "kullanici@gmail.com" }).replace(Regex("[^a-zA-Z0-9_-]"), "_")
                                    val backupDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "GoogleDrive/$safeUserFolder/${GoogleDriveBackupManager.DEDICATED_FOLDER_NAME}")
                                    val cleanName = t.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                                    File(backupDir, "${cleanName}.pdf").exists()
                                }

                                if (conflict != null) {
                                    conflictTargetItem = conflict
                                } else {
                                    viewModel.backupSelectedRecordingsToDrive(context, targets)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("backup_to_drive_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val backupLabel = if (selectedIds.isEmpty()) {
                                Localization.get("drive_backup_all", language)
                            } else {
                                "${Localization.get("drive_backup_selected", language)} (${selectedIds.size})"
                            }
                            AdaptiveButtonText(
                                text = backupLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Documents List with Selection
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(recordings, key = { it.id }) { item ->
                    val isSelected = item.id in selectedIds
                    DocumentCard(
                        recording = item,
                        isSelected = isSelected,
                        onToggleSelect = {
                            selectedIds = if (isSelected) {
                                selectedIds - item.id
                            } else {
                                selectedIds + item.id
                            }
                        },
                        onRename = {
                            renamingItem = item
                            newTitleText = item.title
                        },
                        onSetImage = {
                            photoTargetItem = item
                            showPhotoSourceDialog = true
                        },
                        onSavePdf = { viewModel.savePdfToStorage(context, item) },
                        onShare = { viewModel.exportAndSharePdf(context, item) },
                        onBackupDrive = {
                            val safeUserFolder = (userProfile.email.ifBlank { "kullanici@gmail.com" }).replace(Regex("[^a-zA-Z0-9_-]"), "_")
                            val backupDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "GoogleDrive/$safeUserFolder/${GoogleDriveBackupManager.DEDICATED_FOLDER_NAME}")
                            val cleanName = item.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                            if (File(backupDir, "${cleanName}.pdf").exists()) {
                                conflictTargetItem = item
                            } else {
                                viewModel.backupRecordingToDrive(context, item)
                            }
                        },
                        onDelete = { deletingItem = item }
                    )
                }
            }
        }
    // Kural 11: Drive yedekleme esnasında aynı isimde dosya var ise uyarı verip soran diyalog
    if (conflictTargetItem != null) {
        val target = conflictTargetItem!!
        AlertDialog(
            onDismissRequest = { conflictTargetItem = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Dosya Adı Çakışması")
                }
            },
            text = {
                Text(
                    text = "Drive alanında '${target.title}' isimli döküman zaten mevcut.\n\nMevcut dosyanın üzerine mi yazılsın yoksa yeni bir isimle mi yedeklensin?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toBackup = target
                        conflictTargetItem = null
                        viewModel.backupSelectedRecordingsToDrive(context, listOf(toBackup))
                    }
                ) {
                    Text("Üzerine Yaz")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        val toBackup = target
                        conflictTargetItem = null
                        val newTitle = "${toBackup.title} (Yedek ${System.currentTimeMillis() % 1000})"
                        viewModel.renameRecording(toBackup.id, newTitle)
                        viewModel.backupSelectedRecordingsToDrive(context, listOf(toBackup.copy(title = newTitle)))
                    }
                ) {
                    Text("İsmi Değiştir")
                }
            }
        )
    }

    // Automated Google Drive Upload Progress Dialog
    if (driveBackupState.status != DriveBackupStatus.IDLE) {
        val isFinished = driveBackupState.status == DriveBackupStatus.SUCCESS || driveBackupState.status == DriveBackupStatus.ERROR

        AlertDialog(
            onDismissRequest = {
                if (isFinished) {
                    viewModel.resetDriveBackupState()
                }
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (driveBackupState.status == DriveBackupStatus.SUCCESS) {
                                Icons.Default.CheckCircle
                            } else {
                                Icons.Default.CloudUpload
                            },
                            contentDescription = null,
                            tint = if (driveBackupState.status == DriveBackupStatus.SUCCESS) {
                                Color(0xFF10B981)
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (driveBackupState.status == DriveBackupStatus.SUCCESS) {
                                "Drive Yedeği Tamamlandı!"
                            } else if (driveBackupState.status == DriveBackupStatus.ERROR) {
                                "Yedekleme Hatası"
                            } else {
                                "Google Drive'a Yedekleniyor..."
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // Kural 11: Pencerenin en sağ üstüne " X " şeklinde kapatmak için buton
                    IconButton(
                        onClick = { viewModel.resetDriveBackupState() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat (X)",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Dedicated Folder Card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Hedef: ${driveBackupState.folderName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            val displayEmail = if (driveBackupState.userEmail.isNotBlank()) driveBackupState.userEmail else userProfile.email
                            if (displayEmail.isNotBlank()) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "👤 Google Hesabı: $displayEmail",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Yedekler doğrudan bu kullanıcının Google Drive alanına aktarılır.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { driveBackupState.progressFloat },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = if (driveBackupState.status == DriveBackupStatus.SUCCESS) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "%${driveBackupState.progressPercent}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${driveBackupState.completedItems} / ${driveBackupState.totalItems} döküman",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Status Message
                    Text(
                        text = driveBackupState.statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Uploaded files summary list
                    if (driveBackupState.uploadedFiles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Yüklenen Dökümanlar:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            driveBackupState.uploadedFiles.takeLast(4).forEach { name ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Kural 11: Yükleme işlemi bittikten sonra çıkan pencerede Kapat butonu diğer butonlar ile karışmaması için en alt ortaya eklendi
                    if (isFinished) {
                        Spacer(modifier = Modifier.height(18.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = { viewModel.resetDriveBackupState() },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .testTag("drive_backup_close_centered_btn")
                            ) {
                                Text("Kapat", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (driveBackupState.status == DriveBackupStatus.SUCCESS) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                val targets = if (selectedIds.isEmpty()) recordings else recordings.filter { it.id in selectedIds }
                                viewModel.shareBackupToDriveApp(context, targets)
                            }
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            AdaptiveButtonText(Localization.get("drive_save_now", language))
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.openGoogleDrive(context)
                            }
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            AdaptiveButtonText(Localization.get("drive_open_browser", language))
                        }
                    }
                }
            },
            dismissButton = null
        )
    }

    // Custom Google Account Selection Dialog
    if (showAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kendi Google Hesabını Tanımla")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Her kullanıcı döküman ve transkripsiyonlarını kendi Google hesabının Drive kısmına yedekler. Kullanmak istediğiniz Google e-posta adresini giriniz:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = customAccountEmail,
                        onValueChange = { customAccountEmail = it },
                        label = { Text("Google E-posta Adresi") },
                        placeholder = { Text("ornek@gmail.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customAccountName,
                        onValueChange = { customAccountName = it },
                        label = { Text("Ad Soyad (İsteğe Bağlı)") },
                        placeholder = { Text("Ahmet Yılmaz") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customAccountEmail.isNotBlank()) {
                            viewModel.updateGoogleAccount(customAccountEmail, customAccountName)
                            showAccountDialog = false
                            Toast.makeText(context, "Yedekleme hesabı '$customAccountEmail' olarak güncellendi", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Kaydet & Kullan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccountDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    // Rename Dialog
    if (renamingItem != null) {
        AlertDialog(
            onDismissRequest = { renamingItem = null },
            title = { Text("Döküman Adını Değiştir") },
            text = {
                OutlinedTextField(
                    value = newTitleText,
                    onValueChange = { newTitleText = it },
                    label = { Text("Yeni Başlık / Dosya Adı") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        renamingItem?.let {
                            if (newTitleText.isNotBlank()) {
                                viewModel.renameRecording(it.id, newTitleText.trim())
                            }
                        }
                        renamingItem = null
                    }
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingItem = null }) {
                    Text("İptal")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (deletingItem != null) {
        AlertDialog(
            onDismissRequest = { deletingItem = null },
            title = { Text("Dökümanı Sil") },
            text = { Text("\"${deletingItem?.title}\" dökümanı ve ilişkili ses kaydı cihazdan tamamen silinecektir. Onaylıyor musunuz?") },
            confirmButton = {
                Button(
                    onClick = {
                        deletingItem?.let { viewModel.deleteRecording(it) }
                        deletingItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingItem = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    // Choose Photo Source Dialog (Gallery or Camera)
    if (showPhotoSourceDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoSourceDialog = false },
            title = { Text("Önizleme Resmi Seçin") },
            text = {
                Column {
                    Text("Dökümana önizleme görseli eklemek için kaynak seçiniz:")
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            showPhotoSourceDialog = false
                            cameraLauncher.launch(null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kamera ile Fotoğraf Çek")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FilledTonalButton(
                        onClick = {
                            showPhotoSourceDialog = false
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Dahili/Harici Hafızadan (Galeri) Seç")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPhotoSourceDialog = false }) {
                    Text("Kapat")
                }
            }
        )
    }
}
}

@Composable
fun DocumentCard(
    recording: RecordingEntity,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onRename: () -> Unit,
    onSetImage: () -> Unit,
    onSavePdf: () -> Unit,
    onShare: () -> Unit,
    onBackupDrive: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("document_card_${recording.id}")
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox for selection
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("checkbox_${recording.id}")
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Thumbnail Image or Default Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onSetImage() },
                    contentAlignment = Alignment.Center
                ) {
                    if (recording.thumbnailUri != null) {
                        AsyncImage(
                            model = recording.thumbnailUri,
                            contentDescription = "Önizleme Resmi",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Resim Ekle",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Resim Ekle",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = recording.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onToggleSelect() }
                        )
                        IconButton(onClick = onRename) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "İsmi Değiştir",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(recording.timestamp))
                    Text(
                        text = "$dateStr • ${if (recording.isTranscribed) "Metin Hazır" else "Sadece Ses"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (recording.isBackedUpToDrive) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Google Drive'a Yedeklendi",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row (PDF Save, WhatsApp Share, Drive Backup, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // PDF Save
                FilledTonalButton(
                    onClick = onSavePdf,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF Kaydet", fontSize = 12.sp)
                }

                // WhatsApp / Social Share
                Button(
                    onClick = onShare,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Paylaş", fontSize = 12.sp)
                }

                // Drive Backup for single item
                OutlinedButton(
                    onClick = onBackupDrive,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Drive", fontSize = 12.sp)
                }

                // Delete
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Sil",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0L)
    val mins = totalSec / 60
    val secs = totalSec % 60
    return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
}
