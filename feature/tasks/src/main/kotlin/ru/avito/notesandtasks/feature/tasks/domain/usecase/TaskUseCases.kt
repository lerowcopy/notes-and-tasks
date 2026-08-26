package ru.avito.notesandtasks.feature.tasks.domain.usecase

import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.avito.notesandtasks.core.common.flow.SortOrder
import ru.avito.notesandtasks.core.common.result.OperationResult
import ru.avito.notesandtasks.core.common.usecase.UseCase
import ru.avito.notesandtasks.core.gigachat.client.GigaChatClient
import ru.avito.notesandtasks.core.network.result.ApiResult
import ru.avito.notesandtasks.core.voice.recognition.SpeechRecognizer
import ru.avito.notesandtasks.feature.tasks.domain.model.Task
import ru.avito.notesandtasks.feature.tasks.domain.model.TaskStatusFilter
import ru.avito.notesandtasks.feature.tasks.domain.model.ToggleTaskStatusParams
import ru.avito.notesandtasks.feature.tasks.domain.repository.TasksRepository

data class GetTasksParams(
    val query: String,
    val filter: TaskStatusFilter,
    val sortOrder: SortOrder,
)

class GetTasksUseCase @Inject constructor(
    private val repository: TasksRepository,
) : UseCase<GetTasksParams, Flow<List<Task>>> {
    override suspend fun invoke(parameters: GetTasksParams): Flow<List<Task>> = repository
        .observeTasks(
            query = parameters.query,
            sortOrder = parameters.sortOrder,
        ).map { tasks ->
            when (parameters.filter) {
                TaskStatusFilter.All -> tasks
                TaskStatusFilter.Active -> tasks.filterNot(Task::isCompleted)
                TaskStatusFilter.Completed -> tasks.filter(Task::isCompleted)
            }
        }
}

class ToggleTaskStatusUseCase @Inject constructor(
    private val repository: TasksRepository,
) : UseCase<ToggleTaskStatusParams, OperationResult<Unit>> {
    override suspend fun invoke(parameters: ToggleTaskStatusParams): OperationResult<Unit> =
        repository.updateCompletion(
            taskId = parameters.taskId,
            isCompleted = parameters.isCompleted,
        )
}

class CreateTaskUseCase @Inject constructor(
    private val repository: TasksRepository,
) : UseCase<String, OperationResult<Long>> {
    override suspend fun invoke(parameters: String): OperationResult<Long> {
        val title = parameters.trim()
        if (title.isEmpty()) {
            return OperationResult.Error(cause = TaskTitleRequiredException)
        }
        return repository.create(
            Task(
                id = 0,
                title = title,
                isCompleted = false,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }
}

class CreateTaskFromVoiceUseCase @Inject constructor(
    private val speechRecognizer: SpeechRecognizer,
    private val gigaChatClient: GigaChatClient,
    private val createTaskUseCase: CreateTaskUseCase,
) : UseCase<File, OperationResult<Long>> {
    override suspend fun invoke(parameters: File): OperationResult<Long> = when (
        val recognitionResult = speechRecognizer.recognize(parameters)
    ) {
        is ApiResult.Success -> when (
            val refinementResult = gigaChatClient.refineTaskText(recognitionResult.data)
        ) {
            is ApiResult.Success -> createTaskUseCase(refinementResult.data)
            is ApiResult.HttpError -> OperationResult.Error(
                cause = VoiceTaskHttpException(refinementResult.code),
            )

            is ApiResult.NetworkError -> OperationResult.Error(cause = refinementResult.cause)
            is ApiResult.UnknownError -> OperationResult.Error(cause = refinementResult.cause)
        }

        is ApiResult.HttpError -> OperationResult.Error(
            cause = VoiceTaskHttpException(recognitionResult.code),
        )

        is ApiResult.NetworkError -> OperationResult.Error(cause = recognitionResult.cause)
        is ApiResult.UnknownError -> OperationResult.Error(cause = recognitionResult.cause)
    }
}

data object TaskTitleRequiredException : IllegalArgumentException()

class VoiceTaskHttpException(
    val code: Int,
) : IllegalStateException()
