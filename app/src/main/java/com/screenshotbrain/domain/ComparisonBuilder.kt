package com.screenshotbrain.domain

import com.screenshotbrain.database.ScreenshotEntity

data class ComparisonRow(
    val id: Long,
    val uri: String,
    val name: String,
    val price: String?,
    val priceValue: Double?,
    val otherAmounts: List<String>,
    val percentShown: String?,
    val warnings: List<String>
)

data class ComparisonResult(
    val rows: List<ComparisonRow>,
    val cheapestId: Long?,
    val note: String?
)

object ComparisonBuilder {

    private val amountRegex = Regex(
        "(?<![A-Za-z])(?:[₹€$]|Rs\\.?|INR)\\s?\\d[\\d,]*(?:\\.\\d{1,2})?",
        RegexOption.IGNORE_CASE
    )
    private val percentRegex = Regex("(\\d{1,2})\\s?%")
    private val numberRegex = Regex("\\d[\\d,]*(?:\\.\\d+)?")

    fun build(items: List<ScreenshotEntity>): ComparisonResult {
        val rows = items.map { buildRow(it) }

        val ranked = rows.filter { it.priceValue != null }
        val lowest = ranked.minOfOrNull { it.priceValue!! }
        val tied = ranked.count { it.priceValue == lowest } > 1
        val cheapestId = if (ranked.size >= 2 && lowest != null && !tied) {
            ranked.first { it.priceValue == lowest }.id
        } else {
            null
        }

        val nonRupee = rows.any { it.price != null && it.priceValue == null }
        val note = when {
            nonRupee -> "Some prices are not in rupees, so they are not ranked."
            ranked.size >= 2 && tied -> "The lowest price is a tie, so no single cheapest is shown."
            ranked.size < 2 -> "Fewer than two prices were found, so nothing can be ranked."
            else -> null
        }
        return ComparisonResult(rows, cheapestId, note)
    }

    private fun buildRow(item: ScreenshotEntity): ComparisonRow {
        val price = item.price?.trim()?.ifEmpty { null }
        val text = item.ocrText.orEmpty()
        val priceNumber = price?.let { numberOf(it) }

        val others = amountRegex.findAll(text)
            .map { it.value.trim() }
            .filter { amount -> priceNumber == null || numberOf(amount) != priceNumber }
            .distinct()
            .take(3)
            .toList()

        val percent = percentRegex.findAll(text)
            .map { it.groupValues[1].toInt() }
            .firstOrNull { it in 5..90 }
            ?.let { "$it%" }

        val warnings = mutableListOf<String>()
        if (item.ocrStatus == 0) warnings += "Text is still being read."
        if (price == null) warnings += "No price found. Edit the screenshot to add one."
        if (price != null && others.isNotEmpty()) {
            warnings += "Other amounts appear in the text. Check which one is the real price."
        }

        return ComparisonRow(
            id = item.id,
            uri = item.uri,
            name = item.title ?: item.fileName,
            price = price,
            priceValue = price?.let { rupeeValue(it) },
            otherAmounts = others,
            percentShown = percent,
            warnings = warnings
        )
    }

    private fun numberOf(text: String): Double? =
        numberRegex.find(text)?.value?.replace(",", "")?.toDoubleOrNull()

    /** The number if the price is in rupees (₹, Rs, INR), otherwise null. */
    private fun rupeeValue(price: String): Double? {
        val lower = price.trim().lowercase()
        val isRupee = lower.startsWith("₹") || lower.startsWith("rs") || lower.startsWith("inr")
        return if (isRupee) numberOf(lower) else null
    }
}
