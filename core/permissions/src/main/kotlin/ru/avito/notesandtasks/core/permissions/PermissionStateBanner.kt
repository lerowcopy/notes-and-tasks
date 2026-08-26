package ru.avito.notesandtasks.core.permissions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import ru.avito.notesandtasks.core.permissions.R

@Composable
fun PermissionStateBanner(
    state: PermissionState,
    modifier: Modifier = Modifier,
) {
    if (state is PermissionState.PermanentlyDenied) {
        val context = LocalContext.current
        Card(
            modifier = modifier.fillMaxWidth(),
        ) {
            Column {
                Text(
                    text = stringResource(R.string.permission_settings_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(
                    onClick = { context.openApplicationSettings() },
                ) {
                    Text(text = stringResource(R.string.permission_open_settings))
                }
            }
        }
    }
}

private fun Context.openApplicationSettings() {
    startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        ),
    )
}
