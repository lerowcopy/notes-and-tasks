package ru.avito.notesandtasks.feature.notes.presentation.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.avito.notesandtasks.core.common.result.OperationResult
import ru.avito.notesandtasks.core.network.result.ApiResult
import ru.avito.notesandtasks.core.voice.recognition.SpeechRecognizer
import ru.avito.notesandtasks.core.voice.recording.VoiceRecorder
import ru.avito.notesandtasks.feature.notes.domain.model.NoteDraft
import ru.avito.notesandtasks.feature.notes.domain.usecase.GetNoteUseCase
import ru.avito.notesandtasks.feature.notes.domain.usecase.NoteTitleRequiredException
import ru.avito.notesandtasks.feature.notes.domain.usecase.SaveNoteUseCase

data class NoteEditorUiState(
    val isLoading: Boolean = false,
    val title: String = "",
    val text: String = "",
    val imagePath: String? = null,
    val createdAt: Long = 0L,
    val isSaving: Boolean = false,
    val isVoiceRecording: Boolean = false,
    val isVoiceProcessing: Boolean = false,
    val isTitleError: Boolean = false,
    val saveError: NoteEditorSaveError? = null,
    val voiceError: Throwable? = null,
    val savedNoteId: Long? = null,
)

enum class NoteEditorSaveError {
    RequiredTitle,
    SaveFailed,
}

