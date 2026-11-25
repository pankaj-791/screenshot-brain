package com.screenshotbrain.ui.compare

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.screenshotbrain.database.ScreenshotEntity
import com.screenshotbrain.domain.ComparisonResult
import com.screenshotbrain.domain.ComparisonRow

private val LABEL_WIDTH = 92.dp
private val COLUMN_WIDTH = 140.dp
private val IMAGE_HEIGHT = 150.dp
private val NAME_HEIGHT = 64.dp
private val PRICE_HEIGHT = 52.dp
private val OTHER_HEIGHT = 76.dp
private val PERCENT_HEIGHT = 44.dp

@Composable
fun ComparisonTable(result: ComparisonResult) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row {
            Column(modifier = Modifier.width(LABEL_WIDTH)) {
                Spacer(Modifier.height(IMAGE_HEIGHT))
                LabelCell("Name", NAME_HEIGHT)
                LabelCell("Price", PRICE_HEIGHT)
                LabelCell("Other amounts", OTHER_HEIGHT)
                LabelCell("Discount shown", PERCENT_HEIGHT)
            }
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                result.rows.forEach { row ->
                    ProductColumn(row = row, isCheapest = row.id == result.cheapestId)
                }
            }
        }

        result.note?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall)
        }

        val flagged = result.rows.filter { it.warnings.isNotEmpty() }
        if (flagged.isNotEmpty()) {
            Text(text = "Check these", style = MaterialTheme.typography.titleSmall)
            flagged.forEach { row ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = row.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    row.warnings.forEach { warning ->
                        Text(text = "• $warning", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Text(
            text = "Only what is visible in the screenshots is shown. Nothing is guessed.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LabelCell(label: String, height: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Cell(height: Dp, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) { content() }
}

@Composable
private fun NotFound() {
    Text(
        text = "Not found",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ProductColumn(row: ComparisonRow, isCheapest: Boolean) {
    Column(
        modifier = Modifier
            .padding(end = 8.dp)
            .width(COLUMN_WIDTH)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isCheapest) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
    ) {
        AsyncImage(
            model = row.uri,
            contentDescription = row.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(IMAGE_HEIGHT)
        )
        Cell(NAME_HEIGHT) {
            Text(
                text = row.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        Cell(PRICE_HEIGHT) {
            if (row.price == null) {
                NotFound()
            } else {
                Column {
                    Text(
                        text = row.price,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isCheapest) {
                        Text(
                            text = "Lowest",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        Cell(OTHER_HEIGHT) {
            if (row.otherAmounts.isEmpty()) {
                Text(
                    text = "None found",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = row.otherAmounts.joinToString("\n"),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3
                )
            }
        }
        Cell(PERCENT_HEIGHT) {
            if (row.percentShown == null) {
                NotFound()
            } else {
                Text(text = row.percentShown, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun CompareTile(
    item: ScreenshotEntity,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.6f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.title ?: item.fileName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            )
        }
        Icon(
            imageVector = if (isSelected) {
                Icons.Outlined.CheckCircle
            } else {
                Icons.Outlined.RadioButtonUnchecked
            },
            contentDescription = if (isSelected) "Selected" else "Not selected",
            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(24.dp)
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Text(
                text = item.title ?: item.fileName,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.price ?: "No price",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1
            )
        }
    }
}
