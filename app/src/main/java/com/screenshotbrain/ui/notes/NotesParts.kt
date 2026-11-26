package com.screenshotbrain.ui.notes

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.screenshotbrain.domain.ChecklistItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesToolbar(
    checklistMode: Boolean,
    hasExtractedText: Boolean,
    onUseExtracted: () -> Unit,
    onMakeChecklist: () -> Unit,
    onBackToText: () -> Unit,
    onKeyLines: () -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (hasExtractedText) {
            AssistChip(onClick = onUseExtracted, label = { Text("Use extracted text") })
        }
        if (checklistMode) {
            AssistChip(onClick = onBackToText, label = { Text("Back to text") })
        } else {
            AssistChip(onClick = onMakeChecklist, label = { Text("Make checklist") })
        }
        AssistChip(onClick = onKeyLines, label = { Text("Key lines") })
    }
}

@Composable
fun ChecklistEditor(
    items: List<ChecklistItem>,
    onToggle: (Int) -> Unit,
    onEdit: (Int, String) -> Unit,
    onRemove: (Int) -> Unit,
    onAdd: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEachIndexed { index, item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = item.checked, onCheckedChange = { onToggle(index) })
                OutlinedTextField(
                    value = item.text,
                    onValueChange = { onEdit(index, it) },
                    textStyle = if (item.checked) {
                        LocalTextStyle.current.copy(textDecoration = TextDecoration.LineThrough)
                    } else {
                        LocalTextStyle.current
                    },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onRemove(index) }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Remove item")
                }
            }
        }
        TextButton(onClick = onAdd) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add item")
        }
    }
}
