package ru.avito.notesandtasks.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.avito.notesandtasks.core.database.dao.NoteDao
import ru.avito.notesandtasks.core.database.dao.TaskDao
import ru.avito.notesandtasks.core.database.entity.NoteEntity
import ru.avito.notesandtasks.core.database.entity.TaskEntity

@Database(
    entities = [
        NoteEntity::class,
        TaskEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(RoomTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao

    internal companion object {
        const val DATABASE_NAME: String = "notes_and_tasks.db"
    }
}

object AppDatabaseFactory {
    fun create(context: Context): AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        AppDatabase.DATABASE_NAME,
    )
        // TODO: Replace with explicit migrations before the first production release.
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
}
