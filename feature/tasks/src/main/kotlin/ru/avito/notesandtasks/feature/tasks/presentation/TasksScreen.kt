package ru.avito.notesandtasks.feature.tasks.presentation

import androidx.compose.foundation.layout.Arrangement
import java.io.IOException
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.avito.notesandtasks.core.common.flow.SortOrder
import ru.avito.notesandtasks.core.permissions.AppPermission
import ru.avito.notesandtasks.core.permissions.PermissionState
import ru.avito.notesandtasks.core.permissions.PermissionStateBanner
import ru.avito.notesandtasks.core.permissions.rememberPermissionRequestController
import ru.avito.notesandtasks.core.ui.components.EmptyStateView
import ru.avito.notesandtasks.core.ui.components.ErrorView
import ru.avito.notesandtasks.core.ui.components.LoadingIndicator
import ru.avito.notesandtasks.core.ui.components.SearchTopBar
import ru.avito.notesandtasks.core.ui.theme.Spacing
import ru.avito.notesandtasks.core.voice.auth.SaluteSpeechConfigurationException
import ru.avito.notesandtasks.feature.tasks.R
import ru.avito.notesandtasks.feature.tasks.domain.model.Task
import ru.avito.notesandtasks.feature.tasks.domain.model.TaskStatusFilter
import ru.avito.notesandtasks.feature.tasks.domain.usecase.VoiceTaskHttpException

@Composable
fun TasksRoute(
    viewModel: TasksViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TasksScreen(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onSearch = viewModel::onSearch,
        onFilterChange = viewModel::onFilterChange,
        onSortOrderChange = viewModel::onSortOrderChange,
        onFabMenuChange = viewModel::onFabMenuChange,
        onStartTextEntry = viewModel::onStartTextEntry,
        onTextEntryChange = viewModel::onTextEntryChange,
        onSubmitTextEntry = viewModel::onSubmitTextEntry,
        onToggleTaskStatus = viewModel::onToggleTaskStatus,
        onStartVoiceCreation = viewModel::onStartVoiceCreation,
        onStopVoiceCreation = viewModel::onStopVoiceCreation,
        onClearVoiceError = viewModel::clearVoiceError,
        onClearOperationError = viewModel::clearOperationError,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    uiState: TasksUiState,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onFilterChange: (TaskStatusFilter) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onFabMenuChange: (Boolean) -> Unit,
    onStartTextEntry: () -> Unit,
    onTextEntryChange: (String) -> Unit,
    onSubmitTextEntry: () -> Unit,
    onToggleTaskStatus: (Long, Boolean) -> Unit,
    onStartVoiceCreation: () -> Unit,
    onStopVoiceCreation: () -> Unit,
    onClearVoiceError: () -> Unit,
    onClearOperationError: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val microphonePermission = rememberPermissionRequestController(AppPermission.RECORD_AUDIO)
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.tasks_screen_title)) },
            )
        },
        floatingActionButton = {
            TaskCreationFabMenu(
                isExpanded = uiState.isFabMenuExpanded,
                onExpandedChange = onFabMenuChange,
                onTextCreate = onStartTextEntry,
                onVoiceCreate = {
                    if (microphonePermission.state is PermissionState.Granted) {
                        onStartVoiceCreation()
                    } else {
                        microphonePermission.request()
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            TasksControls(
                query = uiState.queryDraft,
                filter = uiState.filter,
                sortOrder = uiState.sortOrder,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                onFilterChange = onFilterChange,
                onSortOrderChange = onSortOrderChange,
            )
            PermissionStateBanner(
                state = microphonePermission.state,
                modifier = Modifier.padding(horizontal = Spacing.large),
            )
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> LoadingIndicator()
                    uiState.loadError != null -> ErrorView(
                        message = stringResource(R.string.tasks_load_error),
                        onRetry = onRetry,
                    )

                    uiState.tasks.isEmpty() && !uiState.isTextEntryVisible -> EmptyStateView(
                        text = stringResource(
                            if (uiState.submittedQuery.isBlank()) {
                                R.string.tasks_empty
                            } else {
                                R.string.tasks_empty_search
                            },
                        ),
                    )

                    else -> TasksList(
                        uiState = uiState,
                        onTextEntryChange = onTextEntryChange,
                        onSubmitTextEntry = onSubmitTextEntry,
                        onToggleTaskStatus = onToggleTaskStatus,
                    )
                }
                TaskFeedbackOverlay(
                    voiceState = uiState.voiceState,
                    operationError = uiState.operationError,
                    onStopVoiceCreation = onStopVoiceCreation,
                    onClearVoiceError = onClearVoiceError,
                    onClearOperationError = onClearOperationError,
                )
            }
        }
    }
}

