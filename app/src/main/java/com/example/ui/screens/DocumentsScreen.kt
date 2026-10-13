package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.export.PdfManager
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

    var renamingItem by remember { mutableStateOf<RecordingEntity?>(null) }
    var newTitleText by remember { mutableStateOf("") }

    var deletingItem by remember { mutableStateOf<RecordingEntity?>(null) }
    var photoTargetItem by remember { mutableStateOf<RecordingEntity?>(null) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }

    // Gallery Picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        photoTargetItem?.let { target ->
            uri?.let {
                // Copy to app storage or store uri
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
        Text(
            text = "Döküman Yönetimi",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "İsim değiştirme, önizleme resmi, PDF kaydetme, WhatsApp paylaşımı ve Drive yedeği",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

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
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(recordings, key = { it.id }) { item ->
                    DocumentCard(
                        recording = item,
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
                        onBackupDrive = { viewModel.backupRecordingToDrive(context, item) },
                        onDelete = { deletingItem = item }
                    )
                }
            }
        }
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

@Composable
fun DocumentCard(
    recording: RecordingEntity,
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
            .testTag("document_card_${recording.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail Image or Default Icon
                Box(
                    modifier = Modifier
                        .size(68.dp)
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
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Resim Ekle",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = recording.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
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
                        Text(
                            text = "✓ Google Drive'a Yedeklendi",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold
                        )
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

                // Drive Backup
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
