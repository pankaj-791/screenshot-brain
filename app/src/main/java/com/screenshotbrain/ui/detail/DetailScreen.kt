package com.screenshotbrain.ui.detail

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.screenshotbrain.database.ScreenshotEntity
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(onBack: () -> Unit, viewModel: DetailViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var showDelete by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var showCollections by remember { mutableStateOf(false) }
    val allCollections by viewModel.allCollections.collectAsStateWithLifecycle()
    val memberOf by viewModel.memberOf.collectAsStateWithLifecycle()

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
                                                    IconButton(onClick = { showCollections = true }) {
                            Icon(
                                Icons.Outlined.CreateNewFolder,
                                contentDescription = "Add to collection"
                            )
                                                    }
                            Icon(
                                imageVector = if (item.isFavourite) {
                                    Icons.Outlined.Favorite
                                } else {
                                    Icons.Outlined.FavoriteBorder
                                },
                                contentDescription = "Toggle favourite"
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
                onShareText = { shareText(context, current.item.ocrText.orEmpty()) }
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

@Composable
private fun DetailContent(
    item: ScreenshotEntity,
    padding: PaddingValues,
    onEdit: () -> Unit,
    onShareImage: () -> Unit,
    onCopyText: () -> Unit,
    onShareText: () -> Unit
) {
    val dateText = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(item.takenAt ?: item.importedAt))
    val hasText = !item.ocrText.isNullOrBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.fileName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .clip(RoundedCornerShape(16.dp))
        )

        Text(
            text = item.title ?: item.fileName,
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = listOfNotNull(item.category, item.price, dateText).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!item.keywords.isNullOrBlank()) {
            Text(
                text = "Keywords: " + item.keywords.replace(",", ", "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!item.note.isNullOrBlank()) {
            Text(text = "Note: " + item.note, style = MaterialTheme.typography.bodyMedium)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Edit")
            }
            OutlinedButton(onClick = onShareImage, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Share image")
            }
        }

        Text(text = "Extracted text", style = MaterialTheme.typography.titleMedium)

        when {
            item.ocrStatus == 0 -> Text("Still reading text…")
            !hasText -> Text("No text was found in this screenshot.")
            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onCopyText, modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Copy text")
                    }
                    OutlinedButton(onClick = onShareText, modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Share text")
                    }
                }
                SelectionContainer {
                    Text(text = item.ocrText.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

private fun shareImage(context: Context, uriString: String): Boolean {
    return try {
        val uri = Uri.parse(uriString)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share screenshot"))
        true
    } catch (e: Exception) {
        false
    }
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share text"))
}
