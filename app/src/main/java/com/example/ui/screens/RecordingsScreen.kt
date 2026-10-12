package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.AppStrings
import com.example.ui.AppViewModel
import com.example.ui.components.CallCaptureDialog

/**
 * Kayıtlar Sekmesi:
 * Sadece 3 temel işlem butonunu barındırır:
 * 1. Görüşme Yakalama (Zoom, Meet, WhatsApp vb. canlı aramalar)
 * 2. Ses Dosyası Yükleme (Cihazdan mp3, wav ses kaydı içe aktarma)
 * 3. Canlı Kayıt (Mikrofon ile anlık kayıt ve konuşmacı ayrıştırma)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsScreen(
    viewModel: AppViewModel,
    onNavigateToLiveRecord: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isCallCaptureActive by viewModel.isCallCaptureActive.collectAsState()
    val activeCallAppName by viewModel.activeCallAppName.collectAsState()
    val recordingDurationMs by viewModel.audioRecorder.recordingDurationMs.collectAsState()

    var showCallCaptureDialog by remember { mutableStateOf(false) }

    // Audio file picker contract for Ses Dosyası Yükleme
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = it.lastPathSegment ?: "ses_kaydi_${System.currentTimeMillis()}.mp3"
            viewModel.importAudioFile(it, fileName)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = AppStrings.get("tab_records", appLanguage),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Ses kaydı başlatın veya yükleyin",
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Aktif Görüşme Yakalama Durum Paneli (Eğer görüşme sürüyorsa)
            AnimatedVisibility(visible = isCallCaptureActive) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("active_call_capture_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                            )
                            Text(
                                text = "$activeCallAppName Görüşmesi Yakalanıyor...",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }

                        val sec = (recordingDurationMs / 1000) % 60
                        val min = (recordingDurationMs / 1000) / 60
                        Text(
                            text = "Kayıt Süresi: ${String.format("%02d:%02d", min, sec)} | Konuşmacılar ayrıştırılıyor",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                        )

                        Button(
                            onClick = { viewModel.stopCallCapture() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("stop_call_capture_btn")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Görüşmeyi Bitir ve Metne Dönüştür", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 1. CANLI KAYIT BUTONU / KARTI
            RecordingActionCard(
                title = AppStrings.get("btn_live_record", appLanguage),
                description = AppStrings.get("btn_live_record_desc", appLanguage),
                badgeText = "Mikrofon & Ayrıştırma",
                icon = Icons.Default.Mic,
                gradient = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF4338CA),
                        Color(0xFF6366F1)
                    )
                ),
                onClick = onNavigateToLiveRecord,
                testTag = "btn_live_record_action"
            )

            // 2. GÖRÜŞME YAKALAMA BUTONU / KARTI
            RecordingActionCard(
                title = AppStrings.get("btn_call_capture", appLanguage),
                description = AppStrings.get("btn_call_capture_desc", appLanguage),
                badgeText = "Zoom • Meet • WhatsApp",
                icon = Icons.Default.VideoCall,
                gradient = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0D9488),
                        Color(0xFF14B8A6)
                    )
                ),
                onClick = {
                    if (isCallCaptureActive) {
                        viewModel.stopCallCapture()
                    } else {
                        showCallCaptureDialog = true
                    }
                },
                testTag = "btn_call_capture_action"
            )

            // 3. SES DOSYASI YÜKLEME BUTONU / KARTI
            RecordingActionCard(
                title = AppStrings.get("btn_import_audio", appLanguage),
                description = AppStrings.get("btn_import_audio_desc", appLanguage),
                badgeText = "MP3 • M4A • WAV",
                icon = Icons.Default.AudioFile,
                gradient = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF7C3AED),
                        Color(0xFF8B5CF6)
                    )
                ),
                onClick = { audioPickerLauncher.launch("audio/*") },
                testTag = "btn_import_audio_action"
            )
        }
    }

    if (showCallCaptureDialog) {
        CallCaptureDialog(
            onDismiss = { showCallCaptureDialog = false },
            onStartCapture = { appName, myLabel, otherLabel ->
                showCallCaptureDialog = false
                viewModel.startCallCapture(appName, myLabel, otherLabel)
            }
        )
    }
}

@Composable
private fun RecordingActionCard(
    title: String,
    description: String,
    badgeText: String,
    icon: ImageVector,
    gradient: Brush,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Gradient Icon Box
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(gradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Tag / Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action button inside card
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
