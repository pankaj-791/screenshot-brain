package com.screenshotbrain.ui.cleanup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanupScreen(
    onBack: () -> Unit,
    onOpenScreenshot: (Long) -> Unit,
    viewModel: CleanupViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirm by remember { mutableStateOf(false) }
    val s = state.suggestions
    val toggle: (Long) -> Unit = { viewModel.toggle(it) }

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
                title = { Text("Smart cleanup") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (state.selected.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)
                ) {
                    OutlinedButton(onClick = { viewModel.clearSelection() }) { Text("Clear") }
                    Button(
                        onClick = { confirm = true },
                        enabled = !state.busy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.weight(1f)
                    ) { Text("Remove ${state.selected.size} from app") }
                }
            }
        }
    ) { inner ->
        if (!state.loaded) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { CleanupIntroCard() }
                if (state.unanalyzed > 0) item { AnalysisProgress(state.unanalyzed) }
                if (s.isEmpty && state.unanalyzed == 0) {
                    item { Text("Nothing to clean up right now. Your library looks tidy.") }
                }
                groupSection(
                    "dup", "Exact duplicates", "Identical files. Each group is one set of copies.", "Copies",
                    s.duplicateGroups, state.selected, toggle, onOpenScreenshot
                ) { viewModel.select(it) }
                groupSection(
                    "sim", "Similar", "Look alike and share most of their words.", "Similar",
                    s.similarGroups, state.selected, toggle, onOpenScreenshot
                ) { viewModel.select(it) }
                listSection(
                    "blur", "Possibly blurry", "A rough estimate. Check before removing.",
                    s.blurry, state.selected, toggle, onOpenScreenshot,
                    { "Sharpness " + String.format(Locale.US, "%.0f", it.sharpness ?: 0.0) }
                )
                listSection(
                    "old", "Old", "Older than 6 months, with no favourite, collection, note or reminder.",
                    s.old, state.selected, toggle, onOpenScreenshot, { dateText(it.takenAt ?: it.importedAt) }
                )
                listSection(
                    "unused", "Unused", "Imported over 30 days ago and never organized.",
                    s.unused, state.selected, toggle, onOpenScreenshot, { "Imported " + dateText(it.importedAt) }
                )
                listSection(
                    "gone", "Unavailable", "The image can no longer be opened.",
                    s.unavailable, state.selected, toggle, onOpenScreenshot, { "Can't open" }
                ) {
                    OutlinedButton(onClick = { viewModel.retry(s.unavailable.map { it.id }) }) {
                        Text("Check again")
                    }
                }
            }
        }
    }

    if (confirm) {
        val extra = if (state.organizedSelected > 0) {
            "\n\n${state.organizedSelected} of them are favourites, in a collection, or have " +
                "a note or reminder. Those links are lost too."
        } else {
            ""
        }
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Remove ${state.selected.size} screenshot(s)?") },
            text = {
                Text(
                    "Their extracted text, titles and notes are deleted from Screenshot Brain. " +
                        "The original photos stay in your gallery." + extra
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    viewModel.removeSelected()
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } }
        )
    }
}

private fun dateText(millis: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))
