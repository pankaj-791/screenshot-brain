package com.screenshotbrain.ui.cleanup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.screenshotbrain.database.CleanupCandidate

private val TILE_WIDTH = 112.dp
private val TILE_HEIGHT = 176.dp

@Composable
fun CleanupIntroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Nothing is removed unless you tick it and confirm.",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Remove only takes a screenshot out of Screenshot Brain. " +
                    "The original photo stays in your gallery.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun AnalysisProgress(remaining: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Analyzing $remaining screenshot(s)… Results appear as they finish.",
            style = MaterialTheme.typography.bodySmall
        )
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun CleanupSectionHeader(title: String, count: Int, hint: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = "$title ($count)", style = MaterialTheme.typography.titleMedium)
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun GroupHeader(label: String, onSelectRest: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onSelectRest) { Text("Select all except the first") }
    }
}

@Composable
fun TileRow(
    items: List<CleanupCandidate>,
    selected: Set<Long>,
    captionFor: (CleanupCandidate, Int) -> String?,
    onToggle: (Long) -> Unit,
    onOpen: (Long) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
            CleanupTile(
                item = item,
                selected = item.id in selected,
                caption = captionFor(item, index),
                onToggle = { onToggle(item.id) },
                onOpen = { onOpen(item.id) }
            )
        }
    }
}

@Composable
fun CleanupTile(
    item: CleanupCandidate,
    selected: Boolean,
    caption: String?,
    onToggle: () -> Unit,
    onOpen: () -> Unit
) {
    val flags = organizedFlags(item)

    Box(
        modifier = Modifier
            .size(width = TILE_WIDTH, height = TILE_HEIGHT)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onToggle)
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.title ?: item.fileName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            )
        }
        Icon(
            imageVector = if (selected) {
                Icons.Outlined.CheckCircle
            } else {
                Icons.Outlined.RadioButtonUnchecked
            },
            contentDescription = if (selected) "Selected" else "Not selected",
            tint = if (selected) MaterialTheme.colorScheme.onPrimary else Color.White,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .size(24.dp)
        )
        IconButton(
            onClick = onOpen,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(32.dp)
                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Outlined.OpenInNew,
                contentDescription = "Open screenshot",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
        if (caption != null || flags.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                if (caption != null) {
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        maxLines = 2
                    )
                }
                if (flags.isNotEmpty()) {
                    Text(
                        text = flags,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 2
                    )
                }
            }
        }
    }
}

private fun organizedFlags(item: CleanupCandidate): String =
    listOfNotNull(
        if (item.isFavourite) "Favourite" else null,
        if (item.collectionCount > 0) "In collection" else null,
        if (item.reminderCount > 0) "Reminder" else null,
        if (!item.note.isNullOrBlank()) "Note" else null
    ).joinToString(" · ")
