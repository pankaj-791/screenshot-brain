package com.screenshotbrain.ui.brain

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.screenshotbrain.database.AppDatabase
import com.screenshotbrain.database.ScreenshotEntity
import com.screenshotbrain.domain.QueryParser
import com.screenshotbrain.domain.SearchEngine
import com.screenshotbrain.domain.SearchResult
import com.screenshotbrain.repository.ScreenshotRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BrainMessage(
    val id: Long,
    val question: String,
    val answer: String,
    val understood: String?,
    val results: List<SearchResult>
)

class BrainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ScreenshotRepository(
        context = application,
        dao = AppDatabase.get(application).screenshotDao()
    )

    private val _messages = MutableStateFlow<List<BrainMessage>>(emptyList())
    val messages: StateFlow<List<BrainMessage>> = _messages

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private var nextId = 1L

    fun ask(question: String) {
        val trimmed = question.trim()
        if (trimmed.isEmpty() || _isSearching.value) return
        val id = nextId++

        viewModelScope.launch {
            _isSearching.value = true
            val message = try {
                val all = repository.observeAll().first()
                withContext(Dispatchers.Default) { buildMessage(id, trimmed, all) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                BrainMessage(
                    id = id,
                    question = trimmed,
                    answer = "Something went wrong while searching. Please try again.",
                    understood = null,
                    results = emptyList()
                )
            }
            _messages.update { it + message }
            _isSearching.value = false
        }
    }

    fun clear() {
        _messages.value = emptyList()
    }

    private fun buildMessage(id: Long, question: String, all: List<ScreenshotEntity>): BrainMessage {
        if (all.isEmpty()) {
            return BrainMessage(
                id, question,
                "Your library is empty. Import some screenshots first, then ask me about them.",
                null, emptyList()
            )
        }

        val parsed = QueryParser.parse(question)
        if (parsed.isEmpty) {
            return BrainMessage(
                id, question,
                "I couldn't find any words, dates or prices to search for. " +
                    "Try something like “payment from yesterday” or “shoes below ₹1,000”.",
                null, emptyList()
            )
        }

        val results = SearchEngine.search(all, parsed)
        val pending = all.count { it.ocrStatus == 0 }

        val head = when (results.size) {
            0 -> "No matching screenshot was found."
            1 -> "Found 1 matching screenshot."
            else -> "Found ${results.size} matching screenshots."
        }
        val tail = if (pending > 0) {
            " $pending screenshot(s) are still being read, so ask again in a moment for complete results."
        } else {
            ""
        }

        return BrainMessage(id, question, head + tail, parsed.describe(), results)
    }
}
