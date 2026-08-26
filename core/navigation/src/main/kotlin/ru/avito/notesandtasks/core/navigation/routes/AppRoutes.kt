package ru.avito.notesandtasks.core.navigation.routes

import kotlinx.serialization.Serializable

sealed interface TopLevelRoute

@Serializable
data object NotesTab : TopLevelRoute

@Serializable
data object TasksTab : TopLevelRoute

@Serializable
data object SettingsTab : TopLevelRoute

@Serializable
data object NotesList

@Serializable
data class NoteEditor(
    val noteId: Long? = null,
)

@Serializable
data object TasksList

@Serializable
data object SettingsScreen
