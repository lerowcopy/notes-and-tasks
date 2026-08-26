package ru.avito.notesandtasks.feature.notes.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import ru.avito.notesandtasks.core.common.flow.SortOrder
import ru.avito.notesandtasks.core.common.result.OperationResult
import ru.avito.notesandtasks.core.common.usecase.UseCase
import ru.avito.notesandtasks.feature.notes.domain.model.Note
import ru.avito.notesandtasks.feature.notes.domain.model.NoteDraft
import ru.avito.notesandtasks.feature.notes.domain.repository.NotesRepository

data class GetNotesParams(
    val query: String,
    val sortOrder: SortOrder,
)

class GetNotesUseCase @Inject constructor(
    private val repository: NotesRepository,
) : UseCase<GetNotesParams, Flow<List<Note>>> {
    override suspend fun invoke(parameters: GetNotesParams): Flow<List<Note>> =
        repository.observeNotes(
            query = parameters.query,
            sortOrder = parameters.sortOrder,
        )
}

class GetNoteUseCase @Inject constructor(
    private val repository: NotesRepository,
) : UseCase<Long, Flow<Note?>> {
    override suspend fun invoke(parameters: Long): Flow<Note?> = repository.observeNote(parameters)
}

class SaveNoteUseCase @Inject constructor(
    private val repository: NotesRepository,
) : UseCase<NoteDraft, OperationResult<Long>> {
    override suspend fun invoke(parameters: NoteDraft): OperationResult<Long> {
        val title = parameters.title.trim()
        if (title.isEmpty()) {
            return OperationResult.Error(cause = NoteTitleRequiredException)
        }

        return repository.save(
            Note(
                id = parameters.id ?: 0,
                title = title,
                text = parameters.text,
                imagePath = parameters.imagePath,
                createdAt = parameters.createdAt,
            ),
        )
    }
}

class DeleteNoteUseCase @Inject constructor(
    private val repository: NotesRepository,
) : UseCase<Long, OperationResult<Unit>> {
    override suspend fun invoke(parameters: Long): OperationResult<Unit> = repository.delete(parameters)
}

data object NoteTitleRequiredException : IllegalArgumentException()
