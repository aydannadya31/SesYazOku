package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TranscriptSegmentEntity

/**
 * Konuşmacı ("Kişi 1") adı veya konuşma metni içindeki bir alanı değiştirmeyi
 * sağlayan iki sekmeli gelişmiş düzenleme diyaloğu.
 *
 * Toplu Değiştirme: Sadece seçili konuşmacı adı ("Kişi 1") değiştirilir,
 * diğer isimler ("Kişi 2", "Kişi 3" vb.) kesinlikle etkilenmez.
 */
@Composable
fun SpeakerAndTextEditDialog(
    segment: TranscriptSegmentEntity,
    recordingId: Long,
    onDismiss: () -> Unit,
    onUpdateExactSpeakerName: (oldName: String, newName: String) -> Unit,
    onUpdateSingleSpeaker: (segmentId: Long, newName: String) -> Unit,
    onUpdateSegmentText: (segmentId: Long, newText: String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Kişi Adı, 1: Metin Alanı

    // Kişi adı düzenleme state
    var newSpeakerInput by remember { mutableStateOf(segment.speakerLabel) }

    // Metin alanı düzenleme state
    var editedText by remember { mutableStateOf(segment.text) }
    var findWord by remember { mutableStateOf("") }
    var replaceWord by remember { mutableStateOf("") }
    var showFindReplace by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Düzenle: ${segment.speakerLabel}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Kapat", modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // İki Seçenek Sekmesi (Kişi Adı vs Metin Alanı)
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Kişi Adı", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Metin Alanı", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedTab == 0) {
                    // SEÇENEK 1: KİŞİ ADINI DEĞİŞTİR (TOPLU VEYA TEKİL)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Mevcut Konuşmacı: \"${segment.speakerLabel}\"",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Aşağıya yeni ismi yazıp tüm metinde veya sadece bu konuşmada değiştirmeyi seçebilirsiniz.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newSpeakerInput,
                        onValueChange = { newSpeakerInput = it },
                        label = { Text("Yeni Konuşmacı Adı (Örn: Ahmet)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_speaker_name_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. Toplu Değiştirme Butonu (SADECE bu isimdekileri değiştirir, diğerleri etkilenmez)
                    Button(
                        onClick = {
                            val trimmed = newSpeakerInput.trim()
                            if (trimmed.isNotBlank()) {
                                onUpdateExactSpeakerName(segment.speakerLabel, trimmed)
                                Toast.makeText(
                                    context,
                                    "Tüm metindeki \"${segment.speakerLabel}\" adları \"$trimmed\" olarak güncellendi. Diğer isimler korunmuştur.",
                                    Toast.LENGTH_LONG
                                ).show()
                                onDismiss()
                            }
                        },
                        enabled = newSpeakerInput.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_bulk_rename_speaker_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tüm Metinde '${segment.speakerLabel}' Adını Değiştir", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "⚡ Toplu değiştirme: Sadece \"${segment.speakerLabel}\" olanlar değişir, diğer konuşmacılar (Kişi 2, Kişi 3...) kesinlikle değişmez.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Sadece Bu Segmenti Değiştir Butonu
                    OutlinedButton(
                        onClick = {
                            val trimmed = newSpeakerInput.trim()
                            if (trimmed.isNotBlank()) {
                                onUpdateSingleSpeaker(segment.id, trimmed)
                                Toast.makeText(
                                    context,
                                    "Yalnızca bu konuşmanın etiketi \"$trimmed\" yapıldı.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onDismiss()
                            }
                        },
                        enabled = newSpeakerInput.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_single_rename_speaker_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sadece Bu Konuşmada Adı Değiştir", fontSize = 12.sp)
                    }

                } else {
                    // SEÇENEK 2: METİN ALANINI / İÇERİĞİNİ DEĞİŞTİR
                    Text(
                        text = "Konuşma Metnini Düzenleyin:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = editedText,
                        onValueChange = { editedText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .testTag("dialog_segment_text_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Hızlı Bul & Değiştir aracı
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFindReplace = !showFindReplace }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FindReplace, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (showFindReplace) "Metin İçi Kelime Değiştirmeyi Gizle" else "Metin İçi Kelime Değiştir (Kelime / Cümle)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (showFindReplace) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = findWord,
                                onValueChange = { findWord = it },
                                label = { Text("Aranan Kelime", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = replaceWord,
                                onValueChange = { replaceWord = it },
                                label = { Text("Yeni Kelime", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                if (findWord.isNotBlank() && editedText.contains(findWord)) {
                                    editedText = editedText.replace(findWord, replaceWord)
                                    Toast.makeText(context, "\"$findWord\" kelimesi \"$replaceWord\" ile değiştirildi.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Aranan kelime metinde bulunamadı.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Kelimeyi Metin İçinde Değiştir", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            onUpdateSegmentText(segment.id, editedText.trim())
                            Toast.makeText(context, "Metin alanı başarıyla güncellendi.", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_save_segment_text_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Metin Değişikliğini Kaydet", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}
