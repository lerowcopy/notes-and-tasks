package ru.avito.notesandtasks.feature.tasks.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.avito.notesandtasks.core.common.flow.SortOrder
import ru.avito.notesandtasks.core.common.result.OperationResult
import ru.avito.notesandtasks.feature.tasks.domain.model.Task

interface TasksRepository {
    fun observeTasks(
        query: String,
        sortOrder: SortOrder,
    ): Flow<List<Task>>

    suspend fun create(task: Task): OperationResult<Long>

    suspend fun updateCompletion(
        taskId: Long,
        isCompleted: Boolean,
    ): OperationResult<Unit>
}
