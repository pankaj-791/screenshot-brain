package com.screenshotbrain.domain

import kotlin.math.sqrt

data class ChecklistItem(val text: String, val checked: Boolean)

object NoteTools {

    private const val UNCHECKED = "☐"
    private const val CHECKED = "☑"

    private val leadingMark = Regex(
        "^\\s*(?:[-*•·▪◦‣○●■□☐☑✓✔]|\\d{1,2}[.)]|\\[[ xX]?\\])\\s*"
    )
    private val checkedMark = Regex("^\\s*(?:☑|✓|✔|\\[[xX]\\])")
    private val sentenceSplit = Regex("(?<=[.!?])\\s+|\\n+")
    private val wordRegex = Regex("[\\p{L}][\\p{L}\\p{N}']{3,}")

    private val stopWords = setOf(
        "this", "that", "with", "from", "have", "your", "will", "been", "were", "they",
        "their", "there", "what", "when", "which", "would", "about", "more", "than",
        "then", "them", "into", "just", "also", "only", "very", "some", "such", "here",
        "these", "those", "where", "while", "after", "before", "other", "being", "does",
        "each", "most", "much", "many"
    )

    /** True when every non-empty line already starts with a tick box. */
    fun isChecklist(text: String): Boolean {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        return lines.isNotEmpty() &&
            lines.all { it.startsWith(UNCHECKED) || it.startsWith(CHECKED) }
    }

    /** One item per non-empty line. Bullets and numbers are removed, ticks are kept. */
    fun parseChecklist(text: String): List<ChecklistItem> =
        text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                val checked = checkedMark.containsMatchIn(line)
                val clean = line.replace(leadingMark, "").trim()
                if (clean.isEmpty()) null else ChecklistItem(clean, checked)
            }

    fun formatChecklist(items: List<ChecklistItem>): String =
        items.joinToString("\n") { (if (it.checked) CHECKED else UNCHECKED) + " " + it.text }

    /**
     * A shortened version made of the most informative sentences, in their original order.
     * It is not rewritten and not AI. Returns null if the text is too short to shorten.
     */
    fun summarize(text: String, maxSentences: Int = 3): String? {
        val sentences = text.split(sentenceSplit)
            .map { it.trim() }
            .filter { it.length >= 12 }
        if (sentences.size <= maxSentences) return null

        val frequency = HashMap<String, Int>()
        for (match in wordRegex.findAll(text.lowercase())) {
            val word = match.value
            if (word !in stopWords) frequency[word] = (frequency[word] ?: 0) + 1
        }
        if (frequency.isEmpty()) return null

        val scored = sentences.mapIndexed { index, sentence ->
            val words = wordRegex.findAll(sentence.lowercase())
                .map { it.value }
                .filter { it !in stopWords }
                .toList()
            val score = if (words.isEmpty()) {
                0.0
            } else {
                words.sumOf { frequency[it] ?: 0 }.toDouble() / sqrt(words.size.toDouble())
            }
            Triple(index, sentence, score)
        }

        return scored
            .sortedByDescending { it.third }
            .take(maxSentences)
            .sortedBy { it.first }
            .joinToString("\n") { "• " + it.second }
    }
}
