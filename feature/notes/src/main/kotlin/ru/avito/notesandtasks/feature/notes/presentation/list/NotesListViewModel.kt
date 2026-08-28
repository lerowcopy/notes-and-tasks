package ru.avito.notesandtasks.feature.notes.presentation.list

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import ru.avito.notesandtasks.core.common.flow.SortOrder
import ru.avito.notesandtasks.core.common.result.OperationResult
import ru.avito.notesandtasks.feature.notes.domain.model.Note
import ru.avito.notesandtasks.feature.notes.domain.usecase.DeleteNoteUseCase
import ru.avito.notesandtasks.feature.notes.domain.usecase.GetNotesParams
import ru.avito.notesandtasks.feature.notes.domain.usecase.GetNotesUseCase

data class NotesListUiState(
    val isLoading: Boolean = true,
    val notes: List<Note> = emptyList(),
    val queryDraft: String = "",
    val submittedQuery: String = "",
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST,
    val isDeleteMode: Boolean = false,
    val error: Throwable? = null,
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class NotesListViewModel @Inject constructor(
    private val getNotesUseCase: GetNotesUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
) : ViewModel() {
    private val queryDraft = MutableStateFlow("")
    private val sortOrder = MutableStateFlow(SortOrder.NEWEST_FIRST)
    private val mutableUiState = MutableStateFlow(NotesListUiState())
    private var notesObservation: Job? = null

    val uiState: StateFlow<NotesListUiState> = mutableUiState.asStateFlow()

    init {
        observeNotes()
    }

    fun onQueryChange(query: String) {
        queryDraft.value = query
        mutableUiState.value = mutableUiState.value.copy(queryDraft = query)
    }

    fun onSearch(query: String) {
        onQueryChange(query)
    }

    fun onSortOrderChange(order: SortOrder) {
        sortOrder.value = order
        mutableUiState.value = mutableUiState.value.copy(sortOrder = order)
    }

    fun onDeleteModeChange(enabled: Boolean) {
        mutableUiState.value = mutableUiState.value.copy(isDeleteMode = enabled)
    }

    fun delete(noteId: Long) {
        viewModelScope.launch {
            when (val result = deleteNoteUseCase(noteId)) {
                is OperationResult.Success -> Unit
                is OperationResult.Error -> {
                    mutableUiState.value = mutableUiState.value.copy(
                        error = result.cause,
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun retry() {
        observeNotes()
    }

    private fun observeNotes() {
        notesObservation?.cancel()
        notesObservation = viewModelScope.launch {
            combine(
                queryDraft
                    .debounce(350L)
                    .distinctUntilChanged()
                    .onEach { query ->
                        mutableUiState.value = mutableUiState.value.copy(submittedQuery = query)
                    },
                sortOrder,
            ) { query, order ->
                GetNotesParams(query = query, sortOrder = order)
            }.flatMapLatest { params ->
                getNotesUseCase(params)
            }.onStart {
                mutableUiState.value = mutableUiState.value.copy(isLoading = true, error = null)
            }.catch { error ->
                mutableUiState.value = mutableUiState.value.copy(
                    isLoading = false,
                    error = error,
                )
            }.collect { notes ->
                mutableUiState.value = mutableUiState.value.copy(
                    isLoading = false,
                    notes = notes,
                    error = null,
                )
            }
        }
    }
}
