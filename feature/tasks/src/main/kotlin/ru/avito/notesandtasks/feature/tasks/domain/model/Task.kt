package ru.avito.notesandtasks.feature.tasks.domain.model

data class Task(
    val id: Long,
    val title: String,
    val isCompleted: Boolean,
    val createdAt: Long,
)

enum class TaskStatusFilter {
    All,
    Active,
    Completed,
}

data class ToggleTaskStatusParams(
    val taskId: Long,
    val isCompleted: Boolean,
)
