package ru.avito.notesandtasks.core.permissions

import android.Manifest
import android.os.Build

sealed interface PermissionState {
    data object Granted : PermissionState
    data object Denied : PermissionState
    data object PermanentlyDenied : PermissionState
}

enum class AppPermission(
    val androidPermission: String,
) {
    CAMERA(Manifest.permission.CAMERA),
    RECORD_AUDIO(Manifest.permission.RECORD_AUDIO),
    READ_MEDIA_IMAGES(Manifest.permission.READ_MEDIA_IMAGES),
    READ_EXTERNAL_STORAGE(Manifest.permission.READ_EXTERNAL_STORAGE),
}

sealed interface ImageAccessRequirement {
    data object PhotoPicker : ImageAccessRequirement

    data class RuntimePermission(
        val permission: AppPermission,
    ) : ImageAccessRequirement
}

/**
 * Photo Picker не требует runtime-разрешения. Если он недоступен, legacy-галерея использует
 * READ_MEDIA_IMAGES на API 33+ и READ_EXTERNAL_STORAGE на более ранних версиях Android.
 */
fun imageAccessRequirement(
    isPhotoPickerAvailable: Boolean,
    sdkInt: Int = Build.VERSION.SDK_INT,
): ImageAccessRequirement = if (isPhotoPickerAvailable) {
    ImageAccessRequirement.PhotoPicker
} else if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
    ImageAccessRequirement.RuntimePermission(AppPermission.READ_MEDIA_IMAGES)
} else {
    ImageAccessRequirement.RuntimePermission(AppPermission.READ_EXTERNAL_STORAGE)
}
