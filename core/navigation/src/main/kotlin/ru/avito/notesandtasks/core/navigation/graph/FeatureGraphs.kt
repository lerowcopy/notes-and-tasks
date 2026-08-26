package ru.avito.notesandtasks.core.navigation.graph

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import ru.avito.notesandtasks.core.navigation.routes.NoteEditor
import ru.avito.notesandtasks.core.navigation.routes.NotesList
import ru.avito.notesandtasks.core.navigation.routes.SettingsScreen
import ru.avito.notesandtasks.core.navigation.routes.TasksList

fun NavGraphBuilder.notesFeatureGraph(
    navController: NavController,
    notesListContent: @Composable (onCreateNote: () -> Unit, onOpenNote: (Long) -> Unit) -> Unit,
    noteEditorContent: @Composable (noteId: Long?, onNavigateBack: () -> Unit) -> Unit,
) {
    composable<NotesList> {
        notesListContent(
            { navController.navigate(NoteEditor()) },
            { noteId -> navController.navigate(NoteEditor(noteId)) },
        )
    }
    composable<NoteEditor> { backStackEntry ->
        noteEditorContent(
            backStackEntry.toRoute<NoteEditor>().noteId,
            navController::popBackStack,
        )
    }
}

fun NavGraphBuilder.tasksFeatureGraph(
    tasksContent: @Composable () -> Unit,
) {
    composable<TasksList> {
        tasksContent()
    }
}

fun NavGraphBuilder.settingsFeatureGraph(
    settingsContent: @Composable () -> Unit,
) {
    composable<SettingsScreen> {
        settingsContent()
    }
}
