package ru.avito.notesandtasks.core.permissions

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver

class PermissionRequestController internal constructor(
    val state: PermissionState,
    private val requestPermission: () -> Unit,
) {
    fun request() {
        requestPermission()
    }
}

@Composable
fun rememberPermissionRequestController(
    permission: AppPermission,
): PermissionRequestController {
    val context = LocalContext.current
    val activity = context.findActivity()
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasRequested by rememberSaveable(permission) { mutableStateOf(false) }
    var state by remember(permission) {
        mutableStateOf(
            permissionStateFromSystem(
                context = context,
                activity = activity,
                permission = permission,
                hasRequested = hasRequested,
            ),
        )
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        hasRequested = true
        state = if (isGranted) {
            PermissionState.Granted
        } else {
            permissionStateFromSystem(
                context = context,
                activity = activity,
                permission = permission,
                hasRequested = true,
            )
        }
    }

    DisposableEffect(lifecycleOwner, context, activity, permission, hasRequested) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                state = permissionStateFromSystem(
                    context = context,
                    activity = activity,
                    permission = permission,
                    hasRequested = hasRequested,
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return remember(state, permission, launcher) {
        PermissionRequestController(
            state = state,
            requestPermission = {
                hasRequested = true
                launcher.launch(permission.androidPermission)
            },
        )
    }
}

private fun permissionStateFromSystem(
    context: Context,
    activity: Activity?,
    permission: AppPermission,
    hasRequested: Boolean,
): PermissionState = when {
    context.checkSelfPermission(permission.androidPermission) == PackageManager.PERMISSION_GRANTED -> {
        PermissionState.Granted
    }

    !hasRequested -> PermissionState.Denied
    activity?.shouldShowRequestPermissionRationale(permission.androidPermission) == true -> {
        PermissionState.Denied
    }

    else -> PermissionState.PermanentlyDenied
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