@Composable
private fun TasksControls(
    query: String,
    filter: TaskStatusFilter,
    sortOrder: SortOrder,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onFilterChange: (TaskStatusFilter) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        SearchTopBar(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusFilterSegments(
                filter = filter,
                onFilterChange = onFilterChange,
                modifier = Modifier.weight(1f),
            )
            SortMenu(
                sortOrder = sortOrder,
                onSortOrderChange = onSortOrderChange,
            )
        }
    }
}

@Composable
private fun StatusFilterSegments(
    filter: TaskStatusFilter,
    onFilterChange: (TaskStatusFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        TaskStatusFilter.entries.forEachIndexed { index, item ->
            SegmentedButton(
                selected = filter == item,
                onClick = { onFilterChange(item) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = TaskStatusFilter.entries.size,
                ),
                label = { Text(text = stringResource(item.stringResource())) },
            )
        }
    }
}

@Composable
private fun SortMenu(
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Sort,
            contentDescription = stringResource(R.string.tasks_sort_content_description),
        )
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
    ) {
        SortOrder.entries.forEach { item ->
            DropdownMenuItem(
                text = { Text(text = stringResource(item.stringResource())) },
                onClick = {
                    expanded = false
                    if (item != sortOrder) {
                        onSortOrderChange(item)
                    }
                },
            )
        }
    }
}

@Composable
private fun TasksList(
    uiState: TasksUiState,
    onTextEntryChange: (String) -> Unit,
    onSubmitTextEntry: () -> Unit,
    onToggleTaskStatus: (Long, Boolean) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        if (uiState.isTextEntryVisible) {
            item(key = "task_text_entry") {
                TaskTextEntry(
                    text = uiState.textEntry,
                    isSaving = uiState.isTextEntrySaving,
                    isError = uiState.isTextEntryError,
                    onTextChange = onTextEntryChange,
                    onSubmit = onSubmitTextEntry,
                )
            }
        }
        items(
            items = uiState.tasks,
            key = Task::id,
        ) { task ->
            TaskListItem(task = task, onToggle = onToggleTaskStatus)
        }
    }
}

@Composable
private fun TaskTextEntry(
    text: String,
    isSaving: Boolean,
    isError: Boolean,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        label = { Text(text = stringResource(R.string.tasks_text_entry_label)) },
        supportingText = if (isError) {
            { Text(text = stringResource(R.string.tasks_text_entry_error)) }
        } else {
            null
        },
        isError = isError,
        enabled = !isSaving,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        trailingIcon = {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.width(Spacing.large))
            } else {
                IconButton(onClick = onSubmit) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = stringResource(R.string.tasks_save_content_description),
                    )
                }
            }
        },
    )
}

@Composable
private fun TaskListItem(
    task: Task,
    onToggle: (Long, Boolean) -> Unit,
) {
    val contentAlpha = if (task.isCompleted) {
        COMPLETED_TASK_ALPHA
    } else {
        ACTIVE_TASK_ALPHA
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(contentAlpha),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.large),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { isChecked -> onToggle(task.id, isChecked) },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        if (task.isCompleted) {
                            R.string.tasks_status_completed
                        } else {
                            R.string.tasks_status_active
                        },
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TaskCreationFabMenu(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onTextCreate: () -> Unit,
    onVoiceCreate: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        if (isExpanded) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                FabActionLabel(text = stringResource(R.string.tasks_create_voice))
                SmallFloatingActionButton(onClick = onVoiceCreate) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = stringResource(R.string.tasks_create_voice_content_description),
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                FabActionLabel(text = stringResource(R.string.tasks_create_text))
                SmallFloatingActionButton(onClick = onTextCreate) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.tasks_create_text_content_description),
                    )
                }
            }
        }
        FloatingActionButton(onClick = { onExpandedChange(!isExpanded) }) {
            Icon(
                imageVector = if (isExpanded) Icons.Outlined.Close else Icons.Outlined.Add,
                contentDescription = stringResource(
                    if (isExpanded) {
                        R.string.tasks_fab_close_content_description
                    } else {
                        R.string.tasks_fab_open_content_description
                    },
                ),
            )
        }
    }
}

