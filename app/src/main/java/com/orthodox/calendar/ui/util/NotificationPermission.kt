package com.orthodox.calendar.ui.util

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Returns a function that asks for permission to post notifications (API 33+)
 * and then calls [onDone] whatever the answer. Asked only when there is
 * something to remind about — a slava just set — never at launch; below API 33
 * there is nothing to ask and [onDone] runs at once.
 */
@Composable
fun rememberNotificationPermissionRequest(onDone: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val done by rememberUpdatedState(onDone)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        done()
    }
    return {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            runCatching { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                .onFailure { done() }
        } else {
            done()
        }
    }
}
