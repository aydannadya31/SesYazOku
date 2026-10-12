package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.i18n.AppStrings
import com.example.ui.AppViewModel
import com.example.ui.screens.NoteDetailScreen
import com.example.ui.screens.RecordingScreen
import com.example.ui.screens.RecordingsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TranscriptsListScreen
import com.example.ui.theme.SesYaziciTheme

enum class ScreenTab {
    RECORDINGS,
    TRANSCRIPTS,
    SETTINGS
}

enum class NavigationDestination {
    MAIN_TABS,
    RECORDING,
    NOTE_DETAIL
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appViewModel: AppViewModel = viewModel()
            val themeMode by appViewModel.themeMode.collectAsState()
            val isDarkTheme = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            SesYaziciTheme(darkTheme = isDarkTheme) {
                MainAppHost(viewModel = appViewModel)
            }
        }
    }
}

@Composable
fun MainAppHost(viewModel: AppViewModel) {
    var currentTab by remember { mutableStateOf(ScreenTab.RECORDINGS) }
    var destination by remember { mutableStateOf(NavigationDestination.MAIN_TABS) }
    val appLanguage by viewModel.appLanguage.collectAsState()

    // Handle back stack navigation
    BackHandler(enabled = destination != NavigationDestination.MAIN_TABS) {
        if (destination == NavigationDestination.NOTE_DETAIL) {
            viewModel.closeNote()
            destination = NavigationDestination.MAIN_TABS
        } else if (destination == NavigationDestination.RECORDING) {
            destination = NavigationDestination.MAIN_TABS
        }
    }

    when (destination) {
        NavigationDestination.RECORDING -> {
            RecordingScreen(
                viewModel = viewModel,
                onNavigateBack = { destination = NavigationDestination.MAIN_TABS }
            )
        }
        NavigationDestination.NOTE_DETAIL -> {
            NoteDetailScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    viewModel.closeNote()
                    destination = NavigationDestination.MAIN_TABS
                }
            )
        }
        NavigationDestination.MAIN_TABS -> {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentTab == ScreenTab.RECORDINGS,
                            onClick = { currentTab = ScreenTab.RECORDINGS },
                            icon = { Icon(Icons.Default.GraphicEq, contentDescription = null) },
                            label = { Text(AppStrings.get("tab_records", appLanguage)) },
                            modifier = Modifier.testTag("nav_recordings")
                        )
                        NavigationBarItem(
                            selected = currentTab == ScreenTab.TRANSCRIPTS,
                            onClick = { currentTab = ScreenTab.TRANSCRIPTS },
                            icon = { Icon(Icons.Default.Description, contentDescription = null) },
                            label = { Text(AppStrings.get("tab_transcripts", appLanguage)) },
                            modifier = Modifier.testTag("nav_transcripts")
                        )
                        NavigationBarItem(
                            selected = currentTab == ScreenTab.SETTINGS,
                            onClick = { currentTab = ScreenTab.SETTINGS },
                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            label = { Text(AppStrings.get("tab_settings", appLanguage)) },
                            modifier = Modifier.testTag("nav_settings")
                        )
                    }
                }
            ) { padding ->
                val contentModifier = Modifier.padding(padding)
                when (currentTab) {
                    ScreenTab.RECORDINGS -> {
                        RecordingsScreen(
                            viewModel = viewModel,
                            onNavigateToLiveRecord = { destination = NavigationDestination.RECORDING },
                            modifier = contentModifier
                        )
                    }
                    ScreenTab.TRANSCRIPTS -> {
                        TranscriptsListScreen(
                            viewModel = viewModel,
                            onNoteClick = { noteId ->
                                viewModel.openNote(noteId)
                                destination = NavigationDestination.NOTE_DETAIL
                            },
                            onNavigateToRecord = { currentTab = ScreenTab.RECORDINGS },
                            modifier = contentModifier
                        )
                    }
                    ScreenTab.SETTINGS -> {
                        SettingsScreen(
                            viewModel = viewModel,
                            modifier = contentModifier
                        )
                    }
                }
            }
        }
    }
}
