package com.screenshotbrain.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DateSuggestion(
    val date: LocalDate,
    val label: String,
    val source: String
)

object DateSuggester {

    private const val MAX_SUGGESTIONS = 3

    private val MONTH_NAMES = listOf(
        "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"
    )

    private val monthPattern =
        "(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|june?|july?|" +
            "aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)"

    private val isoRegex = Regex("\\b(20\\d{2})-(\\d{1,2})-(\\d{1,2})\\b")

    private val numericRegex = Regex("\\b(\\d{1,2})[/.\\-](\\d{1,2})[/.\\-](\\d{4}|\\d{2})\\b")

    private val dayMonthRegex = Regex(
        "\\b(\\d{1,2})(?:st|nd|rd|th)?\\s+(?:of\\s+)?$monthPattern\\b\\.?(?:,?\\s+(20\\d{2}))?",
        RegexOption.IGNORE_CASE
    )

    private val monthDayRegex = Regex(
        "\\b$monthPattern\\b\\.?\\s+(\\d{1,2})(?:st|nd|rd|th)?\\b(?:,?\\s+(20\\d{2}))?",
        RegexOption.IGNORE_CASE
    )

    /** Dates found in the text that are today or later. Never schedules anything. */
    fun suggest(text: String?, today: LocalDate = LocalDate.now()): List<DateSuggestion> {
        if (text.isNullOrBlank()) return emptyList()

        val found = LinkedHashMap<LocalDate, String>()

        fun add(date: LocalDate?, source: String) {
            if (date != null && !date.isBefore(today) && !found.containsKey(date)) {
                found[date] = source.trim()
            }
        }

        for (m in isoRegex.findAll(text)) {
            add(
                build(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt()),
                m.value
            )
        }
        for (m in numericRegex.findAll(text)) {
            val year = m.groupValues[3].toInt().let { if (it < 100) 2000 + it else it }
            add(build(year, m.groupValues[2].toInt(), m.groupValues[1].toInt()), m.value)
        }
        for (m in dayMonthRegex.findAll(text)) {
            val year = m.groupValues[3].toIntOrNull() ?: today.year
            add(build(year, monthOf(m.groupValues[2]), m.groupValues[1].toInt()), m.value)
        }
        for (m in monthDayRegex.findAll(text)) {
            val year = m.groupValues[3].toIntOrNull() ?: today.year
            add(build(year, monthOf(m.groupValues[1]), m.groupValues[2].toInt()), m.value)
        }

        val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
        return found.entries
            .sortedBy { it.key }
            .take(MAX_SUGGESTIONS)
            .map { DateSuggestion(date = it.key, label = it.key.format(formatter), source = it.value) }
    }

    private fun monthOf(token: String): Int =
        MONTH_NAMES.indexOf(token.take(3).lowercase()) + 1

    private fun build(year: Int, month: Int, day: Int): LocalDate? =
        try {
            LocalDate.of(year, month, day)
        } catch (e: Exception) {
            null
        }
}
