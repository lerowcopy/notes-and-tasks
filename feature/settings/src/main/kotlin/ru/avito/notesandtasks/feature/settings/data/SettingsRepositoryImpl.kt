package ru.avito.notesandtasks.feature.settings.data

import kotlinx.coroutines.flow.Flow
import ru.avito.notesandtasks.core.common.result.OperationResult
import ru.avito.notesandtasks.core.common.settings.AccentColor
import ru.avito.notesandtasks.core.common.settings.ThemeMode
import ru.avito.notesandtasks.core.datastore.UserSettings
import ru.avito.notesandtasks.core.datastore.UserSettingsRepository
import ru.avito.notesandtasks.core.gigachat.client.BalanceEntry
import ru.avito.notesandtasks.core.gigachat.client.GigaChatClient
import ru.avito.notesandtasks.core.network.result.ApiResult
import ru.avito.notesandtasks.feature.settings.domain.repository.SettingsRepository

class SettingsRepositoryImpl(
    private val userSettingsRepository: UserSettingsRepository,
    private val gigaChatClient: GigaChatClient,
) : SettingsRepository {
    override val userSettings: Flow<UserSettings> = userSettingsRepository.userSettings

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        userSettingsRepository.setThemeMode(themeMode)
    }

    override suspend fun setAccentColor(accentColor: AccentColor) {
        userSettingsRepository.setAccentColor(accentColor)
    }

    override suspend fun reset() {
        userSettingsRepository.reset()
    }

    override suspend fun getBalance(): OperationResult<List<BalanceEntry>> = when (
        val result = gigaChatClient.getBalance()
    ) {
        is ApiResult.Success -> OperationResult.Success(result.data)
        is ApiResult.HttpError -> OperationResult.Error(cause = BalanceHttpException(result.code))
        is ApiResult.NetworkError -> OperationResult.Error(cause = result.cause)
        is ApiResult.UnknownError -> OperationResult.Error(cause = result.cause)
    }
}

class BalanceHttpException(
    val code: Int,
) : IllegalStateException()
