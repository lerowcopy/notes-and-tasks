package ru.avito.notesandtasks.feature.tasks.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import ru.avito.notesandtasks.core.navigation.graph.tasksFeatureGraph as coreTasksFeatureGraph

fun NavGraphBuilder.tasksFeatureGraph(
    tasksContent: @Composable () -> Unit,
) {
    coreTasksFeatureGraph(tasksContent = tasksContent)
}
