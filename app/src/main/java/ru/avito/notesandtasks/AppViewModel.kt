package ru.avito.notesandtasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import ru.avito.notesandtasks.core.datastore.UserSettings
import ru.avito.notesandtasks.core.datastore.UserSettingsRepository

data class AppUiState(
    val userSettings: UserSettings = UserSettings.DEFAULT,
)

@HiltViewModel
class AppViewModel @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(AppUiState())

    val uiState: StateFlow<AppUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            userSettingsRepository.userSettings
                .catch {
                    emit(UserSettings.DEFAULT)
                }.collect { userSettings ->
                    mutableUiState.value = AppUiState(userSettings = userSettings)
                }
        }
    }
}
