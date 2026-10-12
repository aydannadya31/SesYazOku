package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.data.model.TranscriptionNote
import com.example.export.DocumentExportManager
import com.example.i18n.AppStrings
import com.example.ui.AppViewModel
import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Metinler Sekmesi:
 * Sesten metne dönüştürülen tüm kayıtları görüntüler.
 * Her metin için konuşmacı etiketleri gösterilir.
 * Cihaz içine PDF kaydetme ve WhatsApp vb. üzerinden PDF paylaşma desteği sunar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptsListScreen(
    viewModel: AppViewModel,
    onNoteClick: (Long) -> Unit,
    onNavigateToRecord: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notes by viewModel.filteredNotes.collectAsState()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()

    var savedPdfFile by remember { mutableStateOf<File?>(null) }
    var showSavedDialog by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<TranscriptionNote?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = AppStrings.get("tab_transcripts", appLanguage),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Sesten metne dönüştürülen kayıtlar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Arama Çubuğu
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("search_transcripts_input"),
                placeholder = { Text(AppStrings.get("search_hint", appLanguage)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Text("✕", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            // Filtre Çipleri
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = !showFavoritesOnly,
                        onClick = { viewModel.setShowFavoritesOnly(false) },
                        label = { Text("Tüm Metinler (${notes.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = showFavoritesOnly,
                        onClick = { viewModel.setShowFavoritesOnly(!showFavoritesOnly) },
                        label = { Text(AppStrings.get("favorites", appLanguage)) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (showFavoritesOnly) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (showFavoritesOnly) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Metinler Listesi
            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Text(
                            text = AppStrings.get("empty_notes", appLanguage),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 22.sp
                        )

                        Button(
                            onClick = onNavigateToRecord,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("empty_go_to_record_btn")
                        ) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Kayıtlar Sekmesine Git", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(notes, key = { it.id }) { note ->
                        TranscriptCard(
                            note = note,
                            onClick = { onNoteClick(note.id) },
                            onToggleFavorite = { viewModel.toggleFavorite(note) },
                            onDelete = { noteToDelete = note },
                            onSavePdf = {
                                viewModel.saveNotePdfToDevice(note) { file ->
                                    if (file != null) {
                                        savedPdfFile = file
                                        showSavedDialog = true
                                    } else {
                                        Toast.makeText(context, "PDF kaydedilemedi", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onSharePdf = {
                                viewModel.shareNotePdf(note)
                            }
                        )
                    }
                }
            }
        }
    }

    // PDF Kaydedildi Bilgi Dialogu
    if (showSavedDialog && savedPdfFile != null) {
        val file = savedPdfFile!!
        AlertDialog(
            onDismissRequest = { showSavedDialog = false },
            icon = {
                Icon(
                    Icons.Default.FileDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text("PDF Cihaza Kaydedildi", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Metin başarıyla PDF formatında cihazınıza kaydedildi.",
                        style = MaterialTheme.typography.bodyMedium
                    )
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
                        showSavedDialog = false
                        DocumentExportManager.openPdfFile(context, file)
                    }
                ) {
                    Text("PDF'i Aç", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavedDialog = false }) {
                    Text("Kapat")
                }
            }
        )
    }

    // Silme Onay Dialogu
    if (noteToDelete != null) {
        val target = noteToDelete!!
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Metni Sil") },
            text = { Text("\"${target.title}\" başlıklı transkript kalıcı olarak silinecek. Emin misiniz?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteNote(target.id)
                        noteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sil", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

@Composable
private fun TranscriptCard(
    note: TranscriptionNote,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onSavePdf: () -> Unit,
    onSharePdf: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("tr", "TR")) }
    val dateStr = remember(note.createdAt) { dateFormat.format(Date(note.createdAt)) }
    val durationMin = remember(note.durationMs) { (note.durationMs / 1000) / 60 }
    val durationSec = remember(note.durationMs) { (note.durationMs / 1000) % 60 }

    // Parse tags for display
    val tagsList = remember(note.tagsJson) {
        try {
            val json = JSONArray(note.tagsJson)
            val list = mutableListOf<String>()
            for (i in 0 until json.length()) {
                list.add(json.getString(i))
            }
            list
        } catch (e: Exception) {
            emptyList<String>()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("transcript_card_${note.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Başlık & Favori / Sil Satırı
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (note.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favori",
                            tint = if (note.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Sil",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Tarih ve Süre
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${String.format("%02d:%02d", durationMin, durationSec)} süre",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Konuşmacı ve Etiket Çipleri
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Konuşmacı Ayrıştırma Rozeti
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF6366F1).copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = Color(0xFF4338CA)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Konuşmacılar Ayrıştırıldı",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF4338CA)
                        )
                    }
                }

                // Etiketler (varsa)
                tagsList.take(2).forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Metin / Özet Önizleme
            if (!note.summary.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = note.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // PDF KAYDET VE WHATSAPP PAYLAŞ BUTONLARI (Kullanıcının talep ettiği ana özellikler)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // PDF Cihaza Kaydet Butonu
                OutlinedButton(
                    onClick = onSavePdf,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_pdf_btn_${note.id}"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PDF Kaydet",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // PDF Paylaş Butonu (WhatsApp vb.)
                Button(
                    onClick = onSharePdf,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("share_pdf_btn_${note.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981), // WhatsApp yeşili tonu
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PDF Paylaş",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
