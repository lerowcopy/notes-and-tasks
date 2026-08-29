package ru.avito.notesandtasks.feature.tasks.presentation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import ru.avito.notesandtasks.core.common.flow.SortOrder
import ru.avito.notesandtasks.core.common.result.OperationResult
import ru.avito.notesandtasks.core.voice.recording.VoiceRecorder
import ru.avito.notesandtasks.feature.tasks.domain.model.Task
import ru.avito.notesandtasks.feature.tasks.domain.model.TaskStatusFilter
import ru.avito.notesandtasks.feature.tasks.domain.model.ToggleTaskStatusParams
import ru.avito.notesandtasks.feature.tasks.domain.usecase.CreateTaskFromVoiceUseCase
import ru.avito.notesandtasks.feature.tasks.domain.usecase.CreateTaskUseCase
import ru.avito.notesandtasks.feature.tasks.domain.usecase.GetTasksParams
import ru.avito.notesandtasks.feature.tasks.domain.usecase.GetTasksUseCase
import ru.avito.notesandtasks.feature.tasks.domain.usecase.TaskTitleRequiredException
import ru.avito.notesandtasks.feature.tasks.domain.usecase.ToggleTaskStatusUseCase

data class TasksUiState(
    val isLoading: Boolean = true,
    val tasks: List<Task> = emptyList(),
    val queryDraft: String = "",
    val submittedQuery: String = "",
    val filter: TaskStatusFilter = TaskStatusFilter.All,
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST,
    val isFabMenuExpanded: Boolean = false,
    val isTextEntryVisible: Boolean = false,
    val textEntry: String = "",
    val isTextEntrySaving: Boolean = false,
    val isTextEntryError: Boolean = false,
    val voiceState: VoiceTaskState = VoiceTaskState.Idle,
    val loadError: Throwable? = null,
    val operationError: Throwable? = null,
)

sealed interface VoiceTaskState {
    data object Idle : VoiceTaskState

    data object Recording : VoiceTaskState

    data object Processing : VoiceTaskState

