package com.screenshotbrain.domain

import com.screenshotbrain.database.CleanupCandidate
import com.screenshotbrain.utils.ImageAnalyzer

data class CleanupSuggestions(
    val duplicateGroups: List<List<CleanupCandidate>> = emptyList(),
    val similarGroups: List<List<CleanupCandidate>> = emptyList(),
    val blurry: List<CleanupCandidate> = emptyList(),
    val old: List<CleanupCandidate> = emptyList(),
    val unused: List<CleanupCandidate> = emptyList(),
    val unavailable: List<CleanupCandidate> = emptyList()
) {
    val isEmpty: Boolean
        get() = duplicateGroups.isEmpty() && similarGroups.isEmpty() && blurry.isEmpty() &&
            old.isEmpty() && unused.isEmpty() && unavailable.isEmpty()
}

object CleanupFinder {

    const val BLUR_THRESHOLD = 15.0
    const val OLD_DAYS = 180L
    const val UNUSED_DAYS = 30L

    private const val MAX_VISUAL_DISTANCE = 6
    private const val MAX_VISUAL_DISTANCE_NO_TEXT = 3
    private const val MIN_TEXT_OVERLAP = 0.6
    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

    private val wordRegex = Regex("[\\p{L}\\p{N}]{3,}")

    fun find(
        all: List<CleanupCandidate>,
        now: Long = System.currentTimeMillis()
    ): CleanupSuggestions {
        val analyzed = all.filter { it.cleanupStatus == STATUS_DONE }
        val usable = all.filter { it.cleanupStatus != STATUS_UNAVAILABLE }

        val old = usable
            .filter { !isProtected(it) && now - effectiveDate(it) > OLD_DAYS * DAY_MILLIS }
            .sortedBy { effectiveDate(it) }
        val oldIds = old.map { it.id }.toSet()
        val unused = usable
            .filter {
                !isProtected(it) && it.id !in oldIds &&
                    now - it.importedAt > UNUSED_DAYS * DAY_MILLIS
            }
            .sortedBy { it.importedAt }

        return CleanupSuggestions(
            duplicateGroups = findDuplicates(analyzed),
            similarGroups = findSimilar(analyzed),
            blurry = analyzed
                .filter { c ->
                    val s = c.sharpness
                    !isProtected(c) && s != null && s < BLUR_THRESHOLD
                }
                .sortedBy { it.sharpness },
            old = old,
            unused = unused,
            unavailable = all.filter { it.cleanupStatus == STATUS_UNAVAILABLE }
        )
    }

    private fun findDuplicates(items: List<CleanupCandidate>): List<List<CleanupCandidate>> =
        items.filter { it.contentHash != null }
            .groupBy { it.contentHash }
            .values
            .filter { it.size >= 2 }
            .map { keepFirst(it) }
            .sortedByDescending { it.size }

    private fun findSimilar(items: List<CleanupCandidate>): List<List<CleanupCandidate>> {
        val reps = items
            .filter { it.visualHash != null && it.contentHash != null }
            .distinctBy { it.contentHash }
        val words = reps.map { wordSet(it) }
        val parent = IntArray(reps.size) { it }

        fun root(start: Int): Int {
            var r = start
            while (parent[r] != r) {
                parent[r] = parent[parent[r]]
                r = parent[r]
            }
            return r
        }

        for (i in reps.indices) {
            for (j in i + 1 until reps.size) {
                val distance = ImageAnalyzer.distance(reps[i].visualHash!!, reps[j].visualHash!!)
                if (distance > MAX_VISUAL_DISTANCE) continue
                val a = words[i]
                val b = words[j]
                val similar = if (a.isEmpty() && b.isEmpty()) {
                    distance <= MAX_VISUAL_DISTANCE_NO_TEXT
                } else {
                    overlap(a, b) >= MIN_TEXT_OVERLAP
                }
                if (similar) parent[root(j)] = root(i)
            }
        }

        return reps.indices
            .groupBy { root(it) }
            .values
            .filter { it.size >= 2 }
            .map { indexes -> keepFirst(indexes.map { reps[it] }) }
            .sortedByDescending { it.size }
    }

    private fun wordSet(c: CleanupCandidate): Set<String> =
        wordRegex.findAll((c.title.orEmpty() + " " + c.ocrText.orEmpty()).lowercase())
            .map { it.value }
            .toSet()

    private fun overlap(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val common = a.count { it in b }
        return common.toDouble() / (a.size + b.size - common)
    }

    private fun effectiveDate(c: CleanupCandidate): Long = c.takenAt ?: c.importedAt

    private fun isProtected(c: CleanupCandidate): Boolean = protection(c) > 0

    private fun protection(c: CleanupCandidate): Int =
        (if (c.isFavourite) 1 else 0) + (if (c.collectionCount > 0) 1 else 0) +
            (if (c.reminderCount > 0) 1 else 0) + (if (!c.note.isNullOrBlank()) 1 else 0)

    /** The best copy to keep comes first: the most organized one, then the oldest. */
    private fun keepFirst(group: List<CleanupCandidate>): List<CleanupCandidate> =
        group.sortedWith(
            compareByDescending<CleanupCandidate> { protection(it) }
                .thenBy { effectiveDate(it) }
        )

    private const val STATUS_DONE = 1
    private const val STATUS_UNAVAILABLE = 2
}
