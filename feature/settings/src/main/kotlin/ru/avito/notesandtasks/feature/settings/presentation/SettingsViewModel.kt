package ru.avito.notesandtasks.feature.settings.presentation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import ru.avito.notesandtasks.core.common.result.OperationResult
import ru.avito.notesandtasks.core.common.settings.AccentColor
import ru.avito.notesandtasks.core.common.settings.ThemeMode
import ru.avito.notesandtasks.core.datastore.UserSettings
import ru.avito.notesandtasks.core.gigachat.client.BalanceEntry
import ru.avito.notesandtasks.feature.settings.domain.usecase.GetGigaChatBalanceUseCase
import ru.avito.notesandtasks.feature.settings.domain.usecase.GetUserSettingsUseCase
import ru.avito.notesandtasks.feature.settings.domain.usecase.ResetSettingsUseCase
import ru.avito.notesandtasks.feature.settings.domain.usecase.SetAccentColorUseCase
import ru.avito.notesandtasks.feature.settings.domain.usecase.SetThemeModeUseCase

data class SettingsUiState(
    val isLoadingSettings: Boolean = true,
    val userSettings: UserSettings = UserSettings.DEFAULT,
    val balanceState: BalanceUiState = BalanceUiState.Idle,
    val settingsError: Throwable? = null,
    val operationError: Throwable? = null,
)

sealed interface BalanceUiState {
    data object Idle : BalanceUiState

    data object Loading : BalanceUiState

    data class Success(
        val entries: List<BalanceEntry>,
    ) : BalanceUiState

    data class Error(
        val cause: Throwable,
    ) : BalanceUiState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getUserSettingsUseCase: GetUserSettingsUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val setAccentColorUseCase: SetAccentColorUseCase,
    private val resetSettingsUseCase: ResetSettingsUseCase,
    private val getGigaChatBalanceUseCase: GetGigaChatBalanceUseCase,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SettingsUiState())
    private var settingsObservation: Job? = null

    val uiState: StateFlow<SettingsUiState> = mutableUiState.asStateFlow()

    init {
        observeSettings()
        refreshBalance()
    }

    fun onThemeModeChange(themeMode: ThemeMode) {
        viewModelScope.launch {
            runSettingsOperation {
                setThemeModeUseCase(themeMode)
            }
        }
    }

    fun onAccentColorChange(accentColor: AccentColor) {
        viewModelScope.launch {
            runSettingsOperation {
                setAccentColorUseCase(accentColor)
            }
        }
    }

    fun onResetSettings() {
        viewModelScope.launch {
            runSettingsOperation {
                resetSettingsUseCase(Unit)
            }
        }
    }

    fun refreshBalance() {
        viewModelScope.launch {
            updateState { copy(balanceState = BalanceUiState.Loading) }
            try {
                when (val result = getGigaChatBalanceUseCase(Unit)) {
                    is OperationResult.Success -> updateState {
                        copy(balanceState = BalanceUiState.Success(result.data))
                    }

                    is OperationResult.Error -> updateState {
                        copy(
                            balanceState = BalanceUiState.Error(
                                result.cause ?: BalanceRequestFailedException,
                            ),
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                updateState { copy(balanceState = BalanceUiState.Error(error)) }
            }
        }
    }

    fun clearOperationError() {
        updateState { copy(operationError = null) }
    }

    fun retrySettings() {
        observeSettings()
    }

    private fun observeSettings() {
        settingsObservation?.cancel()
        settingsObservation = viewModelScope.launch {
            getUserSettingsUseCase(Unit)
                .catch { error ->
                    updateState { copy(isLoadingSettings = false, settingsError = error) }
                }.collect { settings ->
                    updateState {
                        copy(
                            isLoadingSettings = false,
                            userSettings = settings,
                            settingsError = null,
                        )
                    }
                }
        }
    }

    private suspend fun runSettingsOperation(operation: suspend () -> Unit) {
        try {
            updateState { copy(operationError = null) }
            operation()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            updateState { copy(operationError = error) }
        }
    }

    private fun updateState(transform: SettingsUiState.() -> SettingsUiState) {
        mutableUiState.value = mutableUiState.value.transform()
    }
}

data object BalanceRequestFailedException : IllegalStateException()
