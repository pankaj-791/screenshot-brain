package com.screenshotbrain.ui.reminders

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.screenshotbrain.database.ReminderWithInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    onBack: () -> Unit,
    onOpenScreenshot: (Long) -> Unit,
    viewModel: RemindersViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showCreate by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ReminderWithInfo?>(null) }
    var deleteTarget by remember { mutableStateOf<ReminderWithInfo?>(null) }
    var pendingSave by remember { mutableStateOf<(() -> Unit)?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Save the reminder whether or not permission was granted.
        // The ViewModel tells the user if notifications are off.
        pendingSave?.invoke()
        pendingSave = null
    }

    fun saveNew(title: String, note: String, remindAt: Long) {
        val action = { viewModel.create(title, note, remindAt) }
        val needsPermission = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pendingSave = action
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            action()
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Reminders") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("New") }
            )
        }
    ) { innerPadding ->
        when {
            !state.loaded -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            state.upcoming.isEmpty() && state.done.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            ) { EmptyReminders() }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(
                    start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "header_upcoming") { ReminderSection("Upcoming") }
                if (state.upcoming.isEmpty()) {
                    item(key = "none_upcoming") { Text("No upcoming reminders.") }
                }
                items(state.upcoming, key = { it.id }) { r ->
                    ReminderCard(
                        item = r,
                        onOpenScreenshot = onOpenScreenshot,
                        onEdit = { editing = r },
                        onMarkDone = { viewModel.markDone(r.id) },
                        onDelete = { deleteTarget = r }
                    )
                }
                if (state.done.isNotEmpty()) {
                    item(key = "header_done") { ReminderSection("Done") }
                    items(state.done, key = { it.id }) { r ->
                        ReminderCard(
                            item = r,
                            onOpenScreenshot = onOpenScreenshot,
                            onEdit = { editing = r },
                            onMarkDone = { },
                            onDelete = { deleteTarget = r }
                        )
                    }
                }
            }
        }
    }

    if (showCreate) {
        ReminderEditorDialog(
            heading = "New reminder",
            initialTitle = "",
            initialNote = "",
            initialRemindAt = defaultReminderTime(),
            onSave = { title, note, at ->
                showCreate = false
                saveNew(title, note, at)
            },
            onDismiss = { showCreate = false }
        )
    }

    editing?.let { target ->
        ReminderEditorDialog(
            heading = "Edit reminder",
            initialTitle = target.title,
            initialNote = target.note.orEmpty(),
            initialRemindAt = if (target.remindAt > System.currentTimeMillis()) {
                target.remindAt
            } else {
                defaultReminderTime()
            },
            onSave = { title, note, at ->
                editing = null
                viewModel.update(target.id, title, note, at, target.screenshotId)
            },
            onDismiss = { editing = null }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete this reminder?") },
            text = { Text("“${target.title}” will be removed. Its screenshot stays in your library.") },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    viewModel.delete(target.id)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}
