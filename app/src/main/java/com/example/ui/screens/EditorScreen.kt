package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.PaddingValues
import com.example.ui.components.SpeakerAndTextEditDialog
import com.example.ui.util.AdaptiveButtonText
import com.example.ui.util.Localization
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
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
fun EditorScreen(
    viewModel: MainViewModel,
    onNavigateToDocuments: () -> Unit
) {
    val context = LocalContext.current
    val recordings by viewModel.recordings.collectAsState()
    val selectedId by viewModel.selectedRecordingId.collectAsState()
    val segments by viewModel.selectedSegments.collectAsState()

    // Sadece transkripsiyonu olan kayıtlar
    val transcribedRecordings = remember(recordings) {
        recordings.filter { it.isTranscribed || it.transcriptFullText.isNotBlank() }
    }

    // Aktif seçili kayıt
    val currentRecording = remember(transcribedRecordings, selectedId) {
        transcribedRecordings.firstOrNull { it.id == selectedId }
            ?: transcribedRecordings.firstOrNull()
    }

    LaunchedEffect(currentRecording) {
        currentRecording?.let {
            if (selectedId != it.id) {
                viewModel.selectRecordingForView(it.id)
            }
        }
    }

    // Toplu ve tekil kişi / metin düzenleme durumları
    var bulkEditingSpeakerLabel by remember { mutableStateOf<String?>(null) }
    var newBulkLabelInput by remember { mutableStateOf("") }
    var activeEditingSegment by remember { mutableStateOf<TranscriptSegmentEntity?>(null) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val language by viewModel.language.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("editor_screen")
    ) {
        // Üst Başlık & Açıklama
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Metin & Etiket Düzenleyici",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Konuşmacı etiketlerini tek seferde özelleştirin",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (transcribedRecordings.isEmpty()) {
            // Henüz transkripsiyonu yapılmış kayıt yok
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Düzenlenecek Transkript Bulunamadı",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Kayıt Merkezi veya Transkriptor sekmesinden ses kaydınızı metne dönüştürdükten sonra buraya gelerek kişi etiketlerini düzenleyebilirsiniz.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        } else {
            // Kayıt Seçim Açılır Menüsü (Kural 7)
            Text(
                text = Localization.get("editor_select_doc", language),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    onClick = { isDropdownExpanded = !isDropdownExpanded },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_document_dropdown_trigger")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = currentRecording?.title ?: "Döküman Seçin",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                currentRecording?.let {
                                    val durMin = (it.durationMs / 1000) / 60
                                    val durSec = (it.durationMs / 1000) % 60
                                    Text(
                                        text = "Süre: ${String.format(Locale.getDefault(), "%02d:%02d", durMin, durSec)} • ${it.speakerCount} Konuşmacı",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Icon(
                            imageVector = if (isDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Açılır Menü",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    transcribedRecordings.forEach { rec ->
                        val isSelected = currentRecording?.id == rec.id
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = rec.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    val durMin = (rec.durationMs / 1000) / 60
                                    val durSec = (rec.durationMs / 1000) % 60
                                    Text(
                                        text = "Süre: ${String.format(Locale.getDefault(), "%02d:%02d", durMin, durSec)} • ${rec.speakerCount} Konuşmacı",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Description,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = {
                                viewModel.selectRecordingForView(rec.id)
                                isDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            currentRecording?.let { activeRec ->
                // Seçili Kaydın Konuşmacı Yönetim Kartı
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tespit Edilen Konuşmacılar",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(activeRec.timestamp)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Segmentlerden benzersiz konuşmacı listesini çıkar
                        val uniqueSpeakers = remember(segments) {
                            segments.map { it.speakerLabel.trim() }
                                .filter { it.isNotBlank() }
                                .distinct()
                        }

                        if (uniqueSpeakers.isEmpty()) {
                            Text(
                                text = "Bu kayıtta ayrıştırılmış segment bulunamadı. Genel metin Dökümanlar sekmesinde görüntülenebilir.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                uniqueSpeakers.forEach { spkLabel ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.background,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                bulkEditingSpeakerLabel = spkLabel
                                                newBulkLabelInput = spkLabel
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = spkLabel,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "Toplu Değiştir",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontSize = 10.sp
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Düzenle",
                                                tint = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Döküman Önizleme Başlığı ve Dökümanlara Git Butonu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Döküman İçeriği Önizlemesi:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    TextButton(
                        onClick = onNavigateToDocuments,
                        modifier = Modifier.testTag("go_to_documents_button")
                    ) {
                        Text("Dökümanlara Git", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Döküman Segmentleri ve Konuşmalar Listesi
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (segments.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Text(
                                    text = activeRec.transcriptFullText.ifBlank { "Metin bulunamadı." },
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(14.dp),
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    } else {
                        items(segments, key = { it.id }) { seg ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activeEditingSegment = seg }
                                    .testTag("editor_segment_card_${seg.id}"),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.clickable { activeEditingSegment = seg }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = seg.speakerLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Metin içindeki "Kişi 1" yazan yerdeki değiştirme butonu
                                        OutlinedButton(
                                            onClick = { activeEditingSegment = seg },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier
                                                .height(28.dp)
                                                .testTag("edit_segment_button_${seg.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Kişi veya Metin Değiştir",
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Değiştir (Kişi / Metin)", fontSize = 10.sp)
                                        }

                                        Spacer(modifier = Modifier.weight(1f))

                                        IconButton(
                                            onClick = { activeEditingSegment = seg },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Düzenle",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = seg.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 1. Toplu Kişi Etiketi Değiştirme Diyaloğu (Sadece seçili etiketi değiştirir, diğerlerini etkilemez)
    bulkEditingSpeakerLabel?.let { oldLabel ->
        AlertDialog(
            onDismissRequest = { bulkEditingSpeakerLabel = null },
            title = {
                Text(
                    text = "Toplu Kişi İsmi Değiştir",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Bu dökümandaki yalnızca \"$oldLabel\" etiketleri yeni belirteceğiniz isimle güncellenecektir. Diğer konuşmacı isimleri (Kişi 2, Kişi 3...) kesinlikle değiştirilmez.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = newBulkLabelInput,
                        onValueChange = { newBulkLabelInput = it },
                        label = { Text("Yeni Konuşmacı Adı (Örn: Ahmet Hoca)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bulk_speaker_label_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newBulkLabelInput.trim()
                        if (trimmed.isNotBlank() && currentRecording != null) {
                            viewModel.updateSpeakerLabelByExactName(
                                recordingId = currentRecording.id,
                                oldLabel = oldLabel,
                                newLabel = trimmed
                            )
                            Toast.makeText(
                                context,
                                "Tüm \"$oldLabel\" isimleri \"$trimmed\" olarak değiştirildi. Diğer isimler korundu.",
                                Toast.LENGTH_LONG
                            ).show()
                            bulkEditingSpeakerLabel = null
                        }
                    },
                    enabled = newBulkLabelInput.isNotBlank(),
                    modifier = Modifier.testTag("confirm_bulk_speaker_label_button")
                ) {
                    Text("Sadece '$oldLabel' İsimlerini Değiştir")
                }
            },
            dismissButton = {
                TextButton(onClick = { bulkEditingSpeakerLabel = null }) {
                    Text("İptal")
                }
            }
        )
    }

    // 2. Metin İçindeki Segmentte Kişi Adı veya Metin Alanını Değiştirme Diyaloğu
    activeEditingSegment?.let { seg ->
        currentRecording?.let { rec ->
            SpeakerAndTextEditDialog(
                segment = seg,
                recordingId = rec.id,
                onDismiss = { activeEditingSegment = null },
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
}
