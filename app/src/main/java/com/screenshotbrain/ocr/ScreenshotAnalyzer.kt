package com.screenshotbrain.ocr

data class AnalysisResult(
    val title: String?,
    val category: String,
    val keywords: String?,
    val price: String?
)

object ScreenshotAnalyzer {

    private val categoryTerms: Map<String, List<String>> = linkedMapOf(
        "Payments" to listOf(
            "payment", "paid", "transaction", "upi", "debited", "credited", "receipt",
            "bank", "txn", "successful", "paytm", "phonepe", "gpay", "google pay",
            "transfer", "balance"
        ),
        "Shopping" to listOf(
            "discount", "sale", "offer", "deal", "coupon", "cart", "buy now", "mrp",
            "delivery", "price", "product", "shop", "order", "free shipping", "add to bag",
            "amazon", "flipkart", "myntra"
        ),
        "Education" to listOf(
            "exam", "lecture", "course", "syllabus", "chapter", "assignment", "homework",
            "university", "college", "jee", "neet", "tutorial", "quiz", "marks", "study",
            "student", "school"
        ),
        "Social Media" to listOf(
            "instagram", "followers", "following", "likes", "comments", "whatsapp",
            "twitter", "facebook", "reels", "retweet", "snapchat", "youtube", "subscribe",
            "linkedin", "post", "story"
        ),
        "Travel" to listOf(
            "flight", "hotel", "booking", "pnr", "boarding", "train", "ticket", "itinerary",
            "airport", "check-in", "trip", "bus", "irctc", "departure", "arrival"
        ),
        "Food" to listOf(
            "restaurant", "menu", "recipe", "swiggy", "zomato", "pizza", "burger",
            "ingredients", "cafe", "dinner", "lunch", "calories", "cook", "food"
        ),
        "Technology" to listOf(
            "python", "kotlin", "java", "code", "github", "error", "software", "android",
            "api", "programming", "javascript", "algorithm", "database", "server",
            "developer", "function", "ai"
        ),
        "Documents" to listOf(
            "aadhaar", "passport", "pan card", "certificate", "license", "document",
            "resume", "policy", "agreement", "insurance", "pdf", "application", "id card"
        )
    )

    // Whole-word matching, so "bus" does not match "business".
    private val termRegexes: Map<String, List<Pair<String, Regex>>> =
        categoryTerms.mapValues { (_, terms) ->
            terms.map { term ->
                term to Regex("(?<![a-z0-9])" + Regex.escape(term) + "(?![a-z0-9])")
            }
        }

    private val priceRegex = Regex(
        "(?<![A-Za-z])(?:[₹€$]|Rs\\.?|INR)\\s?\\d[\\d,]*(?:\\.\\d{1,2})?",
        RegexOption.IGNORE_CASE
    )

    private val wordRegex = Regex("[a-z][a-z0-9]{3,}")

    private val stopWords = setOf(
        "this", "that", "with", "from", "have", "your", "will", "been", "were", "they",
        "their", "there", "what", "when", "which", "would", "about", "more", "than",
        "then", "them", "into", "just", "also", "only", "very", "some", "such", "here"
    )

    fun analyze(rawText: String): AnalysisResult {
        val text = rawText.trim()
        if (text.isEmpty()) {
            return AnalysisResult(title = null, category = "Other", keywords = null, price = null)
        }

        val lower = text.lowercase()

        // Find the category with the most matching trigger words.
        var bestCategory = "Other"
        var bestTerms: List<String> = emptyList()
        for ((category, rules) in termRegexes) {
            val matched = rules.filter { (_, regex) -> regex.containsMatchIn(lower) }.map { it.first }
            if (matched.size > bestTerms.size) {
                bestCategory = category
                bestTerms = matched
            }
        }

        val price = priceRegex.find(text)?.value?.trim()
        val title = makeTitle(text, bestCategory)
        val keywords = makeKeywords(lower, bestTerms)

        return AnalysisResult(
            title = title,
            category = bestCategory,
            keywords = keywords.ifEmpty { null },
            price = price
        )
    }

    private fun makeTitle(text: String, category: String): String {
        val line = text.lines()
            .map { it.trim() }
            .firstOrNull { it.count { ch -> ch.isLetter() } >= 3 }
        return when {
            line == null -> "$category screenshot"
            line.length > 40 -> line.take(40).trimEnd() + "…"
            else -> line
        }
    }

    private fun makeKeywords(lowerText: String, matchedTerms: List<String>): String {
        val fromCategory = matchedTerms.take(4)
        val frequent = wordRegex.findAll(lowerText)
            .map { it.value }
            .filter { it !in stopWords && it !in fromCategory }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(8 - fromCategory.size)
        return (fromCategory + frequent).joinToString(",")
    }
}
