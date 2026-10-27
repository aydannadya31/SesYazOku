package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.screens.FileManagerScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.util.Localization
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.components.PermissionsDialog
import com.example.ui.components.SplashScreen
import com.example.ui.screens.DocumentsScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.FeedbackScreen
import com.example.ui.screens.RecordScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TranscriptsScreen
import com.example.ui.theme.SesYaziciTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val justUpdated = intent?.getBooleanExtra("EXTRA_APP_JUST_UPDATED", false) ?: false
        if (justUpdated) {
            android.widget.Toast.makeText(
                this,
                "Scribe Craft Global başarıyla güncellendi ve yeniden başlatıldı! (v${BuildConfig.VERSION_NAME})",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }

        setContent {
            val mainViewModel: MainViewModel = viewModel()
            val themeMode by mainViewModel.themeMode.collectAsState()

            val isDark = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            SesYaziciTheme(darkTheme = isDark) {
                AppRootContent(viewModel = mainViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRootContent(viewModel: MainViewModel) {
    var showSplash by remember { mutableStateOf(true) }

    val prefs = androidx.compose.ui.platform.LocalContext.current.getSharedPreferences("sesyazoku_prefs", Context.MODE_PRIVATE)
    var permissionsHandled by remember {
        mutableStateOf(prefs.getBoolean("permissions_prompted", false))
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val authProfile by viewModel.authManager.userProfile.collectAsState()
    val updateState by viewModel.updateUiState.collectAsState()
    val language by viewModel.language.collectAsState()

    // 1. Kural: Uygulamayı kullanıcı her açtığında güncel sürüm kontrolü yap
    LaunchedEffect(Unit) {
        viewModel.updateManager.checkForUpdates()
    }

    if (showSplash) {
        SplashScreen(
            onSplashFinished = { showSplash = false }
        )
        return
    }

    if (!authProfile.isSignedIn) {
        com.example.ui.components.GoogleSignInScreen(
            authManager = viewModel.authManager,
            onSignInSuccess = {
                // User signed in successfully
            }
        )
        return
    }

    if (!permissionsHandled) {
        PermissionsDialog(
            onPermissionsGranted = {
                prefs.edit().putBoolean("permissions_prompted", true).apply()
                permissionsHandled = true
            }
        )
    }

    BackHandler(enabled = selectedTabIndex != 0) {
        selectedTabIndex = 0
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_app_scaffold"),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    icon = { Icon(Icons.Default.Mic, contentDescription = Localization.get("nav_record", language)) },
                    label = { Text(Localization.get("nav_record", language)) },
                    modifier = Modifier.testTag("nav_record")
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    icon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = Localization.get("nav_transcripts", language)) },
                    label = { Text(Localization.get("nav_transcripts", language)) },
                    modifier = Modifier.testTag("nav_transcripts")
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    icon = { Icon(Icons.Default.Edit, contentDescription = Localization.get("nav_editor", language)) },
                    label = { Text(Localization.get("nav_editor", language)) },
                    modifier = Modifier.testTag("nav_editor")
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 3,
                    onClick = { selectedTabIndex = 3 },
                    icon = { Icon(Icons.Default.Folder, contentDescription = Localization.get("nav_documents", language)) },
                    label = { Text(Localization.get("nav_documents", language)) },
                    modifier = Modifier.testTag("nav_documents")
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 4,
                    onClick = { selectedTabIndex = 4 },
                    icon = { Icon(Icons.Default.Feedback, contentDescription = Localization.get("nav_feedback", language)) },
                    label = { Text(Localization.get("nav_feedback", language)) },
                    modifier = Modifier.testTag("nav_feedback")
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 5,
                    onClick = { selectedTabIndex = 5 },
                    icon = {
                        Box {
                            Icon(Icons.Default.Settings, contentDescription = Localization.get("nav_settings", language))
                            // 1. Kural: Güncelleme var ise Ayarlar simgesinin sol üst köşesinde minik turuncu bir nokta
                            if (updateState.isUpdateAvailable) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .align(Alignment.TopStart)
                                        .offset(x = (-3).dp, y = (-2).dp)
                                        .background(Color(0xFFF59E0B), CircleShape)
                                )
                            }
                        }
                    },
                    label = { Text(Localization.get("nav_settings", language)) },
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        var filesSubTabIndex by remember { mutableIntStateOf(0) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTabIndex) {
                0 -> RecordScreen(
                    viewModel = viewModel,
                    onNavigateToTranscripts = { selectedTabIndex = 1 },
                    onNavigateToFileManager = {
                        filesSubTabIndex = 0
                        selectedTabIndex = 3
                    }
                )
                1 -> TranscriptsScreen(
                    viewModel = viewModel,
                    onNavigateToDocuments = {
                        filesSubTabIndex = 1
                        selectedTabIndex = 3
                    },
                    onNavigateToEditor = { selectedTabIndex = 2 }
                )
                2 -> EditorScreen(
                    viewModel = viewModel,
                    onNavigateToDocuments = {
                        filesSubTabIndex = 1
                        selectedTabIndex = 3
                    }
                )
                3 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TabRow(
                            selectedTabIndex = filesSubTabIndex,
                            modifier = Modifier.fillMaxWidth().testTag("files_tab_row")
                        ) {
                            Tab(
                                selected = filesSubTabIndex == 0,
                                onClick = { filesSubTabIndex = 0 },
                                text = {
                                    Text(
                                        text = "📁 Dosya Yöneticisi",
                                        fontWeight = if (filesSubTabIndex == 0) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.testTag("tab_file_manager")
                            )
                            Tab(
                                selected = filesSubTabIndex == 1,
                                onClick = { filesSubTabIndex = 1 },
                                text = {
                                    Text(
                                        text = "📄 PDF & Drive",
                                        fontWeight = if (filesSubTabIndex == 1) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.testTag("tab_pdf_drive")
                            )
                        }
                        when (filesSubTabIndex) {
                            0 -> FileManagerScreen(
                                viewModel = viewModel,
                                onNavigateToTranscripts = { recId ->
                                    viewModel.selectRecordingForView(recId)
                                    selectedTabIndex = 1
                                },
                                onNavigateToDocuments = {
                                    filesSubTabIndex = 1
                                }
                            )
                            1 -> DocumentsScreen(viewModel = viewModel)
                        }
                    }
                }
                4 -> FeedbackScreen(viewModel = viewModel)
                5 -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
