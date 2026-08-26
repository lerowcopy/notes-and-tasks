package ru.avito.notesandtasks.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.avito.notesandtasks.core.common.flow.SortOrder
import ru.avito.notesandtasks.core.database.entity.NoteEntity

@Dao
interface NoteDao {
    @Query(
        """
        SELECT * FROM notes
        WHERE :query = '' OR title LIKE '%' || :query || '%'
        ORDER BY
            CASE WHEN :sortOrder = 'NEWEST_FIRST' THEN createdAt END DESC,
            CASE WHEN :sortOrder = 'OLDEST_FIRST' THEN createdAt END ASC,
            id DESC
        """,
    )
    fun observeNotes(
        query: String,
        sortOrder: SortOrder,
    ): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    fun observeNote(noteId: Long): Flow<NoteEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteById(noteId: Long)
}
