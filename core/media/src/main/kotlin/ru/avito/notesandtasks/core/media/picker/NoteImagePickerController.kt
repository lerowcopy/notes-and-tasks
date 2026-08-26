package ru.avito.notesandtasks.core.media.picker

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import ru.avito.notesandtasks.core.common.result.OperationResult
import ru.avito.notesandtasks.core.media.storage.CameraCaptureTarget
import ru.avito.notesandtasks.core.media.storage.NoteImageStorage
import ru.avito.notesandtasks.core.permissions.AppPermission
import ru.avito.notesandtasks.core.permissions.ImageAccessRequirement
import ru.avito.notesandtasks.core.permissions.PermissionRequestController
import ru.avito.notesandtasks.core.permissions.PermissionState
import ru.avito.notesandtasks.core.permissions.imageAccessRequirement
import ru.avito.notesandtasks.core.permissions.rememberPermissionRequestController

class NoteImagePickerController internal constructor(
    val cameraPermissionState: PermissionState,
    val legacyImagePermissionState: PermissionState,
    val imageAccessRequirement: ImageAccessRequirement,
    private val requestCameraPermission: () -> Unit,
    private val requestLegacyImagePermission: () -> Unit,
    private val launchPhotoPicker: () -> Unit,
    private val launchLegacyGallery: () -> Unit,
    private val launchCamera: () -> Unit,
) {
    fun pickImage() {
        when (imageAccessRequirement) {
            ImageAccessRequirement.PhotoPicker -> launchPhotoPicker()
            is ImageAccessRequirement.RuntimePermission -> {
                if (legacyImagePermissionState is PermissionState.Granted) {
                    launchLegacyGallery()
                } else {
                    requestLegacyImagePermission()
                }
            }
        }
    }

    fun captureImage() {
        if (cameraPermissionState is PermissionState.Granted) {
            launchCamera()
        } else {
            requestCameraPermission()
        }
    }

    fun requestCameraPermission() {
        requestCameraPermission.invoke()
    }

    fun requestLegacyImagePermission() {
        requestLegacyImagePermission.invoke()
    }
}

@Composable
fun rememberNoteImagePickerController(
    onImageReady: (OperationResult<String>) -> Unit,
): NoteImagePickerController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val storage = remember(context) { NoteImageStorage(context) }
    val cameraPermissionController = rememberPermissionRequestController(AppPermission.CAMERA)
    val legacyImagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        AppPermission.READ_MEDIA_IMAGES
    } else {
        AppPermission.READ_EXTERNAL_STORAGE
    }
    val legacyImagePermissionController = rememberPermissionRequestController(legacyImagePermission)
    val isPhotoPickerAvailable = ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)
    val accessRequirement = imageAccessRequirement(isPhotoPickerAvailable)
    var pendingCameraTarget by remember { mutableStateOf<CameraCaptureTarget?>(null) }

    fun persistImage(uri: Uri) {
        scope.launch {
            onImageReady(storage.persistPickedImage(uri))
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let(::persistImage)
    }
    val legacyGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let(::persistImage)
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { isSuccess ->
        pendingCameraTarget?.let { target ->
            if (isSuccess) {
                onImageReady(OperationResult.Success(target.absolutePath))
            } else {
                storage.discardCameraCapture(target)
            }
        }
        pendingCameraTarget = null
    }

    return remember(
        accessRequirement,
        cameraPermissionController,
        legacyImagePermissionController,
        photoPickerLauncher,
        legacyGalleryLauncher,
        cameraLauncher,
    ) {
        NoteImagePickerController(
            cameraPermissionState = cameraPermissionController.state,
            legacyImagePermissionState = legacyImagePermissionController.state,
            imageAccessRequirement = accessRequirement,
            requestCameraPermission = cameraPermissionController::request,
            requestLegacyImagePermission = legacyImagePermissionController::request,
            launchPhotoPicker = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            launchLegacyGallery = {
                legacyGalleryLauncher.launch("image/*")
            },
            launchCamera = {
                val target = storage.createCameraCaptureTarget()
                pendingCameraTarget = target
                cameraLauncher.launch(target.uri)
            },
        )
    }
}
