package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import com.example.ui.util.AdaptiveButtonText
import com.example.ui.util.Localization
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FeedbackEntity
import com.example.ui.MainViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class FeedbackAttachmentItem(
    val id: String = UUID.randomUUID().toString(),
    val type: String, // "AUDIO", "IMAGE", "PDF"
    val uriString: String,
    val name: String
)

private fun resolveFileName(context: Context, uri: Uri, fallbackPrefix: String): String {
    var result: String? = null
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                result = cursor.getString(nameIndex)
            }
        }
    } catch (_: Exception) {}
    return result ?: uri.lastPathSegment ?: "$fallbackPrefix (${System.currentTimeMillis() % 10000})"
}

@Composable
fun FeedbackScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val feedbacks by viewModel.feedbacks.collectAsState()
    val authProfile by viewModel.authManager.userProfile.collectAsState()
    val language by viewModel.language.collectAsState()

    var subject by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("İşleyiş Geri Bildirimi") }
    var message by remember { mutableStateOf("") }

    // Kural 9: Destek ekibi uyarısı alanı
    val prefs = remember { context.getSharedPreferences("sesyazoku_prefs", Context.MODE_PRIVATE) }
    var activeSupportAlert by remember {
        mutableStateOf(prefs.getString("active_support_alert", "Teknik Destek Ekibi: Sistem bağlantınız aktif. Yaşadığınız sorunları veya taleplerinizi buradan iletebilirsiniz.") ?: "")
    }
    var isAlertDismissed by remember { mutableStateOf(false) }

    // Çoklu ek listesi (Birden fazla ses, fotoğraf veya PDF eklenebilir)
    var attachments by remember { mutableStateOf<List<FeedbackAttachmentItem>>(emptyList()) }
    var feedbackToDelete by remember { mutableStateOf<FeedbackEntity?>(null) }

    // Çoklu dosya seçiciler
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val newItems = uris.mapIndexed { idx, uri ->
                val name = resolveFileName(context, uri, "Ses Kaydı ${attachments.count { it.type == "AUDIO" } + idx + 1}")
                FeedbackAttachmentItem(type = "AUDIO", uriString = uri.toString(), name = name)
            }
            attachments = attachments + newItems
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val newItems = uris.mapIndexed { idx, uri ->
                val name = resolveFileName(context, uri, "Fotoğraf ${attachments.count { it.type == "IMAGE" } + idx + 1}")
                FeedbackAttachmentItem(type = "IMAGE", uriString = uri.toString(), name = name)
            }
            attachments = attachments + newItems
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val newItems = uris.mapIndexed { idx, uri ->
                val name = resolveFileName(context, uri, "Döküman ${attachments.count { it.type == "PDF" } + idx + 1}.pdf")
                FeedbackAttachmentItem(type = "PDF", uriString = uri.toString(), name = name)
            }
            attachments = attachments + newItems
        }
    }

    val categories = listOf("İşleyiş Geri Bildirimi", "Eksik Kısımlar", "Hata Bildirimi", "Yeni Özellik")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("feedback_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = Localization.get("feedback_title", language),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Görüş, öneri veya teknik sorunlarınızı web yönetim paneline iletin",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Kural 9: Destek Uyarısı İletildiğinde Görüntülenecek Mesaj Bildirim Alanı
        if (activeSupportAlert.isNotBlank() && !isAlertDismissed) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("support_alert_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = Localization.get("feedback_support_alert", language),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            IconButton(
                                onClick = {
                                    isAlertDismissed = true
                                    prefs.edit().remove("active_support_alert").apply()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Kapat",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = activeSupportAlert,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        // Gönderen Kullanıcı Kimlik Kartı
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = authProfile.name.ifBlank { "Anonim Kullanıcı" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = authProfile.email.ifBlank { "kullanici@sesyazoku.com" },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        // Mesaj Formu
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Yeni Mesaj Oluştur",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Kategori Seçici
                    Text(
                        text = "Kategori:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.take(2).forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.drop(2).forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Konu Başlığı") },
                        placeholder = { Text("Örn: Ses tanıma iyileştirme önerisi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Mesajınız ve detaylar...") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Ek Ekleme Bölümü (Ses kaydı, Fotoğraf, PDF - birden fazla eklenebilir)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dosya / Belge Ekle (Birden Fazla Seçilebilir):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (attachments.isNotEmpty()) {
                            Text(
                                text = "${attachments.size} Dosya Seçildi",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { audioPickerLauncher.launch("audio/*") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ses Ekle", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Fotoğraf", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { pdfPickerLauncher.launch("application/pdf") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF Ekle", fontSize = 11.sp)
                        }
                    }

                    // Eklenen dosyaların dinamik listesi ve silme butonları
                    if (attachments.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            attachments.forEach { att ->
                                val (icon, tint) = when (att.type) {
                                    "AUDIO" -> Icons.Default.Audiotrack to MaterialTheme.colorScheme.primary
                                    "IMAGE" -> Icons.Default.Image to Color(0xFF10B981)
                                    else -> Icons.Default.PictureAsPdf to Color(0xFFEF4444)
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
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
                                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = tint)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = att.name,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        IconButton(
                                            onClick = { attachments = attachments.filterNot { it.id == att.id } },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Kaldır", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            if (subject.isNotBlank() && message.isNotBlank()) {
                                val audioFirst = attachments.firstOrNull { it.type == "AUDIO" }?.uriString
                                val imageFirst = attachments.firstOrNull { it.type == "IMAGE" }?.uriString
                                val pdfFirst = attachments.firstOrNull { it.type == "PDF" }?.uriString
                                val allAttachmentsJson = JSONArray().apply {
                                    attachments.forEach { att ->
                                        put(JSONObject().apply {
                                            put("type", att.type)
                                            put("uri", att.uriString)
                                            put("name", att.name)
                                        })
                                    }
                                }.toString()

                                viewModel.sendFeedback(
                                    subject = subject.trim(),
                                    category = selectedCategory,
                                    message = message.trim(),
                                    userEmail = authProfile.email,
                                    audioUri = audioFirst,
                                    imageUri = imageFirst,
                                    pdfUri = pdfFirst,
                                    attachmentsJson = allAttachmentsJson
                                ) {
                                    subject = ""
                                    message = ""
                                    attachments = emptyList()
                                }
                            }
                        },
                        enabled = subject.isNotBlank() && message.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("send_feedback_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        AdaptiveButtonText(text = Localization.get("feedback_send", language), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Kullanıcının Gönderdiği Mesaj Geçmişi
        if (feedbacks.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gönderilen Mesaj Geçmişiniz",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${feedbacks.size} Mesaj",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            items(feedbacks, key = { it.id }) { fb ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = fb.subject,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.2f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = fb.status,
                                            fontSize = 11.sp,
                                            color = Color(0xFF10B981),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Kullanıcı kendi ekranından silme butonu (Yönetim panelini etkilemez)
                                IconButton(
                                    onClick = { feedbackToDelete = fb },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Mesajı Ekranımdan Sil",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = fb.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Ekler göstergesi
                        val hasMultipleAttachments = fb.attachmentsJson.isNotBlank() && fb.attachmentsJson != "[]"
                        val (audioCount, imgCount, pdfCount) = remember(fb.attachmentsJson) {
                            var a = 0
                            var img = 0
                            var p = 0
                            if (fb.attachmentsJson.isNotBlank()) {
                                try {
                                    val array = JSONArray(fb.attachmentsJson)
                                    for (i in 0 until array.length()) {
                                        when (array.getJSONObject(i).optString("type")) {
                                            "AUDIO" -> a++
                                            "IMAGE" -> img++
                                            "PDF" -> p++
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                            Triple(a, img, p)
                        }

                        if (hasMultipleAttachments || fb.audioUri != null || fb.imageUri != null || fb.pdfUri != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (hasMultipleAttachments) {
                                    if (audioCount > 0) SmallBadge(text = "🎤 $audioCount Ses", color = MaterialTheme.colorScheme.primary)
                                    if (imgCount > 0) SmallBadge(text = "🖼️ $imgCount Fotoğraf", color = Color(0xFF10B981))
                                    if (pdfCount > 0) SmallBadge(text = "📄 $pdfCount PDF", color = Color(0xFFEF4444))
                                    if (audioCount == 0 && imgCount == 0 && pdfCount == 0) SmallBadge(text = "📎 Ekler", color = MaterialTheme.colorScheme.primary)
                                } else {
                                    if (fb.audioUri != null) SmallBadge(text = "🎤 Ses Eki", color = MaterialTheme.colorScheme.primary)
                                    if (fb.imageUri != null) SmallBadge(text = "🖼️ Fotoğraf", color = Color(0xFF10B981))
                                    if (fb.pdfUri != null) SmallBadge(text = "📄 PDF", color = Color(0xFFEF4444))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(fb.timestamp))
                        Text(
                            text = "${fb.category} • $dateStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }

    // Mesaj silme onay diyaloğu (Yalnızca kullanıcının yerel ekranından siler)
    if (feedbackToDelete != null) {
        AlertDialog(
            onDismissRequest = { feedbackToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mesajı Ekranınızdan Sil")
                }
            },
            text = {
                Text(
                    "Bu mesajı kendi ekranınızdaki geçmişten kaldırmak istediğinize emin misiniz?\n\nNot: Bu işlem yalnızca sizin cihazınızdaki görünümü temizler; üreticiye iletilen kayıt yönetim panelinde korunur.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        feedbackToDelete?.let { viewModel.deleteFeedback(it.id) }
                        feedbackToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Evet, Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = { feedbackToDelete = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

@Composable
private fun SmallBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