@Composable
private fun FabActionLabel(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = Spacing.medium, vertical = Spacing.small),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun BoxScope.TaskFeedbackOverlay(
    voiceState: VoiceTaskState,
    operationError: Throwable?,
    onStopVoiceCreation: () -> Unit,
    onClearVoiceError: () -> Unit,
    onClearOperationError: () -> Unit,
) {
    when (voiceState) {
        VoiceTaskState.Idle -> if (operationError != null) {
            TaskErrorSurface(
                modifier = Modifier.align(Alignment.BottomCenter),
                message = stringResource(R.string.tasks_operation_error),
                onDismiss = onClearOperationError,
            )
        }

        VoiceTaskState.Recording -> TaskVoiceSurface(
            modifier = Modifier.align(Alignment.BottomCenter),
            text = stringResource(R.string.tasks_voice_recording),
            icon = Icons.Outlined.Stop,
            actionDescription = stringResource(R.string.tasks_stop_voice_content_description),
            onAction = onStopVoiceCreation,
        )

        VoiceTaskState.Processing -> TaskVoiceSurface(
            modifier = Modifier.align(Alignment.BottomCenter),
            text = stringResource(R.string.tasks_voice_processing),
            icon = null,
            actionDescription = null,
            onAction = null,
        )

        is VoiceTaskState.Error -> TaskErrorSurface(
            modifier = Modifier.align(Alignment.BottomCenter),
            message = voiceErrorMessage(voiceState.cause),
            onDismiss = onClearVoiceError,
        )
    }
}

@Composable
private fun voiceErrorMessage(cause: Throwable): String = when (cause) {
    SaluteSpeechConfigurationException -> stringResource(R.string.tasks_voice_configuration_error)
    is VoiceTaskHttpException -> stringResource(R.string.tasks_voice_http_error, cause.code)
    is IOException -> stringResource(R.string.tasks_voice_network_error)
    else -> stringResource(R.string.tasks_voice_error)
}

@Composable
private fun TaskVoiceSurface(
    modifier: Modifier,
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    actionDescription: String?,
    onAction: (() -> Unit)?,
) {
    Surface(
        modifier = modifier.padding(Spacing.large),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
        shadowElevation = Spacing.small,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.large),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            if (onAction == null) {
                CircularProgressIndicator(modifier = Modifier.width(Spacing.large))
            }
            Text(text = text, modifier = Modifier.weight(1f))
            if (icon != null && actionDescription != null && onAction != null) {
                IconButton(onClick = onAction) {
                    Icon(imageVector = icon, contentDescription = actionDescription)
                }
            }
        }
    }
}

@Composable
private fun TaskErrorSurface(
    modifier: Modifier,
    message: String,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = modifier.padding(Spacing.large),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
        shadowElevation = Spacing.small,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.large),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            IconButton(
                onClick = onDismiss,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.tasks_dismiss_error_content_description),
                )
            }
        }
    }
}

private fun TaskStatusFilter.stringResource(): Int = when (this) {
    TaskStatusFilter.All -> R.string.tasks_filter_all
    TaskStatusFilter.Active -> R.string.tasks_filter_active
    TaskStatusFilter.Completed -> R.string.tasks_filter_completed
}

private fun SortOrder.stringResource(): Int = when (this) {
    SortOrder.NEWEST_FIRST -> R.string.tasks_sort_newest
    SortOrder.OLDEST_FIRST -> R.string.tasks_sort_oldest
}

private const val ACTIVE_TASK_ALPHA = 1f
private const val COMPLETED_TASK_ALPHA = 0.6f
