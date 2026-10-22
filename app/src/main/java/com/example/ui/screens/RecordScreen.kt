package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.FloatingRecordingService
import com.example.ui.MainViewModel
import com.example.ui.util.AdaptiveButtonText
import com.example.ui.util.Localization
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    viewModel: MainViewModel,
    onNavigateToTranscripts: () -> Unit,
    onNavigateToFileManager: () -> Unit = {}
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

    val playerManager = viewModel.playerManager
    val playbackSpeed by playerManager.playbackSpeed.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val currentPlayingPath by playerManager.currentPlayingPath.collectAsState()
    val currentPositionMs by playerManager.currentPositionMs.collectAsState()
    val durationMs by playerManager.durationMs.collectAsState()
    val recordings by viewModel.recordings.collectAsState()
    val language by viewModel.language.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importAudioFile(it)
            onNavigateToTranscripts()
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val ok = viewModel.startLiveRecording()
            if (!ok) {
                Toast.makeText(context, "Kayıt başlatılamadı. Mikrofon hazır değil.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Ses kaydı başlatmak için mikrofon izni gereklidir.", Toast.LENGTH_LONG).show()
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Localization.get("rec_title", language),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = Localization.get("rec_subtitle", language),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = onNavigateToFileManager,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("nav_to_file_manager_from_record")
            ) {
                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(Localization.get("nav_documents", language), fontSize = 12.sp)
            }
        }

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
                        !isRecording -> Localization.get("rec_status_idle", language)
                        isPaused -> Localization.get("rec_status_paused", language)
                        isMicMuted -> Localization.get("rec_status_muted", language)
                        else -> Localization.get("rec_status_recording", language)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Kural: Canlı ses kayıt dalgasına (waveform) akıcı ve estetik gradyan animasyonu
                val animatedAmp by animateFloatAsState(
                    targetValue = if (isRecording && !isPaused && !isMicMuted) amplitude.coerceIn(0.08f, 1f) else 0.04f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "waveform_amplitude"
                )

                val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
                val phase by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = (2 * Math.PI).toFloat(),
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "wave_phase"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val inactiveOutline = MaterialTheme.colorScheme.outlineVariant
                    val inactiveOutlineFaded = inactiveOutline.copy(alpha = 0.5f)

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("waveform_canvas")
                    ) {
                        val barCount = 28
                        val totalSpacing = size.width * 0.35f
                        val barSpacing = totalSpacing / (barCount - 1)
                        val barWidth = ((size.width - totalSpacing) / barCount).coerceAtLeast(3f)

                        val midY = size.height / 2f
                        val maxBarHeight = size.height * 0.92f

                        val gradientBrush = Brush.verticalGradient(
                            colors = if (isRecording && !isPaused && !isMicMuted) {
                                listOf(
                                    Color(0xFF06B6D4), // Cyan
                                    Color(0xFF3B82F6), // Blue
                                    Color(0xFF8B5CF6)  // Purple / Violet
                                )
                            } else {
                                listOf(
                                    inactiveOutline,
                                    inactiveOutlineFaded
                                )
                            },
                            startY = 0f,
                            endY = size.height
                        )

                        for (i in 0 until barCount) {
                            val x = i * (barWidth + barSpacing)
                            val waveMultiplier = kotlin.math.sin(phase + i * 0.38f).toFloat() * 0.28f + 0.72f
                            val normalizedHeight = (animatedAmp * waveMultiplier * maxBarHeight).coerceIn(6f, maxBarHeight)

                            val top = midY - (normalizedHeight / 2f)

                            drawRoundRect(
                                brush = gradientBrush,
                                topLeft = Offset(x, top),
                                size = Size(barWidth, normalizedHeight),
                                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                            )
                        }
                    }
                }

                val liveSpokenText by recorderManager.liveSpokenText.collectAsState()
                if (isRecording && liveSpokenText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Canlı Algılanan Ses:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = liveSpokenText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
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
                        Button(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    val ok = viewModel.startLiveRecording()
                                    if (!ok) {
                                        Toast.makeText(context, "Kayıt başlatılamadı. Mikrofon hazır değil.", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
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

        // Playback Speed & Fast Meeting Listening Card (0.5x, 1x, 1.5x, 2x)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("playback_speed_card"),
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Oynatma Hızı",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Oynatma Hızı & Hızlı Dinleme",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Uzun toplantı ve transkript kayıtları için",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Active Speed Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${if (playbackSpeed == 1.0f) "1" else playbackSpeed.toString()}x Hız",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Ses oynatma hızını seçiniz (Uzun toplantıları 1.5x veya 2x ile daha hızlı tamamlayın):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Playback speed selector chips (0.5x, 1x, 1.5x, 2x)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        0.5f to "0.5x",
                        1.0f to "1.0x",
                        1.5f to "1.5x",
                        2.0f to "2.0x"
                    ).forEach { (speed, label) ->
                        FilterChip(
                            selected = playbackSpeed == speed,
                            onClick = { playerManager.setPlaybackSpeed(speed) },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("speed_chip_${speed}x")
                        )
                    }
                }

                // If any recording is active / playing or if recordings exist
                val latestRecording = recordings.firstOrNull { it.filePath == currentPlayingPath } ?: recordings.firstOrNull()
                if (latestRecording != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isPlaying && currentPlayingPath == latestRecording.filePath) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            playerManager.playOrPause(latestRecording.filePath)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying && currentPlayingPath == latestRecording.filePath) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Oynat / Durdur",
                                            tint = if (isPlaying && currentPlayingPath == latestRecording.filePath) Color.White else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = latestRecording.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    val isCurrentPlaying = currentPlayingPath == latestRecording.filePath
                                    val currentSec = if (isCurrentPlaying) currentPositionMs / 1000 else 0L
                                    val totalSec = if (isCurrentPlaying && durationMs > 0) durationMs / 1000 else latestRecording.durationMs / 1000
                                    val progressTime = String.format(
                                        Locale.getDefault(),
                                        "%02d:%02d / %02d:%02d (%s Hız)",
                                        currentSec / 60, currentSec % 60,
                                        totalSec / 60, totalSec % 60,
                                        if (playbackSpeed == 1.0f) "1x" else "${playbackSpeed}x"
                                    )
                                    Text(
                                        text = progressTime,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                TextButton(
                                    onClick = onNavigateToTranscripts,
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("Transkriptor", fontSize = 11.sp)
                                }
                            }

                            if (isPlaying && currentPlayingPath == latestRecording.filePath && durationMs > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                )
                            }
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
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                return@FilledTonalButton
                            }
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
                        AdaptiveButtonText(text = if (language == "en") "Start App/Call Recording" else "Uygulama/Görüşme Kaydını Başlat")
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
                            AdaptiveButtonText(text = if (language == "en") "Stop & Save" else "Durdur ve Kaydet")
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
                    AdaptiveButtonText(text = if (language == "en") "Choose Audio File from Device" else "Cihazdan Ses Dosyası Seç")
                }
            }
        }
    }
}
