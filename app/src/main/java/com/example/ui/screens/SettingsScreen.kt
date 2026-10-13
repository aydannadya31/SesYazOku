package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val authProfile by viewModel.authManager.userProfile.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val language by viewModel.language.collectAsState()
    val audioFormat by viewModel.audioFormat.collectAsState()
    val audioBitrate by viewModel.audioBitrate.collectAsState()
    val audioSampleRate by viewModel.audioSampleRate.collectAsState()

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
                                coroutineScope.launch {
                                    viewModel.authManager.signInWithGoogle()
                                }
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
    }
}