    data class Error(
        val cause: Throwable,
    ) : VoiceTaskState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TasksViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val toggleTaskStatusUseCase: ToggleTaskStatusUseCase,
    private val createTaskUseCase: CreateTaskUseCase,
    private val createTaskFromVoiceUseCase: CreateTaskFromVoiceUseCase,
    private val voiceRecorder: VoiceRecorder,
) : ViewModel() {
    private val submittedQuery = MutableStateFlow("")
    private val selectedFilter = MutableStateFlow(TaskStatusFilter.All)
    private val selectedSortOrder = MutableStateFlow(SortOrder.NEWEST_FIRST)
    private val mutableUiState = MutableStateFlow(TasksUiState())
    private var tasksObservation: Job? = null

    val uiState: StateFlow<TasksUiState> = mutableUiState.asStateFlow()

    init {
        observeTasks()
    }

    fun onQueryChange(query: String) {
        updateState { copy(queryDraft = query) }
    }

    fun onSearch(query: String) {
        onQueryChange(query)
    }

    fun onFilterChange(filter: TaskStatusFilter) {
        selectedFilter.value = filter
        updateState { copy(filter = filter) }
    }

    fun onSortOrderChange(sortOrder: SortOrder) {
        selectedSortOrder.value = sortOrder
        updateState { copy(sortOrder = sortOrder) }
    }

    fun onFabMenuChange(isExpanded: Boolean) {
        updateState { copy(isFabMenuExpanded = isExpanded) }
    }

    fun onStartTextEntry() {
        updateState {
            copy(
                isFabMenuExpanded = false,
                isTextEntryVisible = true,
                isTextEntryError = false,
                operationError = null,
            )
        }
    }

    fun onTextEntryChange(text: String) {
        updateState { copy(textEntry = text, isTextEntryError = false) }
    }

    fun onSubmitTextEntry() {
        val title = mutableUiState.value.textEntry
        if (title.isBlank()) {
            updateState { copy(isTextEntryError = true) }
            return
        }

        viewModelScope.launch {
            updateState {
                copy(
                    isTextEntrySaving = true,
                    isTextEntryError = false,
                    operationError = null
                )
            }
            when (val result = createTaskUseCase(title)) {
                is OperationResult.Success -> updateState {
                    copy(
                        isTextEntryVisible = false,
                        textEntry = "",
                        isTextEntrySaving = false,
                    )
                }

                is OperationResult.Error -> updateState {
                    copy(
                        isTextEntrySaving = false,
                        isTextEntryError = result.cause == TaskTitleRequiredException,
                        operationError = if (result.cause == TaskTitleRequiredException) {
                            null
                        } else {
                            result.cause ?: TaskOperationFailedException
                        },
                    )
                }
            }
        }
    }

    fun onToggleTaskStatus(
        taskId: Long,
        isCompleted: Boolean,
    ) {
        viewModelScope.launch {
            when (val result =
                toggleTaskStatusUseCase(ToggleTaskStatusParams(taskId, isCompleted))) {
                is OperationResult.Success -> Unit
                is OperationResult.Error -> updateState {
                    copy(operationError = result.cause ?: TaskOperationFailedException)
                }
            }
        }
    }

    fun onStartVoiceCreation() {
        updateState { copy(isFabMenuExpanded = false, operationError = null) }
        when (val result = voiceRecorder.start()) {
            is OperationResult.Success -> updateState { copy(voiceState = VoiceTaskState.Recording) }
            is OperationResult.Error -> updateState {
                copy(
                    voiceState = VoiceTaskState.Error(
                        result.cause ?: VoiceTaskCreationFailedException
                    )
                )
            }
        }
    }

    fun onStopVoiceCreation() {
        when (val result = voiceRecorder.stop()) {
            is OperationResult.Success -> createTaskFromVoice(File(result.data))
            is OperationResult.Error -> updateState {
                copy(
                    voiceState = VoiceTaskState.Error(
                        result.cause ?: VoiceTaskCreationFailedException
                    )
                )
            }
        }
    }

    fun clearVoiceError() {
        updateState { copy(voiceState = VoiceTaskState.Idle) }
    }

    fun clearOperationError() {
        updateState { copy(operationError = null) }
    }

    fun retry() {
        observeTasks()
    }

    override fun onCleared() {
        voiceRecorder.release()
        super.onCleared()
    }

    private fun observeTasks() {
        tasksObservation?.cancel()
        tasksObservation = viewModelScope.launch {
            combine(submittedQuery, selectedFilter, selectedSortOrder) { query, filter, sortOrder ->
                GetTasksParams(query = query, filter = filter, sortOrder = sortOrder)
            }.flatMapLatest { parameters ->
                getTasksUseCase(parameters)
            }.onStart {
                updateState { copy(isLoading = true, loadError = null) }
            }.catch { error ->
                updateState { copy(isLoading = false, loadError = error) }
            }.collect { tasks ->
                updateState { copy(isLoading = false, tasks = tasks, loadError = null) }
            }
        }
    }

    private fun createTaskFromVoice(audioFile: File) {
        viewModelScope.launch {
            updateState { copy(voiceState = VoiceTaskState.Processing) }
            try {
                when (val result = createTaskFromVoiceUseCase(audioFile)) {
                    is OperationResult.Success -> updateState { copy(voiceState = VoiceTaskState.Idle) }
                    is OperationResult.Error -> updateState {
                        copy(
                            voiceState = VoiceTaskState.Error(
                                result.cause ?: VoiceTaskCreationFailedException
                            )
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                updateState { copy(voiceState = VoiceTaskState.Error(error)) }
            } finally {
                audioFile.delete()
            }
        }
    }

    private fun updateState(transform: TasksUiState.() -> TasksUiState) {
        mutableUiState.value = mutableUiState.value.transform()
    }
}

data object VoiceTaskCreationFailedException : IllegalStateException()

data object TaskOperationFailedException : IllegalStateException()
