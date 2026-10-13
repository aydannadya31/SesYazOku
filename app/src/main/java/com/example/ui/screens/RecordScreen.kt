package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.FloatingRecordingService
import com.example.ui.MainViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    viewModel: MainViewModel,
    onNavigateToTranscripts: () -> Unit
) {
    val context = LocalContext.current
    val recorderManager = viewModel.recorderManager
    val isRecording by recorderManager.isRecording.collectAsState()
    val isPaused by recorderManager.isPaused.collectAsState()
    val isMicMuted by recorderManager.isMicMuted.collectAsState()
    val elapsedTimeMs by recorderManager.elapsedTimeMs.collectAsState()
    val amplitude by recorderManager.currentAmplitude.collectAsState()

    val isServiceRunning by FloatingRecordingService.isRunning.collectAsState()
    val serviceDurationSec by FloatingRecordingService.recordedDurationSeconds.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importAudioFile(it)
            onNavigateToTranscripts()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("record_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Text(
            text = "Ses Kayıt Merkezi",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Canlı mikrofon kaydı, diğer uygulamalar ve harici ses dosyaları",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Main Live Recording Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("live_record_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isRecording) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val totalSeconds = elapsedTimeMs / 1000
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                val timerText = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

                // Timer Display
                Text(
                    text = timerText,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = when {
                        !isRecording -> "Kayıt Yapılmıyor"
                        isPaused -> "Kayıt Duraklatıldı"
                        isMicMuted -> "Kayıt Sürüyor (Mikrofon Sessizde)"
                        else -> "Canlı Ses Kaydediliyor..."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Waveform / Amplitude Meter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val barCount = 18
                    for (i in 0 until barCount) {
                        val barHeight = if (isRecording && !isPaused && !isMicMuted) {
                            val factor = (amplitude * (1f + (i % 5) * 0.2f)).coerceIn(0.1f, 1f)
                            (factor * 32.dp.value).dp
                        } else {
                            4.dp
                        }
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .width(6.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Recording Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isRecording) {
                        // Start Record Button
                        Button(
                            onClick = { viewModel.startLiveRecording() },
                            modifier = Modifier
                                .size(72.dp)
                                .testTag("start_record_button"),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Kayıt Başlat",
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    } else {
                        // Mic Mute / Unmute Toggle Button (Requirement: "istenirse cihaz mikrofonu açık veya kapalı yapılabilsin")
                        IconButton(
                            onClick = { recorderManager.toggleMicMute() },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isMicMuted) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                                )
                                .testTag("toggle_mic_button")
                        ) {
                            Icon(
                                imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Mikrofonu Aç/Kapat",
                                tint = if (isMicMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }

                        // Pause / Resume Button
                        IconButton(
                            onClick = {
                                if (isPaused) recorderManager.resumeRecording() else recorderManager.pauseRecording()
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .testTag("pause_resume_button")
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = "Duraklat / Devam Et",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }

                        // Stop and Save Button
                        Button(
                            onClick = {
                                viewModel.stopAndSaveRecording(sourceType = "LIVE", titlePrefix = "Canlı Kayıt")
                                onNavigateToTranscripts()
                            },
                            modifier = Modifier
                                .size(64.dp)
                                .testTag("stop_save_button"),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Kaydı Tamamla",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Requirement 1: External App & Meeting Recording Mode (WhatsApp, Zoom, Meet, Phone Calls)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("app_call_record_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Görüşme ve Uygulama Kaydı",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Zoom, Meet, WhatsApp ve Telefon Konuşmaları",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Bu mod aktif edildiğinde arka plan servisi başlar. SesYazOku arka planda çalışırken diğer uygulamalara geçebilir, gelen veya giden konuşmaları kesintisiz kaydedebilirsiniz.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (!isServiceRunning) {
                    FilledTonalButton(
                        onClick = {
                            val serviceIntent = Intent(context, FloatingRecordingService::class.java).apply {
                                action = FloatingRecordingService.ACTION_START_CALL_RECORDING
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(serviceIntent)
                            } else {
                                context.startService(serviceIntent)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("start_app_record_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Uygulama/Görüşme Kaydını Başlat")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Kayıt Yapılıyor: ${serviceDurationSec / 60}m ${serviceDurationSec % 60}s",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Button(
                            onClick = {
                                val serviceIntent = Intent(context, FloatingRecordingService::class.java).apply {
                                action = FloatingRecordingService.ACTION_STOP_CALL_RECORDING
                            }
                            context.startService(serviceIntent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Durdur ve Kaydet")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Requirement 1: Import Existing Audio Files (MP3, M4A, WAV, OGG, AAC, 3GP...)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("import_audio_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ses Dosyası Yükle",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "MP3, M4A, WAV, OGG, AAC, 3GP vb.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Dahili veya harici depolama biriminizdeki hazır ses kayıtlarını ekleyerek anında metne ve PDF dökümanına dönüştürün.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = {
                        filePickerLauncher.launch("audio/*")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("pick_audio_file_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cihazdan Ses Dosyası Seç")
                }
            }
        }
    }
}
