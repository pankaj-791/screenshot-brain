package com.screenshotbrain.ui.compare

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.screenshotbrain.database.AppDatabase
import com.screenshotbrain.database.ScreenshotEntity
import com.screenshotbrain.domain.ComparisonBuilder
import com.screenshotbrain.domain.ComparisonResult
import com.screenshotbrain.repository.ScreenshotRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CompareUiState(
    val loaded: Boolean = false,
    val items: List<ScreenshotEntity> = emptyList(),
    val total: Int = 0
)

class CompareViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ScreenshotRepository(
        context = application,
        dao = AppDatabase.get(application).screenshotDao()
    )

    private val _query = MutableStateFlow("")

    val state: StateFlow<CompareUiState> = combine(repository.observeAll(), _query) { all, q ->
        CompareUiState(loaded = true, items = prepare(all, q), total = all.size)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CompareUiState())

    private val _selected = MutableStateFlow<Set<Long>>(emptySet())
    val selected: StateFlow<Set<Long>> = _selected

    private val _result = MutableStateFlow<ComparisonResult?>(null)
    val result: StateFlow<ComparisonResult?> = _result

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun setQuery(value: String) {
        _query.value = value
    }

    fun toggle(id: Long) {
        val current = _selected.value
        if (id in current) {
            _selected.update { it - id }
        } else if (current.size >= MAX_SELECTION) {
            _message.value = "You can compare up to $MAX_SELECTION screenshots."
        } else {
            _selected.update { it + id }
        }
    }

    fun compare() {
        val ids = _selected.value
        if (ids.size < 2) {
            _message.value = "Pick at least 2 screenshots."
            return
        }
        viewModelScope.launch {
            try {
                val all = repository.observeAll().first()
                val chosen = all.filter { it.id in ids }
                if (chosen.size < 2) {
                    _message.value = "Some of those screenshots are no longer available."
                    return@launch
                }
                _result.value = withContext(Dispatchers.Default) {
                    ComparisonBuilder.build(chosen)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _message.value = "Couldn't build the comparison. Please try again."
            }
        }
    }

    fun closeResult() {
        _result.value = null
    }

    fun messageShown() {
        _message.value = null
    }

    private fun prepare(all: List<ScreenshotEntity>, query: String): List<ScreenshotEntity> {
        val tokens = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val matching = if (tokens.isEmpty()) {
            all
        } else {
            all.filter { s ->
                val haystack = listOfNotNull(
                    s.title, s.ocrText, s.keywords, s.category, s.note, s.fileName
                ).joinToString(" ").lowercase()
                tokens.all { it in haystack }
            }
        }
        return matching.sortedWith(
            compareByDescending<ScreenshotEntity> { it.category == "Shopping" }
                .thenByDescending { it.takenAt ?: it.importedAt }
        )
    }

    companion object {
        const val MAX_SELECTION = 4
    }
}
