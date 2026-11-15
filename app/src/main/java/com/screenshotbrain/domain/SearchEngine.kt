package com.screenshotbrain.domain

import com.screenshotbrain.database.ScreenshotEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.min

data class SearchResult(
    val screenshot: ScreenshotEntity,
    val score: Int,
    val reasons: List<String>
)

object SearchEngine {

    private const val MAX_RESULTS = 30
    private const val MAX_REASONS = 4

    // Loose hints from common words to categories (matched against the stored category).
    private val CATEGORY_HINTS = mapOf(
        "product" to "Shopping", "buy" to "Shopping", "purchase" to "Shopping",
        "order" to "Shopping", "sale" to "Shopping", "deal" to "Shopping",
        "paid" to "Payments", "pay" to "Payments", "upi" to "Payments",
        "transaction" to "Payments", "receipt" to "Payments",
        "flight" to "Travel", "hotel" to "Travel", "ticket" to "Travel", "trip" to "Travel",
        "code" to "Technology", "coding" to "Technology", "programming" to "Technology",
        "exam" to "Education", "study" to "Education", "course" to "Education",
        "recipe" to "Food", "restaurant" to "Food"
    )

    private class Matcher(val term: String, val regex: Regex)

    fun search(
        all: List<ScreenshotEntity>,
        query: ParsedQuery,
        limit: Int = MAX_RESULTS
    ): List<SearchResult> {
        if (query.isEmpty) return emptyList()

        val matchers = query.terms.map { Matcher(it, buildRegex(it)) }
        val results = mutableListOf<SearchResult>()

        for (item in all) {
            val reasons = mutableListOf<String>()

            // 1. Date constraint
            query.dateRange?.let { range ->
                if (!range.contains(effectiveDay(item))) continue
                reasons += "Date matches ${range.label}"
            }

            // 2. Price constraint (only ₹ prices are compared)
            if (query.maxPrice != null || query.minPrice != null) {
                val value = rupeeValue(item.price) ?: continue
                query.maxPrice?.let {
                    if (value > it) continue
                    reasons += "Price ${item.price} is under ${QueryParser.formatPrice(it)}"
                }
                query.minPrice?.let {
                    if (value < it) continue
                    reasons += "Price ${item.price} is over ${QueryParser.formatPrice(it)}"
                }
            }

            // 3. Word matching and scoring
            var score = 0
            if (matchers.isNotEmpty()) {
                for (m in matchers) {
                    score += scoreTerm(item, m, reasons)
                }
                if (score == 0) continue
            } else {
                score = 1
            }

            results += SearchResult(item, score, reasons.take(MAX_REASONS))
        }

        return results
            .sortedWith(
                compareByDescending<SearchResult> { it.score }
                    .thenByDescending { it.screenshot.takenAt ?: it.screenshot.importedAt }
            )
            .take(limit)
    }

    private fun scoreTerm(
        item: ScreenshotEntity,
        m: Matcher,
        reasons: MutableList<String>
    ): Int {
        var score = 0
        val term = m.term

        if (item.title?.let { m.regex.containsMatchIn(it) } == true) {
            score += 5
            reasons += "Title contains \"$term\""
        }
        if (item.keywords?.let { m.regex.containsMatchIn(it) } == true) {
            score += 4
            reasons += "Keyword \"$term\""
        }
        val category = item.category
        if (category != null && m.regex.containsMatchIn(category)) {
            score += 4
            reasons += "Category: $category"
        } else if (category != null && CATEGORY_HINTS[term] == category) {
            score += 3
            reasons += "Category $category fits \"$term\""
        }
        if (item.note?.let { m.regex.containsMatchIn(it) } == true) {
            score += 3
            reasons += "Your note mentions \"$term\""
        }
        item.ocrText?.let { text ->
            if (m.regex.containsMatchIn(text)) {
                score += 2
                reasons += "Text says: \"${snippet(text, m.regex)}\""
            }
        }
        if (m.regex.containsMatchIn(item.fileName)) {
            score += 1
            reasons += "File name contains \"$term\""
        }
        return score
    }

    private fun buildRegex(term: String): Regex {
        val escaped = Regex.escape(term)
        val pattern = if (term.length <= 3) {
            "(?<![\\p{L}\\p{N}])$escaped(?![\\p{L}\\p{N}])"
        } else {
            escaped
        }
        return Regex(pattern, RegexOption.IGNORE_CASE)
    }

    private fun snippet(text: String, regex: Regex): String {
        val match = regex.find(text) ?: return ""
        val start = max(0, match.range.first - 25)
        val end = min(text.length, match.range.last + 26)
        val body = text.substring(start, end).replace(Regex("\\s+"), " ").trim()
        return (if (start > 0) "…" else "") + body + (if (end < text.length) "…" else "")
    }

    private fun effectiveDay(item: ScreenshotEntity): LocalDate {
        val millis = item.takenAt ?: item.importedAt
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    /** Returns the number if the price is in rupees (₹, Rs, INR), otherwise null. */
    private fun rupeeValue(price: String?): Double? {
        if (price == null) return null
        val lower = price.trim().lowercase()
        val isRupee = lower.startsWith("₹") || lower.startsWith("rs") || lower.startsWith("inr")
        if (!isRupee) return null
        val digits = Regex("\\d[\\d,]*(?:\\.\\d+)?").find(lower)?.value ?: return null
        return digits.replace(",", "").toDoubleOrNull()
    }
}
