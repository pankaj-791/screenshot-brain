package com.screenshotbrain.ui.cleanup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import com.screenshotbrain.database.CleanupCandidate

internal fun LazyListScope.groupSection(
    key: String,
    title: String,
    hint: String,
    label: String,
    groups: List<List<CleanupCandidate>>,
    selected: Set<Long>,
    onToggle: (Long) -> Unit,
    onOpen: (Long) -> Unit,
    onSelectRest: (Collection<Long>) -> Unit
) {
    if (groups.isEmpty()) return
    item(key = "h_$key") { CleanupSectionHeader(title, groups.size, hint) }
    groups.forEachIndexed { index, group ->
        item(key = "g_${key}_${group.first().id}") {
            Column {
                GroupHeader("$label ${index + 1} · ${group.size} screenshots") {
                    onSelectRest(group.drop(1).map { it.id })
                }
                TileRow(
                    items = group,
                    selected = selected,
                    captionFor = { _, i -> if (i == 0) "Suggested keep" else null },
                    onToggle = onToggle,
                    onOpen = onOpen
                )
            }
        }
    }
}

internal fun LazyListScope.listSection(
    key: String,
    title: String,
    hint: String,
    items: List<CleanupCandidate>,
    selected: Set<Long>,
    onToggle: (Long) -> Unit,
    onOpen: (Long) -> Unit,
    caption: (CleanupCandidate) -> String,
    footer: (@Composable () -> Unit)? = null
) {
    if (items.isEmpty()) return
    item(key = "h_$key") { CleanupSectionHeader(title, items.size, hint) }
    item(key = "r_$key") {
        Column {
            TileRow(
                items = items,
                selected = selected,
                captionFor = { c, _ -> caption(c) },
                onToggle = onToggle,
                onOpen = onOpen
            )
            footer?.invoke()
        }
    }
}
