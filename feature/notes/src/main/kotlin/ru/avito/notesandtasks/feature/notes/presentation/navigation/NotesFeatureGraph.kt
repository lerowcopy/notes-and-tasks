package ru.avito.notesandtasks.feature.notes.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import ru.avito.notesandtasks.core.navigation.graph.notesFeatureGraph as coreNotesFeatureGraph

fun NavGraphBuilder.notesFeatureGraph(
    navController: NavController,
    notesListContent: @Composable (onCreateNote: () -> Unit, onOpenNote: (Long) -> Unit) -> Unit,
    noteEditorContent: @Composable (noteId: Long?, onNavigateBack: () -> Unit) -> Unit,
) {
    coreNotesFeatureGraph(
        navController = navController,
        notesListContent = notesListContent,
        noteEditorContent = noteEditorContent,
    )
}
