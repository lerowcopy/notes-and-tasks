package ru.avito.notesandtasks.feature.notes.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.avito.notesandtasks.core.common.result.OperationResult
import ru.avito.notesandtasks.core.common.flow.SortOrder
import ru.avito.notesandtasks.feature.notes.domain.model.Note

interface NotesRepository {
    fun observeNotes(
        query: String,
        sortOrder: SortOrder,
    ): Flow<List<Note>>

    fun observeNote(noteId: Long): Flow<Note?>

    suspend fun save(note: Note): OperationResult<Long>

    suspend fun delete(noteId: Long): OperationResult<Unit>
}
