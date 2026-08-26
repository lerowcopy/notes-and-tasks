package ru.avito.notesandtasks.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import ru.avito.notesandtasks.core.common.settings.AccentColor
import ru.avito.notesandtasks.core.common.settings.ThemeMode

private const val USER_SETTINGS_DATA_STORE_NAME = "user_settings"

private val Context.userSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = USER_SETTINGS_DATA_STORE_NAME,
)

data class UserSettings(
    val themeMode: ThemeMode,
    val accentColor: AccentColor,
) {
    companion object {
        val DEFAULT: UserSettings = UserSettings(
            themeMode = ThemeMode.SYSTEM,
            accentColor = AccentColor.BLUE,
        )
    }
}

interface UserSettingsRepository {
    val userSettings: Flow<UserSettings>

    suspend fun setThemeMode(themeMode: ThemeMode)

    suspend fun setAccentColor(accentColor: AccentColor)

    suspend fun reset()
}

class UserSettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : UserSettingsRepository {
    override val userSettings: Flow<UserSettings> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { preferences ->
            UserSettings(
                themeMode = preferences[PreferenceKeys.themeMode].toThemeMode(),
                accentColor = preferences[PreferenceKeys.accentColor].toAccentColor(),
            )
        }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.themeMode] = themeMode.name
        }
    }

    override suspend fun setAccentColor(accentColor: AccentColor) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.accentColor] = accentColor.name
        }
    }

    override suspend fun reset() {
        dataStore.edit { preferences ->
            preferences.remove(PreferenceKeys.themeMode)
            preferences.remove(PreferenceKeys.accentColor)
        }
    }
}

fun createUserSettingsRepository(context: Context): UserSettingsRepository =
    UserSettingsRepositoryImpl(context.userSettingsDataStore)

private object PreferenceKeys {
    val themeMode = stringPreferencesKey("theme_mode")
    val accentColor = stringPreferencesKey("accent_color")
}

private fun String?.toThemeMode(): ThemeMode = ThemeMode.entries.firstOrNull {
    it.name == this
} ?: UserSettings.DEFAULT.themeMode

private fun String?.toAccentColor(): AccentColor = AccentColor.entries.firstOrNull {
    it.name == this
} ?: UserSettings.DEFAULT.accentColor
