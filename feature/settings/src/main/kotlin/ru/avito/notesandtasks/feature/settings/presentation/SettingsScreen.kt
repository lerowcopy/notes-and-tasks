package ru.avito.notesandtasks.feature.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.shape.CircleShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.avito.notesandtasks.core.common.settings.AccentColor
import ru.avito.notesandtasks.core.common.settings.ThemeMode
import ru.avito.notesandtasks.core.datastore.UserSettings
import ru.avito.notesandtasks.core.ui.components.ErrorView
import ru.avito.notesandtasks.core.ui.components.LoadingIndicator
import ru.avito.notesandtasks.core.ui.theme.Spacing
import ru.avito.notesandtasks.core.ui.theme.colorScheme
import ru.avito.notesandtasks.feature.settings.R

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        uiState = uiState,
        onThemeModeChange = viewModel::onThemeModeChange,
        onAccentColorChange = viewModel::onAccentColorChange,
        onResetSettings = viewModel::onResetSettings,
        onRefreshBalance = viewModel::refreshBalance,
        onRetrySettings = viewModel::retrySettings,
        onClearOperationError = viewModel::clearOperationError,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentColorChange: (AccentColor) -> Unit,
    onResetSettings: () -> Unit,
    onRefreshBalance: () -> Unit,
    onRetrySettings: () -> Unit,
    onClearOperationError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_screen_title)) },
            )
        },
    ) { contentPadding ->
        when {
            uiState.isLoadingSettings -> LoadingIndicator(modifier = Modifier.padding(contentPadding))
            uiState.settingsError != null -> ErrorView(
                message = stringResource(R.string.settings_load_error),
                onRetry = onRetrySettings,
                modifier = Modifier.padding(contentPadding),
            )

            else -> SettingsContent(
                uiState = uiState,
                onThemeModeChange = onThemeModeChange,
                onAccentColorChange = onAccentColorChange,
                onResetSettings = onResetSettings,
                onRefreshBalance = onRefreshBalance,
                onClearOperationError = onClearOperationError,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        }
    }
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentColorChange: (AccentColor) -> Unit,
    onResetSettings: () -> Unit,
    onRefreshBalance: () -> Unit,
    onClearOperationError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Spacing.large),
    ) {
        BalanceCard(
            balanceState = uiState.balanceState,
            onRefresh = onRefreshBalance,
        )
        ThemeSection(
            selectedThemeMode = uiState.userSettings.themeMode,
            onThemeModeChange = onThemeModeChange,
        )
        AccentColorSection(
            selectedAccentColor = uiState.userSettings.accentColor,
            onAccentColorChange = onAccentColorChange,
        )
        Button(
            onClick = onResetSettings,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.settings_reset))
        }
        if (uiState.operationError != null) {
            OperationErrorCard(onDismiss = onClearOperationError)
        }
    }
}

@Composable
private fun BalanceCard(
    balanceState: BalanceUiState,
    onRefresh: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            Text(
                text = stringResource(R.string.settings_balance_title),
                style = MaterialTheme.typography.titleMedium,
            )
            when (balanceState) {
                BalanceUiState.Idle -> {
                    Text(
                        text = stringResource(R.string.settings_balance_idle),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = onRefresh) {
                        Text(text = stringResource(R.string.settings_balance_refresh))
                    }
                }

                BalanceUiState.Loading -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(Spacing.large))
                    Text(text = stringResource(R.string.settings_balance_loading))
                }

                is BalanceUiState.Success -> {
                    if (balanceState.entries.isEmpty()) {
                        Text(
                            text = stringResource(R.string.settings_balance_empty),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        balanceState.entries.forEach { entry ->
                            Text(
                                text = stringResource(
                                    R.string.settings_balance_entry,
                                    entry.usage,
                                    entry.value,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    TextButton(onClick = onRefresh) {
                        Text(text = stringResource(R.string.settings_balance_refresh))
                    }
                }

                is BalanceUiState.Error -> {
                    Text(
                        text = stringResource(R.string.settings_balance_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = onRefresh) {
                        Text(text = stringResource(R.string.settings_balance_retry))
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSection(
    selectedThemeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        Text(
            text = stringResource(R.string.settings_theme_title),
            style = MaterialTheme.typography.titleMedium,
        )
        ThemeMode.entries.forEach { themeMode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onThemeModeChange(themeMode) }
                    .padding(vertical = Spacing.extraSmall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                RadioButton(
                    selected = themeMode == selectedThemeMode,
                    onClick = { onThemeModeChange(themeMode) },
                )
                Text(text = stringResource(themeMode.stringResource()))
            }
        }
    }
}

@Composable
private fun AccentColorSection(
    selectedAccentColor: AccentColor,
    onAccentColorChange: (AccentColor) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        Text(
            text = stringResource(R.string.settings_accent_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            AccentColor.entries.forEach { accentColor ->
                AccentColorSwatch(
                    accentColor = accentColor,
                    isSelected = accentColor == selectedAccentColor,
                    onClick = { onAccentColorChange(accentColor) },
                )
            }
        }
    }
}

@Composable
private fun AccentColorSwatch(
    accentColor: AccentColor,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colorScheme = accentColor.colorScheme(isDarkTheme = false)
    Surface(
        modifier = Modifier
            .size(Spacing.extraLarge)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = colorScheme.primary,
        border = if (isSelected) {
            BorderStroke(Spacing.extraSmall, MaterialTheme.colorScheme.onSurface)
        } else {
            null
        },
    ) {}
}

@Composable
private fun OperationErrorCard(
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.large),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            Text(
                text = stringResource(R.string.settings_operation_error),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.error,
            )
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.settings_dismiss_error))
            }
        }
    }
}

private fun ThemeMode.stringResource(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
}
