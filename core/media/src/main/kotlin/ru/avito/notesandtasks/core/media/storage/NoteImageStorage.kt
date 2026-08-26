package ru.avito.notesandtasks.core.media.storage

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileNotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.avito.notesandtasks.core.common.result.OperationResult

private const val NOTE_IMAGES_DIRECTORY = "note_images"
private const val IMAGE_FILE_PREFIX = "note_image_"
private const val DEFAULT_IMAGE_EXTENSION = "jpg"

data class CameraCaptureTarget(
    val uri: Uri,
    val absolutePath: String,
)

class NoteImageStorage(
    private val context: Context,
) {
    fun createCameraCaptureTarget(): CameraCaptureTarget {
        val imageFile = createImageFile(extension = DEFAULT_IMAGE_EXTENSION)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile,
        )
        return CameraCaptureTarget(
            uri = uri,
            absolutePath = imageFile.absolutePath,
        )
    }

    suspend fun persistPickedImage(sourceUri: Uri): OperationResult<String> =
        withContext(Dispatchers.IO) {
            val result: OperationResult<String> = runCatching {
                val imageFile = createImageFile(extension = sourceUri.imageExtension())
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    imageFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: throw FileNotFoundException()
                imageFile.absolutePath
            }.fold(
                onSuccess = { path -> OperationResult.Success(path) },
                onFailure = { cause -> OperationResult.Error(cause = cause) },
            )
            result
        }

    fun discardCameraCapture(target: CameraCaptureTarget) {
        File(target.absolutePath).delete()
    }

    private fun createImageFile(extension: String): File {
        val imageDirectory = File(context.filesDir, NOTE_IMAGES_DIRECTORY).apply {
            mkdirs()
        }
        return File.createTempFile(
            IMAGE_FILE_PREFIX,
            ".${extension}",
            imageDirectory,
        )
    }

    private fun Uri.imageExtension(): String = context.contentResolver
        .getType(this)
        ?.let(MimeTypeMap.getSingleton()::getExtensionFromMimeType)
        ?: displayNameExtension()
        ?: DEFAULT_IMAGE_EXTENSION

    private fun Uri.displayNameExtension(): String? = context.contentResolver.query(
        this,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null,
    )?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index == -1 || !cursor.moveToFirst()) {
            null
        } else {
            cursor.getString(index)
                .substringAfterLast('.', missingDelimiterValue = "")
                .takeIf(String::isNotBlank)
        }
    }
}
