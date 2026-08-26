package ru.avito.notesandtasks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NoteAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.avito.notesandtasks.core.common.settings.ThemeMode
import ru.avito.notesandtasks.core.navigation.routes.NoteEditor
import ru.avito.notesandtasks.core.navigation.routes.NotesList
import ru.avito.notesandtasks.core.navigation.routes.NotesTab
import ru.avito.notesandtasks.core.navigation.routes.SettingsScreen
import ru.avito.notesandtasks.core.navigation.routes.SettingsTab
import ru.avito.notesandtasks.core.navigation.routes.TasksList
import ru.avito.notesandtasks.core.navigation.routes.TasksTab
import ru.avito.notesandtasks.core.navigation.routes.TopLevelRoute
import ru.avito.notesandtasks.core.ui.theme.NotesAndTasksTheme
import ru.avito.notesandtasks.feature.notes.presentation.editor.NoteEditorRoute
import ru.avito.notesandtasks.feature.notes.presentation.editor.NoteEditorViewModel
import ru.avito.notesandtasks.feature.notes.presentation.list.NotesListRoute
import ru.avito.notesandtasks.feature.notes.presentation.list.NotesListViewModel
import ru.avito.notesandtasks.feature.notes.presentation.navigation.notesFeatureGraph
import ru.avito.notesandtasks.feature.settings.presentation.SettingsRoute
import ru.avito.notesandtasks.feature.settings.presentation.SettingsViewModel
import ru.avito.notesandtasks.feature.settings.presentation.navigation.settingsFeatureGraph
import ru.avito.notesandtasks.feature.tasks.presentation.TasksRoute
import ru.avito.notesandtasks.feature.tasks.presentation.TasksViewModel
import ru.avito.notesandtasks.feature.tasks.presentation.navigation.tasksFeatureGraph

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NotesAndTasksApp()
        }
    }
}

@Composable
private fun NotesAndTasksApp(
    appViewModel: AppViewModel = hiltViewModel(),
) {
    val uiState by appViewModel.uiState.collectAsStateWithLifecycle()
    NotesAndTasksTheme(
        darkTheme = uiState.userSettings.themeMode.resolveDarkTheme(),
        accentColor = uiState.userSettings.accentColor,
    ) {
        Surface(color = MaterialTheme.colorScheme.background) {
            AppNavigation()
        }
    }
}

@Composable
private fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val tabs = listOf(
        AppTab(
            route = NotesTab,
            labelRes = R.string.tab_notes,
            icon = { Icon(Icons.Outlined.NoteAlt, contentDescription = null) },
        ),
        AppTab(
            route = TasksTab,
            labelRes = R.string.tab_tasks,
            icon = { Icon(Icons.Outlined.TaskAlt, contentDescription = null) },
        ),
        AppTab(
            route = SettingsTab,
            labelRes = R.string.tab_settings,
            icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
        ),
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentDestination.isSelectedFor(tab.route),
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = tab.icon,
                        label = { Text(text = stringResource(tab.labelRes)) },
                    )
                }
            }
        },
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = NotesTab,
            modifier = Modifier.padding(contentPadding),
        ) {
            composable<NotesTab> {
                NotesListRoute(
                    viewModel = hiltViewModel<NotesListViewModel>(),
                    onCreateNote = { navController.navigate(NoteEditor()) },
                    onOpenNote = { noteId -> navController.navigate(NoteEditor(noteId)) },
                )
            }
            notesFeatureGraph(
                navController = navController,
                notesListContent = { onCreateNote, onOpenNote ->
                    NotesListRoute(
                        viewModel = hiltViewModel<NotesListViewModel>(),
                        onCreateNote = onCreateNote,
                        onOpenNote = onOpenNote,
                    )
                },
                noteEditorContent = { _, onNavigateBack ->
                    NoteEditorRoute(
                        viewModel = hiltViewModel<NoteEditorViewModel>(),
                        onBack = onNavigateBack,
                    )
                },
            )
            composable<TasksTab> {
                TasksRoute(viewModel = hiltViewModel<TasksViewModel>())
            }
            tasksFeatureGraph {
                TasksRoute(viewModel = hiltViewModel<TasksViewModel>())
            }
            composable<SettingsTab> {
                SettingsRoute(viewModel = hiltViewModel<SettingsViewModel>())
            }
            settingsFeatureGraph {
                SettingsRoute(viewModel = hiltViewModel<SettingsViewModel>())
            }
        }
    }
}

private fun NavDestination?.isSelectedFor(tab: TopLevelRoute): Boolean = when (tab) {
    NotesTab -> this?.hasRoute<NotesTab>() == true ||
        this?.hasRoute<NotesList>() == true ||
        this?.hasRoute<NoteEditor>() == true

    TasksTab -> this?.hasRoute<TasksTab>() == true || this?.hasRoute<TasksList>() == true
    SettingsTab -> this?.hasRoute<SettingsTab>() == true || this?.hasRoute<SettingsScreen>() == true
}

private data class AppTab(
    val route: TopLevelRoute,
    val labelRes: Int,
    val icon: @Composable () -> Unit,
)

@Composable
private fun ThemeMode.resolveDarkTheme(): Boolean = when (this) {
    ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}
