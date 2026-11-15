package com.screenshotbrain.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class DateRange(val start: LocalDate, val end: LocalDate, val label: String) {
    fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)
}

data class ParsedQuery(
    val terms: List<String>,
    val dateRange: DateRange?,
    val maxPrice: Double?,
    val minPrice: Double?
) {
    val hasConstraints: Boolean
        get() = dateRange != null || maxPrice != null || minPrice != null

    val isEmpty: Boolean
        get() = terms.isEmpty() && !hasConstraints

    fun describe(): String {
        val parts = mutableListOf<String>()
        if (terms.isNotEmpty()) parts += "words: " + terms.joinToString(", ")
        dateRange?.let { parts += "date: ${it.label}" }
        maxPrice?.let { parts += "price under ${QueryParser.formatPrice(it)}" }
        minPrice?.let { parts += "price over ${QueryParser.formatPrice(it)}" }
        return parts.joinToString(" · ")
    }
}

object QueryParser {

    private val SHORT = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
    private val MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

    private val maxRegex = Regex(
        "(?:below|under|less than|cheaper than|up to|upto|max)\\s*(?:₹|rs\\.?|inr|[$])?\\s*(\\d[\\d,]*(?:\\.\\d+)?)"
    )
    private val minRegex = Regex(
        "(?:above|over|more than|at least|min)\\s*(?:₹|rs\\.?|inr|[$])?\\s*(\\d[\\d,]*(?:\\.\\d+)?)"
    )

    private val monthRegex = Regex(
        "\\b(january|february|march|april|june|july|august|september|october|november|december|jan|feb|apr|jun|jul|aug|sept|sep|oct|nov|dec)\\b(?:\\s+(\\d{4}))?"
    )
    private val mayRegex = Regex("\\b(?:in|from|of|during)\\s+(may)\\b(?:\\s+(\\d{4}))?")

    private val MONTHS = mapOf(
        "january" to 1, "jan" to 1, "february" to 2, "feb" to 2, "march" to 3,
        "april" to 4, "apr" to 4, "may" to 5, "june" to 6, "jun" to 6,
        "july" to 7, "jul" to 7, "august" to 8, "aug" to 8,
        "september" to 9, "sept" to 9, "sep" to 9, "october" to 10, "oct" to 10,
        "november" to 11, "nov" to 11, "december" to 12, "dec" to 12
    )

    private val STOP_WORDS = setOf(
        "a", "an", "the", "i", "me", "my", "mine", "we", "you", "it", "is", "are", "was",
        "were", "be", "been", "am", "do", "did", "does", "have", "had", "has", "of", "in",
        "on", "at", "to", "for", "from", "with", "about", "and", "or", "that", "this",
        "those", "these", "which", "what", "where", "when", "who", "any", "all", "some",
        "find", "show", "give", "get", "look", "looking", "search", "see", "saw", "want",
        "wanted", "need", "needed", "please", "can", "could", "would", "should", "there",
        "their", "containing", "contain", "contains", "took", "taken", "take", "saved",
        "save", "kept", "one", "ones", "thing", "things", "related", "regarding", "than",
        "then", "just", "also", "only", "very", "screenshot", "screenshots", "image",
        "images", "photo", "photos", "pic", "pics", "picture", "pictures"
    )

    fun formatPrice(value: Double): String = "₹" + String.format(Locale.US, "%,.0f", value)

    fun parse(raw: String, today: LocalDate = LocalDate.now()): ParsedQuery {
        var text = " " + raw.lowercase(Locale.ROOT) + " "

        var maxPrice: Double? = null
        maxRegex.find(text)?.let {
            maxPrice = toNumber(it.groupValues[1])
            text = text.replace(it.value, " ")
        }

        var minPrice: Double? = null
        minRegex.find(text)?.let {
            minPrice = toNumber(it.groupValues[1])
            text = text.replace(it.value, " ")
        }

        var dateRange: DateRange? = null
        findDate(text, today)?.let { (range, matched) ->
            dateRange = range
            text = text.replace(matched, " ")
        }

        val terms = Regex("[\\p{L}\\p{N}]+").findAll(text)
            .map { it.value }
            .filter { it.length >= 2 && it !in STOP_WORDS }
            .map { singular(it) }
            .distinct()
            .toList()

        return ParsedQuery(terms, dateRange, maxPrice, minPrice)
    }

    private fun toNumber(text: String): Double? = text.replace(",", "").toDoubleOrNull()

    private fun singular(word: String): String {
        return if (word.length > 3 && word.endsWith("s") &&
            !word.endsWith("ss") && !word.endsWith("us")
        ) {
            word.dropLast(1)
        } else {
            word
        }
    }

    private fun findDate(text: String, today: LocalDate): Pair<DateRange, String>? {
        Regex("\\byesterday\\b").find(text)?.let {
            val day = today.minusDays(1)
            return DateRange(day, day, "yesterday (${day.format(SHORT)})") to it.value
        }
        Regex("\\btoday\\b").find(text)?.let {
            return DateRange(today, today, "today") to it.value
        }
        Regex("\\blast week\\b").find(text)?.let {
            val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val start = monday.minusWeeks(1)
            val end = monday.minusDays(1)
            val label = "last week (${start.format(SHORT)} to ${end.format(SHORT)})"
            return DateRange(start, end, label) to it.value
        }
        Regex("\\bthis week\\b").find(text)?.let {
            val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            return DateRange(start, today, "this week") to it.value
        }
        Regex("\\blast month\\b").find(text)?.let {
            val start = today.withDayOfMonth(1).minusMonths(1)
            val end = start.plusMonths(1).minusDays(1)
            return DateRange(start, end, "last month (${start.format(MONTH_YEAR)})") to it.value
        }
        Regex("\\bthis month\\b").find(text)?.let {
            return DateRange(today.withDayOfMonth(1), today, "this month") to it.value
        }
        Regex("\\b(?:last|past)\\s+(\\d{1,3})\\s+days?\\b").find(text)?.let {
            val days = it.groupValues[1].toLong()
            if (days > 0) {
                return DateRange(today.minusDays(days), today, "last $days days") to it.value
            }
        }

        val monthMatch = monthRegex.find(text) ?: mayRegex.find(text)
        if (monthMatch != null) {
            val month = MONTHS[monthMatch.groupValues[1]]
            if (month != null) {
                val year = monthMatch.groupValues[2].toIntOrNull()
                    ?: (if (LocalDate.of(today.year, month, 1).isAfter(today)) today.year - 1 else today.year)
                val start = LocalDate.of(year, month, 1)
                val end = start.plusMonths(1).minusDays(1)
                return DateRange(start, end, start.format(MONTH_YEAR)) to monthMatch.value
            }
        }
        return null
    }
}
