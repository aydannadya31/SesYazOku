package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.components.PermissionsDialog
import com.example.ui.components.SplashScreen
import com.example.ui.screens.DocumentsScreen
import com.example.ui.screens.FeedbackScreen
import com.example.ui.screens.RecordScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TranscriptsScreen
import com.example.ui.theme.SesYaziciTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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

    if (showSplash) {
        SplashScreen(
            onSplashFinished = { showSplash = false }
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
                    icon = { Icon(Icons.Default.Mic, contentDescription = "Kayıt") },
                    label = { Text("Kayıt") },
                    modifier = Modifier.testTag("nav_record")
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    icon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = "Transkriptor") },
                    label = { Text("Transkriptor") },
                    modifier = Modifier.testTag("nav_transcripts")
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    icon = { Icon(Icons.Default.Description, contentDescription = "Döküman") },
                    label = { Text("Döküman") },
                    modifier = Modifier.testTag("nav_documents")
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 3,
                    onClick = { selectedTabIndex = 3 },
                    icon = { Icon(Icons.Default.Feedback, contentDescription = "Üreticiye Mesaj") },
                    label = { Text("Mesaj") },
                    modifier = Modifier.testTag("nav_feedback")
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 4,
                    onClick = { selectedTabIndex = 4 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ayarlar") },
                    label = { Text("Ayarlar") },
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTabIndex) {
                0 -> RecordScreen(
                    viewModel = viewModel,
                    onNavigateToTranscripts = { selectedTabIndex = 1 }
                )
                1 -> TranscriptsScreen(
                    viewModel = viewModel,
                    onNavigateToDocuments = { selectedTabIndex = 2 }
                )
                2 -> DocumentsScreen(viewModel = viewModel)
                3 -> FeedbackScreen(viewModel = viewModel)
                4 -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
