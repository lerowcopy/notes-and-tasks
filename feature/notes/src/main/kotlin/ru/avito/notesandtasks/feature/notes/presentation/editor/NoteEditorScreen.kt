package ru.avito.notesandtasks.feature.notes.presentation.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.avito.notesandtasks.core.media.picker.rememberNoteImagePickerController
import ru.avito.notesandtasks.core.media.ui.NotePreviewImage
import ru.avito.notesandtasks.core.permissions.AppPermission
import ru.avito.notesandtasks.core.permissions.PermissionState
import ru.avito.notesandtasks.core.permissions.PermissionStateBanner
import ru.avito.notesandtasks.core.permissions.rememberPermissionRequestController
import ru.avito.notesandtasks.core.ui.components.LoadingIndicator
import ru.avito.notesandtasks.core.ui.theme.Spacing
import ru.avito.notesandtasks.feature.notes.R

@Composable
fun NoteEditorRoute(
    viewModel: NoteEditorViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.savedNoteId) {
        if (uiState.savedNoteId != null) {
            viewModel.onSavedNavigationHandled()
            onBack()
        }
    }
    NoteEditorScreen(
        uiState = uiState,
        onBack = onBack,
        onTitleChange = viewModel::onTitleChange,
        onTextChange = viewModel::onTextChange,
        onImageReady = viewModel::onImageReady,
        onRemoveImage = viewModel::removeImage,
        onStartVoiceRecording = viewModel::onStartVoiceRecording,
        onStopVoiceRecording = viewModel::onStopVoiceRecording,
        onSave = viewModel::onSave,
        onClearVoiceError = viewModel::clearVoiceError,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    uiState: NoteEditorUiState,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onTextChange: (String) -> Unit,
    onImageReady: (ru.avito.notesandtasks.core.common.result.OperationResult<String>) -> Unit,
    onRemoveImage: () -> Unit,
    onStartVoiceRecording: () -> Unit,
    onStopVoiceRecording: () -> Unit,
    onSave: () -> Unit,
    onClearVoiceError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val imagePicker = rememberNoteImagePickerController(onImageReady = onImageReady)
    val microphonePermission = rememberPermissionRequestController(AppPermission.RECORD_AUDIO)
    var isPhotoMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.note_editor_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.note_editor_back_content_description),
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        if (uiState.isLoading) {
            LoadingIndicator(modifier = Modifier.padding(contentPadding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(Spacing.large),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                PermissionStateBanner(state = imagePicker.cameraPermissionState)
                PermissionStateBanner(state = imagePicker.legacyImagePermissionState)
                PermissionStateBanner(state = microphonePermission.state)

                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(R.string.note_editor_title_label)) },
                    supportingText = if (uiState.isTitleError) {
                        { Text(text = stringResource(R.string.note_editor_title_required)) }
                    } else {
                        null
                    },
                    isError = uiState.isTitleError,
                    singleLine = true,
                )
                OutlinedTextField(
                    value = uiState.text,
                    onValueChange = onTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(R.string.note_editor_text_label)) },
                    minLines = 5,
                )

                if (uiState.imagePath != null) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.small),
                    ) {
                        NotePreviewImage(
                            path = uiState.imagePath,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(Spacing.huge * 4),
                        )
                        Button(onClick = onRemoveImage) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = null,
                            )
                            Text(
                                text = stringResource(R.string.note_editor_remove_photo),
                                modifier = Modifier.padding(start = Spacing.small),
                            )
                        }
                    }
                } else {
                    Button(onClick = { isPhotoMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Outlined.AddAPhoto,
                            contentDescription = null,
                        )
                        Text(
                            text = stringResource(R.string.note_editor_add_photo),
                            modifier = Modifier.padding(start = Spacing.small),
                        )
                    }
                    DropdownMenu(
                        expanded = isPhotoMenuExpanded,
                        onDismissRequest = { isPhotoMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.note_editor_choose_gallery)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Image,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                isPhotoMenuExpanded = false
                                imagePicker.pickImage()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.note_editor_use_camera)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.CameraAlt,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                isPhotoMenuExpanded = false
                                imagePicker.captureImage()
                            },
                        )
                    }
                }

                VoiceInputSection(
                    isRecording = uiState.isVoiceRecording,
                    isProcessing = uiState.isVoiceProcessing,
                    hasError = uiState.voiceError != null,
                    microphonePermissionState = microphonePermission.state,
                    onRequestMicrophonePermission = microphonePermission::request,
                    onStartRecording = onStartVoiceRecording,
                    onStopRecording = onStopVoiceRecording,
                    onClearError = onClearVoiceError,
                )

                if (uiState.saveError == NoteEditorSaveError.SaveFailed) {
                    Text(
                        text = stringResource(R.string.note_editor_save_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Button(
                    onClick = onSave,
                    enabled = !uiState.isSaving && !uiState.isVoiceProcessing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(Spacing.large),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(text = stringResource(R.string.note_editor_save))
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceInputSection(
    isRecording: Boolean,
    isProcessing: Boolean,
    hasError: Boolean,
    microphonePermissionState: PermissionState,
    onRequestMicrophonePermission: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onClearError: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        when {
            isProcessing -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                CircularProgressIndicator(modifier = Modifier.width(Spacing.large))
                Text(text = stringResource(R.string.note_editor_voice_processing))
            }

            isRecording -> Button(onClick = onStopRecording) {
                Icon(imageVector = Icons.Outlined.Stop, contentDescription = null)
                Text(
                    text = stringResource(R.string.note_editor_stop_recording),
                    modifier = Modifier.padding(start = Spacing.small),
                )
            }

            else -> Button(
                onClick = {
                    if (microphonePermissionState is PermissionState.Granted) {
                        onStartRecording()
                    } else {
                        onRequestMicrophonePermission()
                    }
                },
            ) {
                Icon(imageVector = Icons.Outlined.Mic, contentDescription = null)
                Text(
                    text = stringResource(R.string.note_editor_start_recording),
                    modifier = Modifier.padding(start = Spacing.small),
                )
            }
        }
        if (hasError) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                Text(
                    text = stringResource(R.string.note_editor_voice_error),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClearError) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.note_editor_clear_voice_error_content_description),
                    )
                }
            }
        }
    }
}
