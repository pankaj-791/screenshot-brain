package com.screenshotbrain.ui.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private const val POLICY_TEXT =
    "Screenshot Brain keeps everything it creates on this phone: the list of screenshots " +
        "you chose, the text it read from them, titles, notes, collections and reminders.\n\n" +
        "It reads only the screenshots you pick with the photo picker. It does not upload " +
        "your screenshots or their text anywhere. If an online AI feature is added in the " +
        "future, it will ask for your permission before sending anything.\n\n" +
        "You can export your data, or delete all of it, from Settings. Deleting never " +
        "touches your original photos.\n\n" +
        "This is a plain-language summary, not a legal document. A full privacy policy " +
        "page will be published before any public release."

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showDelete by remember { mutableStateOf(false) }
    var showPolicy by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) viewModel.exportTo(uri) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)

            SettingsSection("Appearance") {
                Text("Theme", style = MaterialTheme.typography.titleSmall)
                ThemeSelector(state.settings.themeMode) { viewModel.setThemeMode(it) }
                Text("Accent color", style = MaterialTheme.typography.titleSmall)
                AccentPicker(state.settings.accent) { viewModel.setAccent(it) }
            }

            SettingsSection("Notifications") {
                SettingsRow(
                    title = if (state.notificationsEnabled) {
                        "Notifications are on"
                    } else {
                        "Notifications are off"
                    },
                    subtitle = if (state.notificationsEnabled) {
                        "Reminders can alert you."
                    } else {
                        "Reminders are saved, but they cannot alert you."
                    }
                )
                OutlinedButton(onClick = { openNotificationSettings(context) }) {
                    Text("Open Android notification settings")
                }
            }

            SettingsSection("Privacy") {
                SettingsRow(
                    "Stays on your phone",
                    "Screenshots are read on this device. This version has no feature " +
                        "that uploads your screenshots or text."
                )
                SettingsRow(
                    "Minimal access",
                    "The app sees only the screenshots you pick with the photo picker. " +
                        "The only permission it asks you for is notifications."
                )
                SettingsRow(
                    "AI Brain",
                    "Search runs on your phone. Conversations are kept only while the " +
                        "screen is open and are never saved."
                )
                SettingsRow(
                    "Privacy policy",
                    "Read how your data is handled",
                    onClick = { showPolicy = true }
                )
            }

            SettingsSection("Storage") { StorageSummary(state.stats) }

            SettingsSection("Your data") {
                Button(
                    onClick = { exportLauncher.launch("screenshot-brain-export.json") },
                    enabled = !state.busy
                ) { Text("Export data") }
                Text(
                    "Saves titles, notes, extracted text, collections and reminders as one " +
                        "file. Images are not included.",
                    style = MaterialTheme.typography.bodySmall
                )
                Button(
                    onClick = { showDelete = true },
                    enabled = !state.busy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Delete all app data") }
            }

            SettingsSection("About") {
                SettingsRow("Screenshot Brain", "Version ${viewModel.versionName}")
                SettingsRow("Your screenshots. Your memories. One intelligent brain.")
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete all app data?") },
            text = {
                Text(
                    "This permanently removes everything Screenshot Brain stores: the list of " +
                        "screenshots, extracted text, titles, notes, collections, reminders " +
                        "and settings. Your original photos are not touched. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    viewModel.deleteAll()
                }) { Text("Delete everything") }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } }
        )
    }

    if (showPolicy) {
        AlertDialog(
            onDismissRequest = { showPolicy = false },
            title = { Text("Privacy policy") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(POLICY_TEXT)
                }
            },
            confirmButton = { TextButton(onClick = { showPolicy = false }) { Text("Close") } }
        )
    }
}

private fun openNotificationSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        // No settings screen available. Nothing else to do.
    }
}
