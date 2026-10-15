package com.example.ui.screens

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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.update.UpdateStatus
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val authProfile by viewModel.authManager.userProfile.collectAsState()
    val updateState by viewModel.updateUiState.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val language by viewModel.language.collectAsState()
    val audioFormat by viewModel.audioFormat.collectAsState()
    val audioBitrate by viewModel.audioBitrate.collectAsState()
    val audioSampleRate by viewModel.audioSampleRate.collectAsState()

    var showChangeAccountDialog by remember { mutableStateOf(false) }
    var editEmail by remember { mutableStateOf("") }
    var editName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Ayarlar",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // 0. Google Account & Cloud Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (authProfile.isSignedIn) authProfile.name else "Google Hesabı",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (authProfile.isSignedIn) authProfile.email else "Drive ve Gmail senkronizasyonu için giriş yapın",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!authProfile.isSignedIn) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                viewModel.authManager.signInWithGoogle()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Google ile Giriş Yap")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = {
                                editEmail = authProfile.email
                                editName = authProfile.name
                                showChangeAccountDialog = true
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Hesap Değiştir")
                        }
                        OutlinedButton(
                            onClick = { viewModel.authManager.signOut() },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Çıkış Yap")
                        }
                    }
                }
            }
        }

        if (showChangeAccountDialog) {
            AlertDialog(
                onDismissRequest = { showChangeAccountDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Google Hesabını Belirle")
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Her kullanıcı dökümanlarını kendi Google hesabının Drive kısmına yedekler. Aktif Google hesabınızı güncelleyin:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = editEmail,
                            onValueChange = { editEmail = it },
                            label = { Text("Google E-posta") },
                            placeholder = { Text("kullanici@gmail.com") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Kullanıcı Adı") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editEmail.isNotBlank()) {
                                viewModel.updateGoogleAccount(editEmail, editName)
                                showChangeAccountDialog = false
                            }
                        }
                    ) {
                        Text("Kaydet")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showChangeAccountDialog = false }) {
                        Text("Vazgeç")
                    }
                }
            )
        }

        // 4. Theme Setting Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Uygulama Teması", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = themeMode == "SYSTEM",
                        onClick = { viewModel.setThemeMode("SYSTEM") },
                        label = { Text("Sistem") }
                    )
                    FilterChip(
                        selected = themeMode == "LIGHT",
                        onClick = { viewModel.setThemeMode("LIGHT") },
                        label = { Text("Açık") }
                    )
                    FilterChip(
                        selected = themeMode == "DARK",
                        onClick = { viewModel.setThemeMode("DARK") },
                        label = { Text("Koyu") }
                    )
                }
            }
        }

        // 4. Language Setting Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Uygulama Dili", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = language == "tr",
                        onClick = { viewModel.setLanguage("tr") },
                        label = { Text("Türkçe") }
                    )
                    FilterChip(
                        selected = language == "en",
                        onClick = { viewModel.setLanguage("en") },
                        label = { Text("English") }
                    )
                    FilterChip(
                        selected = language == "de",
                        onClick = { viewModel.setLanguage("de") },
                        label = { Text("Deutsch") }
                    )
                }
            }
        }

        // 4. Audio Quality Fine Tuning Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Ses Kayıt İnce Ayarları", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Kayıt Formatı:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = audioFormat == "M4A",
                        onClick = { viewModel.setAudioFormat("M4A") },
                        label = { Text("M4A (AAC)") }
                    )
                    FilterChip(
                        selected = audioFormat == "WAV",
                        onClick = { viewModel.setAudioFormat("WAV") },
                        label = { Text("WAV (Kayıpsız)") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Örnekleme Hızı (Sample Rate):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = audioSampleRate == 44100,
                        onClick = { viewModel.setAudioSampleRate(44100) },
                        label = { Text("44.1 kHz") }
                    )
                    FilterChip(
                        selected = audioSampleRate == 48000,
                        onClick = { viewModel.setAudioSampleRate(48000) },
                        label = { Text("48.0 kHz") }
                    )
                    FilterChip(
                        selected = audioSampleRate == 16000,
                        onClick = { viewModel.setAudioSampleRate(16000) },
                        label = { Text("16.0 kHz") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Ses Bitrate:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = audioBitrate == 128000,
                        onClick = { viewModel.setAudioBitrate(128000) },
                        label = { Text("128 kbps") }
                    )
                    FilterChip(
                        selected = audioBitrate == 256000,
                        onClick = { viewModel.setAudioBitrate(256000) },
                        label = { Text("256 kbps") }
                    )
                    FilterChip(
                        selected = audioBitrate == 64000,
                        onClick = { viewModel.setAudioBitrate(64000) },
                        label = { Text("64 kbps") }
                    )
                }
            }
        }

        // 5. Version & In-App Update Card (GitHub & Web Release Sync)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("app_version_update_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (updateState.isUpdateAvailable) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header with App Version
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
                            shape = CircleShape,
                            color = if (updateState.isUpdateAvailable) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (updateState.isUpdateAvailable) Icons.Default.NewReleases else Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Uygulama Sürümü",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Kurulu Sürüm: v${updateState.currentVersionName} (Build ${updateState.currentVersionCode})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (updateState.isUpdateAvailable) {
                            Color(0xFFEF4444).copy(alpha = 0.15f)
                        } else {
                            Color(0xFF10B981).copy(alpha = 0.15f)
                        }
                    ) {
                        Text(
                            text = if (updateState.isUpdateAvailable) "YENİ SÜRÜM!" else "GÜNCEL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (updateState.isUpdateAvailable) Color(0xFFDC2626) else Color(0xFF059669),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // When Update Available Banner
                if (updateState.isUpdateAvailable) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.NewReleases,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Daha güncel sürüm var! (v${updateState.latestVersionName})",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF92400E),
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (updateState.releaseNotes.isNotBlank()) {
                                        updateState.releaseNotes
                                    } else {
                                        "GitHub ve web sitesinde yeni özellikler ve geliştirmeler içeren güncel APK hazır."
                                    },
                                    color = Color(0xFF78350F),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    // Up to date confirmation row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = updateState.statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Downloading Progress Section
                if (updateState.status == UpdateStatus.DOWNLOADING) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = updateState.statusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "%${updateState.downloadProgressPercent}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { updateState.downloadProgressFloat },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (updateState.downloadedSizeFormatted.isNotBlank()) {
                            Text(
                                text = updateState.downloadedSizeFormatted,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (updateState.isUpdateAvailable) {
                        // Prominent Update Button
                        Button(
                            onClick = {
                                viewModel.downloadAndInstallUpdate(context)
                            },
                            enabled = updateState.status != UpdateStatus.DOWNLOADING,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("update_apk_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (updateState.status == UpdateStatus.DOWNLOADING) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("İndiriliyor... (%${updateState.downloadProgressPercent})")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Güncelle (v${updateState.latestVersionName} Otomatik İndir ve Kur)",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Secondary Web Download Button
                        OutlinedButton(
                            onClick = {
                                viewModel.openWebDownloadPage(context)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Web Sayfasından Doğrudan İndir (APK Linki)")
                        }
                    } else {
                        // Check for Updates Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.checkForUpdates()
                                },
                                enabled = updateState.status != UpdateStatus.CHECKING,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("check_updates_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (updateState.status == UpdateStatus.CHECKING) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Denetleniyor...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Güncellemeleri Denetle")
                                }
                            }

                            // Quick test button to verify the update notification and flow
                            OutlinedButton(
                                onClick = {
                                    viewModel.simulateUpdateCheck()
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("⚡ Güncellemeyi Test Et", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
