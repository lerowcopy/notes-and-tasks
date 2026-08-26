package ru.avito.notesandtasks.feature.notes.domain.model

data class Note(
    val id: Long,
    val title: String,
    val text: String,
    val imagePath: String?,
    val createdAt: Long,
)

data class NoteDraft(
    val id: Long? = null,
    val title: String,
    val text: String,
    val imagePath: String?,
    val createdAt: Long,
)
