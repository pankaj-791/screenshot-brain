package com.screenshotbrain.ui.collections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.screenshotbrain.database.CollectionWithInfo

@Composable
fun CollectionsScreen(
    onOpenCollection: (Long) -> Unit,
    viewModel: CollectionsViewModel = viewModel()
) {
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreate by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<CollectionWithInfo?>(null) }
    var deleteTarget by remember { mutableStateOf<CollectionWithInfo?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("New") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            Text(
                text = "Collections",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
            )

            val list = collections
            when {
                list == null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                list.isEmpty() -> EmptyCollections()

                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(list, key = { it.id }) { item ->
                        CollectionCard(
                            item = item,
                            onClick = { onOpenCollection(item.id) },
                            onFavourite = { viewModel.toggleFavourite(item) },
                            onRename = { renameTarget = item },
                            onDelete = { deleteTarget = item }
                        )
                    }
                }
            }
        }
    }

    if (showCreate) {
        NameDialog(
            title = "New collection",
            initial = "",
            confirmLabel = "Create",
            onConfirm = {
                showCreate = false
                viewModel.create(it)
            },
            onDismiss = { showCreate = false }
        )
    }

    renameTarget?.let { target ->
        NameDialog(
            title = "Rename collection",
            initial = target.name,
            confirmLabel = "Save",
            onConfirm = {
                renameTarget = null
                viewModel.rename(target.id, it)
            },
            onDismiss = { renameTarget = null }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete “${target.name}”?") },
            text = {
                Text(
                    "The collection is removed, but its ${target.itemCount} " +
                        "screenshot(s) stay in your library."
                )
            },
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
