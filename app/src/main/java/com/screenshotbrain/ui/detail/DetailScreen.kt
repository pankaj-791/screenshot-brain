package com.screenshotbrain.ui.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(onBack: () -> Unit, viewModel: DetailViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val allCollections by viewModel.allCollections.collectAsStateWithLifecycle()
    val memberOf by viewModel.memberOf.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var showDelete by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var showCollections by remember { mutableStateOf(false) }
    var showReminder by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    val item = (state as? DetailState.Found)?.item

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Screenshot") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (item != null) {
                        IconButton(onClick = { viewModel.toggleFavourite(item) }) {
                            Icon(
                                imageVector = if (item.isFavourite) {
                                    Icons.Outlined.Favorite
                                } else {
                                    Icons.Outlined.FavoriteBorder
                                },
                                contentDescription = "Toggle favourite"
                            )
                        }
                        IconButton(onClick = { showReminder = true }) {
                            Icon(Icons.Outlined.Alarm, contentDescription = "Set reminder")
                        }
                        IconButton(onClick = { showCollections = true }) {
                            Icon(
                                Icons.Outlined.CreateNewFolder,
                                contentDescription = "Add to collection"
                            )
                        }
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val current = state) {
            is DetailState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is DetailState.Missing -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("This screenshot is no longer available.")
                    TextButton(onClick = onBack) { Text("Go back") }
                }
            }

            is DetailState.Found -> DetailContent(
                item = current.item,
                padding = innerPadding,
                onEdit = { showEdit = true },
                onShareImage = {
                    if (!shareImage(context, current.item.uri)) {
                        scope.launch { snackbarHostState.showSnackbar("Couldn't share this image.") }
                    }
                },
                onCopyText = {
                    clipboard.setText(AnnotatedString(current.item.ocrText.orEmpty()))
                    scope.launch { snackbarHostState.showSnackbar("Text copied") }
                },
                onShareText = { shareText(context, current.item.ocrText.orEmpty()) },
                onSetReminder = { showReminder = true }
            )
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Remove from Screenshot Brain?") },
            text = {
                Text(
                    "This removes the screenshot, its extracted text and your note from the app. " +
                        "The original photo stays in your gallery."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    viewModel.delete(onDone = onBack)
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Cancel") }
            }
        )
    }

    if (showCollections) {
        AddToCollectionDialog(
            collections = allCollections,
            memberOf = memberOf,
            onToggle = { collectionId, add -> viewModel.setInCollection(collectionId, add) },
            onDismiss = { showCollections = false }
        )
    }

    if (showReminder && item != null) {
        ScreenshotReminderDialog(
            screenshot = item,
            onSave = { title, note, at -> viewModel.addReminder(title, note, at) },
            onDismiss = { showReminder = false }
        )
    }

    if (showEdit && item != null) {
        EditDetailsDialog(
            initialTitle = item.title.orEmpty(),
            initialCategory = item.category ?: "Other",
            initialNote = item.note.orEmpty(),
            initialPrice = item.price.orEmpty(),
            onDismiss = { showEdit = false },
            onSave = { title, category, note, price ->
                showEdit = false
                viewModel.save(title, category, note, price)
            }
        )
    }
}
