package com.screenshotbrain.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.screenshotbrain.ui.library.LibraryViewModel

@Composable
fun EditDetailsDialog(
    initialTitle: String,
    initialCategory: String,
    initialNote: String,
    onDismiss: () -> Unit,
    onSave: (title: String, category: String, note: String) -> Unit
) {
    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var category by rememberSaveable { mutableStateOf(initialCategory) }
    var note by rememberSaveable { mutableStateOf(initialNote) }
    var menuOpen by rememberSaveable { mutableStateOf(false) }

    val categories = if (category in LibraryViewModel.CATEGORIES) {
        LibraryViewModel.CATEGORIES
    } else {
        LibraryViewModel.CATEGORIES + category
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit details") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Box {
                    OutlinedButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Category: $category")
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        categories.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    category = option
                                    menuOpen = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Personal note") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, category, note) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
