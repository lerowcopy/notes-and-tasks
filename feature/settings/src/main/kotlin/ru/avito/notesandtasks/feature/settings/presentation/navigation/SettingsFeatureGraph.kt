package ru.avito.notesandtasks.feature.settings.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import ru.avito.notesandtasks.core.navigation.graph.settingsFeatureGraph as coreSettingsFeatureGraph

fun NavGraphBuilder.settingsFeatureGraph(
    settingsContent: @Composable () -> Unit,
) {
    coreSettingsFeatureGraph(settingsContent = settingsContent)
}
