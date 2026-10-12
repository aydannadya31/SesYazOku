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
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CloudSync
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
import com.example.ui.screens.CloudSyncScreen
import com.example.ui.screens.NoteDetailScreen
import com.example.ui.screens.NotesListScreen
import com.example.ui.screens.RecordingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.theme.SesYaziciTheme

enum class ScreenTab {
    NOTES,
    TASKS,
    CLOUD,
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
    var currentTab by remember { mutableStateOf(ScreenTab.NOTES) }
    var destination by remember { mutableStateOf(NavigationDestination.MAIN_TABS) }
    val appLanguage by viewModel.appLanguage.collectAsState()
    val activeNoteId by viewModel.activeNoteId.collectAsState()

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
                            selected = currentTab == ScreenTab.NOTES,
                            onClick = { currentTab = ScreenTab.NOTES },
                            icon = { Icon(Icons.Default.GraphicEq, contentDescription = null) },
                            label = { Text(AppStrings.get("tab_notes", appLanguage)) },
                            modifier = Modifier.testTag("nav_notes")
                        )
                        NavigationBarItem(
                            selected = currentTab == ScreenTab.TASKS,
                            onClick = { currentTab = ScreenTab.TASKS },
                            icon = { Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null) },
                            label = { Text(AppStrings.get("tab_tasks", appLanguage)) },
                            modifier = Modifier.testTag("nav_tasks")
                        )
                        NavigationBarItem(
                            selected = currentTab == ScreenTab.CLOUD,
                            onClick = { currentTab = ScreenTab.CLOUD },
                            icon = { Icon(Icons.Default.CloudSync, contentDescription = null) },
                            label = { Text(AppStrings.get("tab_cloud", appLanguage)) },
                            modifier = Modifier.testTag("nav_cloud")
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
                    ScreenTab.NOTES -> {
                        NotesListScreen(
                            viewModel = viewModel,
                            onNavigateToRecord = { destination = NavigationDestination.RECORDING },
                            onNoteClick = { noteId ->
                                viewModel.openNote(noteId)
                                destination = NavigationDestination.NOTE_DETAIL
                            },
                            modifier = contentModifier
                        )
                    }
                    ScreenTab.TASKS -> {
                        TasksScreen(
                            viewModel = viewModel,
                            modifier = contentModifier
                        )
                    }
                    ScreenTab.CLOUD -> {
                        CloudSyncScreen(
                            viewModel = viewModel,
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