class NoteEditorViewModel(
    private val noteId: Long?,
    private val savedStateHandle: SavedStateHandle,
    private val getNoteUseCase: GetNoteUseCase,
    private val saveNoteUseCase: SaveNoteUseCase,
    private val voiceRecorder: VoiceRecorder,
    private val speechRecognizer: SpeechRecognizer?,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(initialState())

    val uiState: StateFlow<NoteEditorUiState> = mutableUiState.asStateFlow()

    init {
        loadExistingNoteIfNeeded()
    }

    fun onTitleChange(title: String) {
        updateState {
            copy(
                title = title,
                isTitleError = false,
                saveError = null,
                savedNoteId = null,
            )
        }
    }

    fun onTextChange(text: String) {
        updateState { copy(text = text, savedNoteId = null) }
    }

    fun onImageReady(result: OperationResult<String>) {
        when (result) {
            is OperationResult.Success -> updateState {
                copy(imagePath = result.data, saveError = null, savedNoteId = null)
            }

            is OperationResult.Error -> updateState {
                copy(saveError = NoteEditorSaveError.SaveFailed)
            }
        }
    }

    fun removeImage() {
        updateState { copy(imagePath = null, savedNoteId = null) }
    }

    fun onStartVoiceRecording() {
        when (val result = voiceRecorder.start()) {
            is OperationResult.Success -> updateState {
                copy(isVoiceRecording = true, isVoiceProcessing = false, voiceError = null)
            }

            is OperationResult.Error -> updateState {
                copy(isVoiceRecording = false, isVoiceProcessing = false, voiceError = result.cause)
            }
        }
    }

    fun onStopVoiceRecording() {
        when (val result = voiceRecorder.stop()) {
            is OperationResult.Success -> recognizeVoice(File(result.data))
            is OperationResult.Error -> updateState {
                copy(isVoiceRecording = false, isVoiceProcessing = false, voiceError = result.cause)
            }
        }
    }

    fun onSave() {
        val current = mutableUiState.value
        if (current.title.isBlank()) {
            updateState {
                copy(
                    isTitleError = true,
                    saveError = NoteEditorSaveError.RequiredTitle,
                    savedNoteId = null,
                )
            }
            return
        }

        viewModelScope.launch {
            updateState { copy(isSaving = true, saveError = null, savedNoteId = null) }
            when (
                val result = saveNoteUseCase(
                    NoteDraft(
                        id = noteId,
                        title = current.title,
                        text = current.text,
                        imagePath = current.imagePath,
                        createdAt = current.createdAt,
                    ),
                )
            ) {
                is OperationResult.Success -> updateState {
                    copy(isSaving = false, savedNoteId = result.data)
                }

                is OperationResult.Error -> updateState {
                    copy(
                        isSaving = false,
                        isTitleError = result.cause == NoteTitleRequiredException,
                        saveError = if (result.cause == NoteTitleRequiredException) {
                            NoteEditorSaveError.RequiredTitle
                        } else {
                            NoteEditorSaveError.SaveFailed
                        },
                    )
                }
            }
        }
    }

    fun onSavedNavigationHandled() {
        updateState { copy(savedNoteId = null) }
    }

    fun clearVoiceError() {
        updateState { copy(voiceError = null) }
    }

    override fun onCleared() {
        voiceRecorder.release()
        super.onCleared()
    }

    private fun initialState(): NoteEditorUiState = NoteEditorUiState(
        title = savedStateHandle[STATE_TITLE] ?: "",
        text = savedStateHandle[STATE_TEXT] ?: "",
        imagePath = savedStateHandle[STATE_IMAGE_PATH],
        createdAt = savedStateHandle[STATE_CREATED_AT] ?: currentTimeMillis(),
    )

    private fun loadExistingNoteIfNeeded() {
        if (noteId == null || hasRestoredDraft()) {
            return
        }
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            try {
                getNoteUseCase(noteId).first()?.let { note ->
                    updateState {
                        copy(
                            isLoading = false,
                            title = note.title,
                            text = note.text,
                            imagePath = note.imagePath,
                            createdAt = note.createdAt,
                        )
                    }
                } ?: updateState {
                    copy(isLoading = false, saveError = NoteEditorSaveError.SaveFailed)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                updateState { copy(isLoading = false, saveError = NoteEditorSaveError.SaveFailed) }
            }
        }
    }

    private fun hasRestoredDraft(): Boolean = savedStateHandle.contains(STATE_TITLE) ||
        savedStateHandle.contains(STATE_TEXT) ||
        savedStateHandle.contains(STATE_IMAGE_PATH)

    private fun recognizeVoice(audioFile: File) {
        val recognizer = speechRecognizer
        if (recognizer == null) {
            audioFile.delete()
            updateState {
                copy(
                    isVoiceRecording = false,
                    isVoiceProcessing = false,
                    voiceError = VoiceRecognitionUnavailableException,
                )
            }
            return
        }

        viewModelScope.launch {
            updateState { copy(isVoiceRecording = false, isVoiceProcessing = true, voiceError = null) }
            try {
                when (val result = recognizer.recognize(audioFile)) {
                    is ApiResult.Success -> updateState {
                        copy(
                            text = appendTranscript(text, result.data),
                            isVoiceProcessing = false,
                            voiceError = null,
                            savedNoteId = null,
                        )
                    }

                    is ApiResult.HttpError -> updateState {
                        copy(
                            isVoiceProcessing = false,
                            voiceError = VoiceRecognitionHttpException(result.code),
                        )
                    }

                    is ApiResult.NetworkError -> updateState {
                        copy(isVoiceProcessing = false, voiceError = result.cause)
                    }

                    is ApiResult.UnknownError -> updateState {
                        copy(isVoiceProcessing = false, voiceError = result.cause)
                    }
                }
            } finally {
                audioFile.delete()
            }
        }
    }

    private fun updateState(transform: NoteEditorUiState.() -> NoteEditorUiState) {
        val updatedState = mutableUiState.value.transform()
        mutableUiState.value = updatedState
        savedStateHandle[STATE_TITLE] = updatedState.title
        savedStateHandle[STATE_TEXT] = updatedState.text
        savedStateHandle[STATE_IMAGE_PATH] = updatedState.imagePath
        savedStateHandle[STATE_CREATED_AT] = updatedState.createdAt
    }

    private companion object {
        const val STATE_TITLE = "note_editor_title"
        const val STATE_TEXT = "note_editor_text"
        const val STATE_IMAGE_PATH = "note_editor_image_path"
        const val STATE_CREATED_AT = "note_editor_created_at"
    }
}

private fun appendTranscript(currentText: String, transcript: String): String = if (currentText.isBlank()) {
    transcript
} else {
    currentText + "\n" + transcript
}

data object VoiceRecognitionUnavailableException : IllegalStateException()

class VoiceRecognitionHttpException(
    val code: Int,
) : IllegalStateException()
