package com.example.ui.screens

import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FolderEntity
import com.example.data.RecordingEntity
import com.example.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class GroupingMode(val title: String) {
    DATE("Tarihe Göre"),
    PROJECT("Projeye Göre"),
    CATEGORY("Kategoriye Göre")
}

enum class SortMode(val title: String) {
    DATE_DESC("Tarih (En Yeni)"),
    DATE_ASC("Tarih (En Eski)"),
    NAME_ASC("İsim (A-Z)"),
    DURATION_DESC("Süre (En Uzun)"),
    SIZE_DESC("Boyut (En Büyük)")
}

data class FolderGroup(
    val name: String,
    val type: GroupingMode,
    val colorHex: String,
    val recordings: List<RecordingEntity>,
    val customFolderId: Long? = null
) {
    val totalSize: Long get() = recordings.sumOf { it.fileSize }
    val totalDurationMs: Long get() = recordings.sumOf { it.durationMs }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FileManagerScreen(
    viewModel: MainViewModel,
    onNavigateToTranscripts: (Long) -> Unit = {},
    onNavigateToDocuments: () -> Unit = {}
) {
    val context = LocalContext.current
    val recordings by viewModel.recordings.collectAsState()
    val customFolders by viewModel.folders.collectAsState()

    val playerManager = viewModel.playerManager
    val isPlaying by playerManager.isPlaying.collectAsState()
    val playingPath by playerManager.currentPlayingPath.collectAsState()
    val currentPositionMs by playerManager.currentPositionMs.collectAsState()
    val durationMs by playerManager.durationMs.collectAsState()
    val playbackSpeed by playerManager.playbackSpeed.collectAsState()

    var groupingMode by remember { mutableStateOf(GroupingMode.PROJECT) }
    var sortMode by remember { mutableStateOf(SortMode.DATE_DESC) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFolder by remember { mutableStateOf<String?>(null) }

    // Multi-select state inside folder
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedRecordingIds by remember { mutableStateOf(setOf<Long>()) }

    // Dialogs
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("#3B82F6") }

    var renamingFolder by remember { mutableStateOf<FolderGroup?>(null) }
    var renameFolderNameText by remember { mutableStateOf("") }

    var movingRecording by remember { mutableStateOf<RecordingEntity?>(null) }
    var showBatchMoveDialog by remember { mutableStateOf(false) }

    var renamingRecording by remember { mutableStateOf<RecordingEntity?>(null) }
    var renameRecordingTitleText by remember { mutableStateOf("") }

    var deletingRecording by remember { mutableStateOf<RecordingEntity?>(null) }
    var deletingFolder by remember { mutableStateOf<FolderGroup?>(null) }

    // Handle back press inside open folder
    BackHandler(enabled = selectedFolder != null) {
        selectedFolder = null
        isSelectionMode = false
        selectedRecordingIds = emptySet()
    }

    // Prepare Date Group Name Helper
    fun getDateGroupName(timestamp: Long): String {
        val now = Calendar.getInstance()
        val itemCal = Calendar.getInstance().apply { timeInMillis = timestamp }

        val isSameDay = now.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == itemCal.get(Calendar.DAY_OF_YEAR)

        if (isSameDay) return "Bugün"

        val yesterday = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = yesterday.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == itemCal.get(Calendar.DAY_OF_YEAR)

        if (isYesterday) return "Dün"

        val diffDays = (now.timeInMillis - timestamp) / (1000 * 60 * 60 * 24)
        if (diffDays in 2..7) return "Bu Hafta"
        if (diffDays in 8..30) return "Bu Ay"

        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("tr"))
        return monthFormat.format(Date(timestamp))
    }

    // Compute Groups based on current grouping mode
    val groups: List<FolderGroup> = remember(recordings, customFolders, groupingMode, searchQuery, sortMode) {
        val filteredRecordings = recordings.filter {
            if (searchQuery.isBlank()) true
            else it.title.contains(searchQuery, ignoreCase = true) ||
                    it.projectName.contains(searchQuery, ignoreCase = true) ||
                    it.category.contains(searchQuery, ignoreCase = true)
        }

        val sorted = when (sortMode) {
            SortMode.DATE_DESC -> filteredRecordings.sortedByDescending { it.timestamp }
            SortMode.DATE_ASC -> filteredRecordings.sortedBy { it.timestamp }
            SortMode.NAME_ASC -> filteredRecordings.sortedBy { it.title.lowercase() }
            SortMode.DURATION_DESC -> filteredRecordings.sortedByDescending { it.durationMs }
            SortMode.SIZE_DESC -> filteredRecordings.sortedByDescending { it.fileSize }
        }

        when (groupingMode) {
            GroupingMode.DATE -> {
                sorted.groupBy { getDateGroupName(it.timestamp) }
                    .map { (dateName, recs) ->
                        val color = when (dateName) {
                            "Bugün" -> "#10B981"
                            "Dün" -> "#3B82F6"
                            "Bu Hafta" -> "#8B5CF6"
                            "Bu Ay" -> "#F59E0B"
                            else -> "#64748B"
                        }
                        FolderGroup(
                            name = dateName,
                            type = GroupingMode.DATE,
                            colorHex = color,
                            recordings = recs
                        )
                    }
            }
            GroupingMode.PROJECT -> {
                val projectFolders = customFolders.filter { it.type == "PROJECT" }
                val projectMap = sorted.groupBy { it.projectName.ifBlank { "Genel Kayıtlar" } }.toMutableMap()

                // Include empty custom folders as well
                for (cf in projectFolders) {
                    if (!projectMap.containsKey(cf.name)) {
                        projectMap[cf.name] = emptyList()
                    }
                }

                projectMap.map { (projName, recs) ->
                    val custom = projectFolders.find { it.name.equals(projName, ignoreCase = true) }
                    FolderGroup(
                        name = projName,
                        type = GroupingMode.PROJECT,
                        colorHex = custom?.colorHex ?: "#3B82F6",
                        recordings = recs,
                        customFolderId = custom?.id
                    )
                }.sortedByDescending { it.recordings.size }
            }
            GroupingMode.CATEGORY -> {
                val categoryFolders = customFolders.filter { it.type == "CATEGORY" }
                val catMap = sorted.groupBy { it.category.ifBlank { "Genel" } }.toMutableMap()

                for (cf in categoryFolders) {
                    if (!catMap.containsKey(cf.name)) {
                        catMap[cf.name] = emptyList()
                    }
                }

                catMap.map { (catName, recs) ->
                    val custom = categoryFolders.find { it.name.equals(catName, ignoreCase = true) }
                    val defaultColor = when (catName) {
                        "Toplantı" -> "#10B981"
                        "Ders & Konferans" -> "#F59E0B"
                        "Mülakat" -> "#8B5CF6"
                        "Sesli Not" -> "#06B6D4"
                        "Önemli" -> "#EF4444"
                        else -> "#3B82F6"
                    }
                    FolderGroup(
                        name = catName,
                        type = GroupingMode.CATEGORY,
                        colorHex = custom?.colorHex ?: defaultColor,
                        recordings = recs,
                        customFolderId = custom?.id
                    )
                }.sortedByDescending { it.recordings.size }
            }
        }
    }

    // Active folder contents when opened
    val currentOpenGroup = remember(selectedFolder, groups) {
        selectedFolder?.let { folderName ->
            groups.find { it.name == folderName }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("file_manager_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedFolder != null) {
                        IconButton(
                            onClick = {
                                selectedFolder = null
                                isSelectionMode = false
                                selectedRecordingIds = emptySet()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = if (selectedFolder == null) "Dosya Yöneticisi" else selectedFolder!!,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = if (selectedFolder == null) {
                        "Ses kayıtlarınızı tarih, proje veya kategori bazında düzenleyin"
                    } else {
                        "${currentOpenGroup?.recordings?.size ?: 0} ses kaydı • Toplam ${formatDuration(currentOpenGroup?.totalDurationMs ?: 0)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (selectedFolder == null) {
                // Create Folder Button
                Button(
                    onClick = {
                        newFolderName = ""
                        selectedColorHex = "#3B82F6"
                        showCreateFolderDialog = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("create_folder_button")
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (groupingMode == GroupingMode.CATEGORY) "Yeni Kategori" else "Yeni Klasör", fontSize = 12.sp)
                }
            } else {
                // Actions when folder is opened
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            isSelectionMode = !isSelectionMode
                            if (!isSelectionMode) selectedRecordingIds = emptySet()
                        }
                    ) {
                        Text(if (isSelectionMode) "İptal" else "Seç", fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Storage & Recordings Summary Card (When at root)
        if (selectedFolder == null) {
            val totalRecordingsCount = recordings.size
            val totalBytes = recordings.sumOf { it.fileSize }
            val totalDuration = recordings.sumOf { it.durationMs }
            val formattedTotalSize = Formatter.formatFileSize(context, totalBytes)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("storage_summary_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SummaryItem(
                        icon = Icons.Default.Folder,
                        label = "Klasörler",
                        value = "${groups.size} Adet",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    SummaryItem(
                        icon = Icons.Default.Audiotrack,
                        label = "Ses Kaydı",
                        value = "$totalRecordingsCount Kayıt",
                        tint = Color(0xFF10B981)
                    )
                    SummaryItem(
                        icon = Icons.Default.Storage,
                        label = "Disk Alanı",
                        value = formattedTotalSize,
                        tint = Color(0xFF8B5CF6)
                    )
                    SummaryItem(
                        icon = Icons.Default.Speed,
                        label = "Toplam Süre",
                        value = formatDuration(totalDuration),
                        tint = Color(0xFFF59E0B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Grouping Mode Chips: Tarih, Proje, Kategori
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = groupingMode == GroupingMode.PROJECT,
                    onClick = { groupingMode = GroupingMode.PROJECT },
                    label = { Text("📁 Projeye Göre") },
                    leadingIcon = {
                        if (groupingMode == GroupingMode.PROJECT) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    },
                    modifier = Modifier.testTag("group_by_project_chip")
                )
                FilterChip(
                    selected = groupingMode == GroupingMode.DATE,
                    onClick = { groupingMode = GroupingMode.DATE },
                    label = { Text("📅 Tarihe Göre") },
                    leadingIcon = {
                        if (groupingMode == GroupingMode.DATE) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    },
                    modifier = Modifier.testTag("group_by_date_chip")
                )
                FilterChip(
                    selected = groupingMode == GroupingMode.CATEGORY,
                    onClick = { groupingMode = GroupingMode.CATEGORY },
                    label = { Text("🏷️ Kategoriye") },
                    leadingIcon = {
                        if (groupingMode == GroupingMode.CATEGORY) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    },
                    modifier = Modifier.testTag("group_by_category_chip")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Search Bar and Sort Options
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("search_files_field"),
                placeholder = {
                    Text(
                        if (selectedFolder == null) "Klasör veya kayıt ara..." else "Bu klasörde ara...",
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Temizle", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Sort Dropdown Button
            var showSortMenu by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(
                    onClick = { showSortMenu = true },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = "Sırala",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sırala", fontSize = 12.sp)
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    SortMode.values().forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = mode.title,
                                    fontWeight = if (sortMode == mode) FontWeight.Bold else FontWeight.Normal,
                                    color = if (sortMode == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                sortMode = mode
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // VIEW 1: ROOT FOLDER LIST
        if (selectedFolder == null) {
            if (groups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Henüz klasör oluşturulmadı",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Yukarıdaki 'Yeni Klasör' butonuyla kayıtlarınızı gruplayacak klasörler açabilirsiniz.",
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(groups, key = { "${it.type}_${it.name}" }) { group ->
                        FolderCardItem(
                            group = group,
                            context = context,
                            onOpenFolder = { selectedFolder = group.name },
                            onRenameFolder = {
                                renamingFolder = group
                                renameFolderNameText = group.name
                            },
                            onDeleteFolder = {
                                deletingFolder = group
                            }
                        )
                    }
                }
            }
        } else {
            // VIEW 2: INSIDE FOLDER RECORDINGS LIST
            val folderRecordings = currentOpenGroup?.recordings ?: emptyList()

            // Selection action bar when multi-selecting
            AnimatedVisibility(visible = isSelectionMode) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${selectedRecordingIds.size} kayıt seçildi",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Select All / Deselect
                            TextButton(
                                onClick = {
                                    selectedRecordingIds = if (selectedRecordingIds.size == folderRecordings.size) {
                                        emptySet()
                                    } else {
                                        folderRecordings.map { it.id }.toSet()
                                    }
                                }
                            ) {
                                Text(if (selectedRecordingIds.size == folderRecordings.size) "Hiçbiri" else "Tümü")
                            }

                            // Batch Move
                            Button(
                                onClick = { showBatchMoveDialog = true },
                                enabled = selectedRecordingIds.isNotEmpty(),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Taşı", fontSize = 12.sp)
                            }

                            // Batch Delete
                            IconButton(
                                onClick = {
                                    viewModel.deleteRecordingsBatch(selectedRecordingIds)
                                    selectedRecordingIds = emptySet()
                                    isSelectionMode = false
                                },
                                enabled = selectedRecordingIds.isNotEmpty()
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Sil", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            if (folderRecordings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Bu klasörde henüz ses kaydı yok",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Diğer klasörlerdeki kayıtları 'Taşı' seçeneğiyle buraya aktarabilirsiniz.",
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(folderRecordings, key = { it.id }) { rec ->
                        val isPlayingThis = isPlaying && playingPath == rec.filePath
                        val isSelected = selectedRecordingIds.contains(rec.id)

                        FileItemCard(
                            recording = rec,
                            context = context,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            isPlaying = isPlayingThis,
                            currentPosMs = if (isPlayingThis) currentPositionMs else 0L,
                            totalDurationMs = if (isPlayingThis && durationMs > 0) durationMs else rec.durationMs,
                            playbackSpeed = playbackSpeed,
                            onToggleSelect = {
                                selectedRecordingIds = if (isSelected) {
                                    selectedRecordingIds - rec.id
                                } else {
                                    selectedRecordingIds + rec.id
                                }
                            },
                            onPlayPause = {
                                playerManager.playOrPause(rec.filePath)
                            },
                            onSpeedChange = { speed ->
                                playerManager.setPlaybackSpeed(speed)
                            },
                            onMove = { movingRecording = rec },
                            onRename = {
                                renamingRecording = rec
                                renameRecordingTitleText = rec.title
                            },
                            onOpenTranscripts = {
                                onNavigateToTranscripts(rec.id)
                            },
                            onDelete = { deletingRecording = rec }
                        )
                    }
                }
            }
        }
    }

    // DIALOG: Create Folder
    if (showCreateFolderDialog) {
        val colorOptions = listOf("#3B82F6", "#10B981", "#8B5CF6", "#F59E0B", "#EF4444", "#06B6D4", "#EC4899", "#64748B")
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = {
                Text(
                    text = if (groupingMode == GroupingMode.CATEGORY) "Yeni Kategori Klasörü" else "Yeni Proje Klasörü",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Ses kayıtlarını sınıflandırmak için yeni bir klasör adı belirleyin:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("Klasör Adı") },
                        placeholder = { Text("Örn: Yönetim Toplantıları") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Klasör Rengi:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        colorOptions.forEach { hex ->
                            val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Blue }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColorHex = hex }
                                    .then(
                                        if (selectedColorHex == hex) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedColorHex == hex) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val type = if (groupingMode == GroupingMode.CATEGORY) "CATEGORY" else "PROJECT"
                        viewModel.createFolder(
                            name = newFolderName,
                            type = type,
                            colorHex = selectedColorHex
                        )
                        showCreateFolderDialog = false
                    },
                    enabled = newFolderName.isNotBlank()
                ) {
                    Text("Oluştur")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    // DIALOG: Move Single Recording
    movingRecording?.let { rec ->
        val projectFolders = customFolders.filter { it.type == "PROJECT" }
        val categoryFolders = customFolders.filter { it.type == "CATEGORY" }

        var targetProject by remember { mutableStateOf(rec.projectName) }
        var targetCategory by remember { mutableStateOf(rec.category) }

        AlertDialog(
            onDismissRequest = { movingRecording = null },
            title = { Text("Klasöre Taşı", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Seçilen ses kaydı:", style = MaterialTheme.typography.bodySmall)
                    Text(rec.title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Hedef Proje Klasörü:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val allProjects = (listOf("Genel Kayıtlar") + projectFolders.map { it.name }).distinct()
                        allProjects.forEach { proj ->
                            FilterChip(
                                selected = targetProject == proj,
                                onClick = { targetProject = proj },
                                label = { Text(proj, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Kategori:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val allCats = (listOf("Genel", "Toplantı", "Ders / Konferans", "Mülakat", "Sesli Not", "Önemli") + categoryFolders.map { it.name }).distinct()
                        allCats.forEach { cat ->
                            FilterChip(
                                selected = targetCategory == cat,
                                onClick = { targetCategory = cat },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.moveRecordingToFolder(rec.id, targetProject, targetCategory)
                        movingRecording = null
                    }
                ) {
                    Text("Taşı")
                }
            },
            dismissButton = {
                TextButton(onClick = { movingRecording = null }) {
                    Text("İptal")
                }
            }
        )
    }

    // DIALOG: Batch Move
    if (showBatchMoveDialog) {
        val projectFolders = customFolders.filter { it.type == "PROJECT" }
        var targetBatchProject by remember { mutableStateOf("Genel Kayıtlar") }

        AlertDialog(
            onDismissRequest = { showBatchMoveDialog = false },
            title = { Text("${selectedRecordingIds.size} Kaydı Taşı", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Seçilen tüm kayıtlar hangi proje klasörüne taşınsın?")
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val allProjects = (listOf("Genel Kayıtlar") + projectFolders.map { it.name }).distinct()
                        allProjects.forEach { proj ->
                            FilterChip(
                                selected = targetBatchProject == proj,
                                onClick = { targetBatchProject = proj },
                                label = { Text(proj) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.moveRecordingsBatch(selectedRecordingIds, targetBatchProject)
                        selectedRecordingIds = emptySet()
                        isSelectionMode = false
                        showBatchMoveDialog = false
                    }
                ) {
                    Text("Taşı")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchMoveDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    // DIALOG: Rename Recording
    renamingRecording?.let { rec ->
        AlertDialog(
            onDismissRequest = { renamingRecording = null },
            title = { Text("Kaydı Yeniden Adlandır", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameRecordingTitleText,
                    onValueChange = { renameRecordingTitleText = it },
                    label = { Text("Kayıt Başlığı") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameRecordingTitleText.isNotBlank()) {
                            viewModel.updateTitle(rec.id, renameRecordingTitleText.trim())
                            renamingRecording = null
                        }
                    },
                    enabled = renameRecordingTitleText.isNotBlank()
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingRecording = null }) {
                    Text("İptal")
                }
            }
        )
    }

    // DIALOG: Rename Folder
    renamingFolder?.let { group ->
        AlertDialog(
            onDismissRequest = { renamingFolder = null },
            title = { Text("Klasörü Yeniden Adlandır", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameFolderNameText,
                    onValueChange = { renameFolderNameText = it },
                    label = { Text("Yeni Klasör Adı") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameFolderNameText.isNotBlank()) {
                            group.customFolderId?.let { id ->
                                val type = if (group.type == GroupingMode.CATEGORY) "CATEGORY" else "PROJECT"
                                viewModel.renameFolder(id, renameFolderNameText.trim(), group.name, type)
                            }
                            renamingFolder = null
                        }
                    },
                    enabled = renameFolderNameText.isNotBlank()
                ) {
                    Text("Güncelle")
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingFolder = null }) {
                    Text("İptal")
                }
            }
        )
    }

    // DIALOG: Delete Folder
    deletingFolder?.let { group ->
        AlertDialog(
            onDismissRequest = { deletingFolder = null },
            title = { Text("Klasörü Sil", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = {
                Text(
                    text = "'${group.name}' klasörünü silmek istediğinize emin misiniz? Klasör içindeki kayıtlar 'Genel Kayıtlar' klasörüne aktarılacaktır."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        group.customFolderId?.let { id ->
                            viewModel.deleteFolder(id)
                        }
                        // Re-assign recordings to default
                        val ids = group.recordings.map { it.id }.toSet()
                        viewModel.moveRecordingsBatch(ids, "Genel Kayıtlar", "Genel")
                        deletingFolder = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingFolder = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    // DIALOG: Delete Single Recording
    deletingRecording?.let { rec ->
        AlertDialog(
            onDismissRequest = { deletingRecording = null },
            title = { Text("Kaydı Sil", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = {
                Text("'${rec.title}' başlıklı ses kaydını ve transkripsiyonunu silmek istediğinize emin misiniz?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRecording(rec.id)
                        deletingRecording = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Evet, Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingRecording = null }) {
                    Text("İptal")
                }
            }
        )
    }
}

@Composable
fun SummaryItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    tint: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun FolderCardItem(
    group: FolderGroup,
    context: android.content.Context,
    onOpenFolder: () -> Unit,
    onRenameFolder: () -> Unit,
    onDeleteFolder: () -> Unit
) {
    val folderColor = try {
        Color(android.graphics.Color.parseColor(group.colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenFolder() }
            .testTag("folder_card_${group.name.replace(' ', '_')}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = folderColor.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (group.type == GroupingMode.DATE) Icons.Default.CalendarToday else Icons.Default.Folder,
                        contentDescription = null,
                        tint = folderColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = folderColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${group.recordings.size} ses",
                            color = folderColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val formattedSize = Formatter.formatFileSize(context, group.totalSize)
                Text(
                    text = "Toplam süre: ${formatDuration(group.totalDurationMs)} • $formattedSize",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (group.customFolderId != null) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Seçenekler", modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Yeniden Adlandır") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onRenameFolder()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Klasörü Sil", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDeleteFolder()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FileItemCard(
    recording: RecordingEntity,
    context: android.content.Context,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    isPlaying: Boolean,
    currentPosMs: Long,
    totalDurationMs: Long,
    playbackSpeed: Float,
    onToggleSelect: () -> Unit,
    onPlayPause: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onMove: () -> Unit,
    onRename: () -> Unit,
    onOpenTranscripts: () -> Unit,
    onDelete: () -> Unit
) {
    val formattedDate = remember(recording.timestamp) {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr"))
        sdf.format(Date(recording.timestamp))
    }
    val formattedSize = remember(recording.fileSize) {
        Formatter.formatFileSize(context, recording.fileSize)
    }

    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("file_item_${recording.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }

                // Play / Pause Circle Button
                Surface(
                    shape = CircleShape,
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onPlayPause() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Duraklat" else "Oynat",
                            tint = if (isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recording.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Text(
                            text = " • $formattedSize • ${formatDuration(recording.durationMs)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 11.sp
                        )
                    }
                }

                // More Menu Button
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Seçenekler", modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Klasöre / Projeye Taşı") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onMove()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Transkripti Aç") },
                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onOpenTranscripts()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Yeniden Adlandır") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onRename()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sil", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            // Tags row: Category & Transcribed badge
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "📁 ${recording.projectName}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "🏷️ ${recording.category}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (recording.isTranscribed) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFD1FAE5)
                    ) {
                        Text(
                            text = "✓ Transkript Hazır",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF065F46),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Audio Player Bar & Speed Chips when playing
            if (isPlaying) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Oynatılıyor: ${formatDuration(currentPosMs)} / ${formatDuration(totalDurationMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${playbackSpeed}x Hız",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        val progress = if (totalDurationMs > 0) (currentPosMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f) else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        // Speed selector chips: 0.5x, 1x, 1.5x, 2x
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Hız:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { speed ->
                                val isSelectedSpeed = playbackSpeed == speed
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelectedSpeed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .clickable { onSpeedChange(speed) }
                                        .padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${speed}x",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelectedSpeed) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelectedSpeed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
