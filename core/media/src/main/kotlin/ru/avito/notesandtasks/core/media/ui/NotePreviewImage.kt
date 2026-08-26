package ru.avito.notesandtasks.core.media.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import java.io.File

@Composable
fun NotePreviewImage(
    path: String?,
    modifier: Modifier = Modifier,
    placeholder: @Composable (Modifier) -> Unit = { placeholderModifier ->
        ImagePlaceholder(modifier = placeholderModifier)
    },
) {
    val imageFile = path
        ?.let(::File)
        ?.takeIf(File::isFile)
    var failedToLoad by remember(path) { mutableStateOf(false) }

    if (imageFile == null || failedToLoad) {
        placeholder(modifier)
    } else {
        AsyncImage(
            model = imageFile,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
            onError = { failedToLoad = true },
        )
    }
}

@Composable
private fun ImagePlaceholder(
    modifier: Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.ImageNotSupported,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
