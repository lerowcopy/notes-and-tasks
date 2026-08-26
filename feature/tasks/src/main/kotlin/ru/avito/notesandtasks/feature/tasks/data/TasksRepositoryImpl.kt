package ru.avito.notesandtasks.feature.tasks.data

import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.avito.notesandtasks.core.common.coroutines.DispatcherProvider
import ru.avito.notesandtasks.core.common.flow.SortOrder
import ru.avito.notesandtasks.core.common.result.OperationResult
import ru.avito.notesandtasks.core.database.dao.TaskDao
import ru.avito.notesandtasks.core.database.entity.TaskEntity
import ru.avito.notesandtasks.feature.tasks.domain.model.Task
import ru.avito.notesandtasks.feature.tasks.domain.repository.TasksRepository

class TasksRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val dispatchers: DispatcherProvider,
) : TasksRepository {
    override fun observeTasks(
        query: String,
        sortOrder: SortOrder,
    ): Flow<List<Task>> = taskDao.observeTasks(query, sortOrder).map { tasks ->
        tasks.map(TaskEntity::toDomain)
    }

    override suspend fun create(task: Task): OperationResult<Long> = withContext(dispatchers.io) {
        try {
            OperationResult.Success(taskDao.insert(task.toEntity()))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            OperationResult.Error(cause = error)
        }
    }

    override suspend fun updateCompletion(
        taskId: Long,
        isCompleted: Boolean,
    ): OperationResult<Unit> = withContext(dispatchers.io) {
        try {
            taskDao.updateCompletion(taskId = taskId, isCompleted = isCompleted)
            OperationResult.Success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            OperationResult.Error(cause = error)
        }
    }
}

private fun TaskEntity.toDomain(): Task = Task(
    id = id,
    title = title,
    isCompleted = isCompleted,
    createdAt = createdAt,
)

private fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    title = title,
    isCompleted = isCompleted,
    createdAt = createdAt,
)
