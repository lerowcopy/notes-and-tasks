package ru.avito.notesandtasks.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.avito.notesandtasks.core.common.flow.SortOrder
import ru.avito.notesandtasks.core.database.entity.TaskEntity

@Dao
interface TaskDao {
    @Query(
        """
        SELECT * FROM tasks
        WHERE :query = '' OR title LIKE '%' || :query || '%'
        ORDER BY
            isCompleted ASC,
            CASE WHEN :sortOrder = 'NEWEST_FIRST' THEN createdAt END DESC,
            CASE WHEN :sortOrder = 'OLDEST_FIRST' THEN createdAt END ASC,
            id DESC
        """,
    )
    fun observeTasks(
        query: String,
        sortOrder: SortOrder,
    ): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :taskId")
    suspend fun updateCompletion(
        taskId: Long,
        isCompleted: Boolean,
    )
}
