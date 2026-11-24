package com.screenshotbrain.ui.detail

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.screenshotbrain.database.ScreenshotEntity
import com.screenshotbrain.domain.DateSuggester
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date

@Composable
fun DetailContent(
    item: ScreenshotEntity,
    padding: PaddingValues,
    onEdit: () -> Unit,
    onShareImage: () -> Unit,
    onCopyText: () -> Unit,
    onShareText: () -> Unit,
    onSetReminder: () -> Unit
) {
    val dateText = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(item.takenAt ?: item.importedAt))
    val hasText = !item.ocrText.isNullOrBlank()
    val suggestions = remember(item.ocrText, item.takenAt, item.importedAt) {
        val reference = Instant.ofEpochMilli(item.takenAt ?: item.importedAt)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        DateSuggester.suggest(item.ocrText, reference = reference)
    }

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

        if (item.ocrStatus != 0 && suggestions.isNotEmpty()) {
            val first = suggestions.first()
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Date found in this screenshot",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${first.label} (“${first.source}”)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    val extra = suggestions.size - 1
                    Text(
                        text = (if (extra > 0) "$extra more date(s) in the pop-up. " else "") +
                            "Nothing is scheduled until you tap Save there.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Button(onClick = onSetReminder) {
                        Icon(Icons.Outlined.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Set reminder")
                    }
                }
            }
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

fun shareImage(context: Context, uriString: String): Boolean {
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

fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share text"))
}
