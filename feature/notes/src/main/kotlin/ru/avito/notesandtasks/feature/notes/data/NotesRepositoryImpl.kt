package ru.avito.notesandtasks.feature.notes.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.avito.notesandtasks.core.common.coroutines.DispatcherProvider
import ru.avito.notesandtasks.core.common.flow.SortOrder
import ru.avito.notesandtasks.core.common.result.OperationResult
import ru.avito.notesandtasks.core.database.dao.NoteDao
import ru.avito.notesandtasks.core.database.entity.NoteEntity
import ru.avito.notesandtasks.feature.notes.domain.model.Note
import ru.avito.notesandtasks.feature.notes.domain.repository.NotesRepository

class NotesRepositoryImpl(
    private val noteDao: NoteDao,
    private val dispatchers: DispatcherProvider,
) : NotesRepository {
    override fun observeNotes(
        query: String,
        sortOrder: SortOrder,
    ): Flow<List<Note>> = noteDao.observeNotes(query, sortOrder).map { notes ->
        notes.map(NoteEntity::toDomain)
    }

    override fun observeNote(noteId: Long): Flow<Note?> = noteDao.observeNote(noteId).map { note ->
        note?.toDomain()
    }

    override suspend fun save(note: Note): OperationResult<Long> = withContext(dispatchers.io) {
        try {
            val entity = note.toEntity()
            val id = if (note.id == 0L) {
                noteDao.insert(entity)
            } else {
                noteDao.update(entity)
                note.id
            }
            OperationResult.Success(id)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            OperationResult.Error(cause = error)
        }
    }

    override suspend fun delete(noteId: Long): OperationResult<Unit> = withContext(dispatchers.io) {
        try {
            noteDao.deleteById(noteId)
            OperationResult.Success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            OperationResult.Error(cause = error)
        }
    }
}

private fun NoteEntity.toDomain(): Note = Note(
    id = id,
    title = title,
    text = text,
    imagePath = imagePath,
    createdAt = createdAt,
)

private fun Note.toEntity(): NoteEntity = NoteEntity(
    id = id,
    title = title,
    text = text,
    imagePath = imagePath,
    createdAt = createdAt,
)
