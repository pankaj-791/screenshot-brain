package com.screenshotbrain.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.screenshotbrain.ui.collections.CenterMessage
import com.screenshotbrain.ui.detail.shareText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    onBack: () -> Unit,
    viewModel: NotesViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var confirmDiscard by remember { mutableStateOf(false) }

    BackHandler(enabled = state.edited) { confirmDiscard = true }

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
                title = { Text("Notes") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.edited) confirmDiscard = true else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.save() }, enabled = state.canSave) {
                        Text("Save")
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            !state.loaded -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            state.missing -> Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                CenterMessage(
                    title = "This screenshot is no longer available.",
                    body = "",
                    actionLabel = "Go back",
                    onAction = onBack
                )
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                NotesToolbar(
                    checklistMode = state.checklistMode,
                    hasExtractedText = state.ocrText.isNotBlank(),
                    onUseExtracted = { viewModel.useExtractedText() },
                    onMakeChecklist = { viewModel.makeChecklist() },
                    onBackToText = { viewModel.backToText() },
                    onKeyLines = { viewModel.addSummary() }
                )
                Text(
                    text = "Key lines picks the most informative sentences from your text. " +
                        "It is not an AI rewrite.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (state.checklistMode) {
                    ChecklistEditor(
                        items = state.items,
                        onToggle = { viewModel.toggleItem(it) },
                        onEdit = { index, value -> viewModel.editItem(index, value) },
                        onRemove = { viewModel.removeItem(it) },
                        onAdd = { viewModel.addItem() }
                    )
                } else {
                    OutlinedTextField(
                        value = state.text,
                        onValueChange = { viewModel.setText(it) },
                        placeholder = { Text("Write a note, or use the extracted text") },
                        minLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val content = state.content()
                            if (content.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar("Nothing to copy") }
                            } else {
                                clipboard.setText(AnnotatedString(content))
                                scope.launch { snackbarHostState.showSnackbar("Note copied") }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Copy") }
                    OutlinedButton(
                        onClick = {
                            val content = state.content()
                            if (content.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar("Nothing to share") }
                            } else {
                                shareText(context, content)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Share") }
                }
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard your changes?") },
            text = { Text("You have edits that are not saved.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    onBack()
                }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") }
            }
        )
    }
}
