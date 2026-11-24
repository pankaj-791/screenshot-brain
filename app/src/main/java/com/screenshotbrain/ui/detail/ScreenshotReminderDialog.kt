package com.screenshotbrain.ui.detail

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.screenshotbrain.database.ScreenshotEntity
import com.screenshotbrain.domain.DateSuggester
import com.screenshotbrain.ui.reminders.ReminderEditorDialog
import com.screenshotbrain.ui.reminders.defaultReminderTime
import java.time.Instant
import java.time.ZoneId

@Composable
fun ScreenshotReminderDialog(
    screenshot: ScreenshotEntity,
    onSave: (title: String, note: String, remindAt: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val suggestions = remember(screenshot.ocrText, screenshot.takenAt, screenshot.importedAt) {
        val reference = Instant.ofEpochMilli(screenshot.takenAt ?: screenshot.importedAt)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        DateSuggester.suggest(screenshot.ocrText, reference = reference)
    }
    var pendingSave by remember { mutableStateOf<(() -> Unit)?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Save the reminder whether or not permission was granted.
        // The ViewModel tells the user if notifications are off.
        pendingSave?.invoke()
        pendingSave = null
    }

    ReminderEditorDialog(
        heading = "Remind me about this",
        initialTitle = screenshot.title.orEmpty(),
        initialNote = "",
        initialRemindAt = defaultReminderTime(),
        suggestions = suggestions,
        onSave = { title, note, at ->
            val finish = {
                onSave(title, note, at)
                onDismiss()
            }
            val needsPermission = Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            if (needsPermission) {
                pendingSave = finish
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                finish()
            }
        },
        onDismiss = onDismiss
    )
}
