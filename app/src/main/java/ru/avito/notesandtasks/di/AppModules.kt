package ru.avito.notesandtasks.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton
import ru.avito.notesandtasks.core.common.coroutines.DefaultDispatcherProvider
import ru.avito.notesandtasks.core.common.coroutines.DispatcherProvider
import ru.avito.notesandtasks.core.database.AppDatabase
import ru.avito.notesandtasks.core.database.AppDatabaseFactory
import ru.avito.notesandtasks.core.database.dao.NoteDao
import ru.avito.notesandtasks.core.database.dao.TaskDao
import ru.avito.notesandtasks.core.datastore.UserSettingsRepository
import ru.avito.notesandtasks.core.datastore.createUserSettingsRepository
import ru.avito.notesandtasks.core.gigachat.client.GigaChatClient
import ru.avito.notesandtasks.core.gigachat.client.GigaChatClientFactory
import ru.avito.notesandtasks.core.network.result.ApiResult
import ru.avito.notesandtasks.core.voice.recognition.SpeechRecognizer
import ru.avito.notesandtasks.core.voice.recording.VoiceRecorder
import ru.avito.notesandtasks.feature.notes.data.NotesRepositoryImpl
import ru.avito.notesandtasks.feature.notes.domain.repository.NotesRepository
import ru.avito.notesandtasks.feature.settings.data.SettingsRepositoryImpl
import ru.avito.notesandtasks.feature.settings.domain.repository.SettingsRepository
import ru.avito.notesandtasks.feature.tasks.data.TasksRepositoryImpl
import ru.avito.notesandtasks.feature.tasks.domain.repository.TasksRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindingsModule {
    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(
        implementation: DefaultDispatcherProvider,
    ): DispatcherProvider

    @Binds
    @Singleton
    abstract fun bindNotesRepository(
        implementation: NotesRepositoryImpl,
    ): NotesRepository

    @Binds
    @Singleton
    abstract fun bindTasksRepository(
        implementation: TasksRepositoryImpl,
    ): TasksRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        implementation: SettingsRepositoryImpl,
    ): SettingsRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppProvidersModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase = AppDatabaseFactory.create(context)

    @Provides
    fun provideNoteDao(database: AppDatabase): NoteDao = database.noteDao()

    @Provides
    fun provideTaskDao(database: AppDatabase): TaskDao = database.taskDao()

    @Provides
    @Singleton
    fun provideUserSettingsRepository(
        @ApplicationContext context: Context,
    ): UserSettingsRepository = createUserSettingsRepository(context)

    @Provides
    @Singleton
    fun provideGigaChatClient(
        @ApplicationContext context: Context,
    ): GigaChatClient = GigaChatClientFactory(context).create()

    @Provides
    @Singleton
    fun provideVoiceRecorder(
        @ApplicationContext context: Context,
    ): VoiceRecorder = VoiceRecorder(context)

    @Provides
    @Singleton
    fun provideSpeechRecognizer(): SpeechRecognizer = UnavailableSpeechRecognizer
}

private object UnavailableSpeechRecognizer : SpeechRecognizer {
    override suspend fun recognize(file: File): ApiResult<String> = ApiResult.UnknownError(
        cause = SpeechRecognitionConfigurationException,
    )
}

private data object SpeechRecognitionConfigurationException : IllegalStateException()
