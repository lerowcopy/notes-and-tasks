package ru.avito.notesandtasks.feature.settings.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.avito.notesandtasks.core.common.result.OperationResult
import ru.avito.notesandtasks.core.common.settings.AccentColor
import ru.avito.notesandtasks.core.common.settings.ThemeMode
import ru.avito.notesandtasks.core.datastore.UserSettings
import ru.avito.notesandtasks.core.gigachat.client.BalanceEntry

interface SettingsRepository {
    val userSettings: Flow<UserSettings>

    suspend fun setThemeMode(themeMode: ThemeMode)

    suspend fun setAccentColor(accentColor: AccentColor)

    suspend fun reset()

    suspend fun getBalance(): OperationResult<List<BalanceEntry>>
}
