package ru.avito.notesandtasks.feature.notes.presentation.list

import java.text.DateFormat
import java.util.Date
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.avito.notesandtasks.core.common.flow.SortOrder
import ru.avito.notesandtasks.core.media.ui.NotePreviewImage
import ru.avito.notesandtasks.core.ui.components.EmptyStateView
import ru.avito.notesandtasks.core.ui.components.ErrorView
import ru.avito.notesandtasks.core.ui.components.LoadingIndicator
import ru.avito.notesandtasks.core.ui.components.SearchTopBar
import ru.avito.notesandtasks.core.ui.theme.Spacing
import ru.avito.notesandtasks.feature.notes.R
import ru.avito.notesandtasks.feature.notes.domain.model.Note

@Composable
fun NotesListRoute(
    viewModel: NotesListViewModel,
    onCreateNote: () -> Unit,
    onOpenNote: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    NotesListScreen(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onSearch = viewModel::onSearch,
        onSortOrderChange = viewModel::onSortOrderChange,
        onDeleteModeChange = viewModel::onDeleteModeChange,
        onCreateNote = onCreateNote,
        onOpenNote = onOpenNote,
        onDeleteNote = viewModel::delete,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
fun NotesListScreen(
    uiState: NotesListUiState,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onDeleteModeChange: (Boolean) -> Unit,
    onCreateNote: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            NotesListTopBar(
                query = uiState.queryDraft,
                sortOrder = uiState.sortOrder,
                isDeleteMode = uiState.isDeleteMode,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                onSortOrderChange = onSortOrderChange,
                onDeleteModeChange = onDeleteModeChange,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateNote) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.notes_create_content_description),
                )
            }
        },
    ) { contentPadding ->
        when {
            uiState.isLoading -> LoadingIndicator(modifier = Modifier.padding(contentPadding))
            uiState.error != null -> ErrorView(
                message = stringResource(R.string.notes_load_error),
                onRetry = onRetry,
                modifier = Modifier.padding(contentPadding),
            )

            uiState.notes.isEmpty() -> EmptyStateView(
                text = stringResource(
                    if (uiState.submittedQuery.isBlank()) {
                        R.string.notes_empty
                    } else {
                        R.string.notes_empty_search
                    },
                ),
                modifier = Modifier.padding(contentPadding),
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Spacing.large,
                    top = contentPadding.calculateTopPadding() + Spacing.large,
                    end = Spacing.large,
                    bottom = contentPadding.calculateBottomPadding() + Spacing.large,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                items(
                    items = uiState.notes,
                    key = Note::id,
                ) { note ->
                    NoteListItem(
                        note = note,
                        isDeleteMode = uiState.isDeleteMode,
                        onOpen = onOpenNote,
                        onDelete = onDeleteNote,
                    )
                }
            }
        }
    }
}

@Composable
private fun NotesListTopBar(
    query: String,
    sortOrder: SortOrder,
    isDeleteMode: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onDeleteModeChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.large),
    ) {
        SearchTopBar(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SortMenu(
                sortOrder = sortOrder,
                onSortOrderChange = onSortOrderChange,
            )
            IconButton(onClick = { onDeleteModeChange(!isDeleteMode) }) {
                Icon(
                    imageVector = if (isDeleteMode) Icons.Outlined.Check else Icons.Outlined.Delete,
                    contentDescription = stringResource(
                        if (isDeleteMode) {
                            R.string.notes_delete_mode_done_content_description
                        } else {
                            R.string.notes_delete_mode_content_description
                        },
                    ),
                )
            }
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
            contentDescription = stringResource(R.string.notes_sort_content_description),
        )
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
    ) {
        SortOrder.entries.forEach { order ->
            DropdownMenuItem(
                text = { Text(text = stringResource(order.stringResource())) },
                onClick = {
                    expanded = false
                    if (order != sortOrder) {
                        onSortOrderChange(order)
                    }
                },
            )
        }
    }
}

@Composable
private fun NoteListItem(
    note: Note,
    isDeleteMode: Boolean,
    onOpen: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val itemModifier = if (isDeleteMode) {
        Modifier
    } else {
        Modifier.clickable { onOpen(note.id) }
    }
    Card(
        modifier = itemModifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.large),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NotePreviewImage(
                path = note.imagePath,
                modifier = Modifier.size(Spacing.huge),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
            ) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = note.text.ifBlank { stringResource(R.string.notes_empty_body) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatCreatedAt(note.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isDeleteMode) {
                IconButton(onClick = { onDelete(note.id) }) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.notes_delete_content_description),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

private fun SortOrder.stringResource(): Int = when (this) {
    SortOrder.NEWEST_FIRST -> R.string.notes_sort_newest
    SortOrder.OLDEST_FIRST -> R.string.notes_sort_oldest
}

private fun formatCreatedAt(timestamp: Long): String = DateFormat.getDateInstance().format(Date(timestamp))
