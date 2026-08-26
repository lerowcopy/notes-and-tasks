package ru.avito.notesandtasks.feature.settings.domain.usecase

import kotlinx.coroutines.flow.Flow
import ru.avito.notesandtasks.core.common.result.OperationResult
import ru.avito.notesandtasks.core.common.settings.AccentColor
import ru.avito.notesandtasks.core.common.settings.ThemeMode
import ru.avito.notesandtasks.core.common.usecase.UseCase
import ru.avito.notesandtasks.core.datastore.UserSettings
import ru.avito.notesandtasks.core.gigachat.client.BalanceEntry
import ru.avito.notesandtasks.feature.settings.domain.repository.SettingsRepository

class GetUserSettingsUseCase(
    private val repository: SettingsRepository,
) : UseCase<Unit, Flow<UserSettings>> {
    override suspend fun invoke(parameters: Unit): Flow<UserSettings> = repository.userSettings
}

class SetThemeModeUseCase(
    private val repository: SettingsRepository,
) : UseCase<ThemeMode, Unit> {
    override suspend fun invoke(parameters: ThemeMode) {
        repository.setThemeMode(parameters)
    }
}

class SetAccentColorUseCase(
    private val repository: SettingsRepository,
) : UseCase<AccentColor, Unit> {
    override suspend fun invoke(parameters: AccentColor) {
        repository.setAccentColor(parameters)
    }
}

class ResetSettingsUseCase(
    private val repository: SettingsRepository,
) : UseCase<Unit, Unit> {
    override suspend fun invoke(parameters: Unit) {
        repository.reset()
    }
}

class GetGigaChatBalanceUseCase(
    private val repository: SettingsRepository,
) : UseCase<Unit, OperationResult<List<BalanceEntry>>> {
    override suspend fun invoke(parameters: Unit): OperationResult<List<BalanceEntry>> = repository.getBalance()
}
