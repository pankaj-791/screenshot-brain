package com.screenshotbrain.ui.collections

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.screenshotbrain.database.AppDatabase
import com.screenshotbrain.database.ScreenshotEntity
import com.screenshotbrain.repository.CollectionRepository
import com.screenshotbrain.repository.ScreenshotRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddScreenshotsState(
    val loaded: Boolean = false,
    val items: List<ScreenshotEntity> = emptyList(),
    val available: Int = 0,
    val libraryTotal: Int = 0
)

class AddScreenshotsViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val collectionId: Long = savedStateHandle["id"] ?: -1L

    private val database = AppDatabase.get(application)
    private val screenshots = ScreenshotRepository(application, database.screenshotDao())
    private val collections = CollectionRepository(database.collectionDao())

    private val _query = MutableStateFlow("")

    private val _selected = MutableStateFlow<Set<Long>>(emptySet())
    val selected: StateFlow<Set<Long>> = _selected

    val state: StateFlow<AddScreenshotsState> = combine(
        screenshots.observeAll(),
        collections.observeScreenshots(collectionId),
        _query
    ) { all, inCollection, q ->
        val alreadyIn = inCollection.map { it.id }.toSet()
        val candidates = all.filter { it.id !in alreadyIn }
        AddScreenshotsState(
            loaded = true,
            items = filter(candidates, q),
            available = candidates.size,
            libraryTotal = all.size
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddScreenshotsState())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun setQuery(value: String) {
        _query.value = value
    }

    fun toggle(id: Long) {
        _selected.update { if (id in it) it - id else it + id }
    }

    fun addSelected(onDone: (Int) -> Unit) {
        val ids = _selected.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            try {
                collections.addScreenshots(collectionId, ids)
                onDone(ids.size)
            } catch (e: Exception) {
                _message.value = "Couldn't add the screenshots. Please try again."
            }
        }
    }

    fun messageShown() {
        _message.value = null
    }

    private fun filter(list: List<ScreenshotEntity>, query: String): List<ScreenshotEntity> {
        val tokens = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return list
        return list.filter { s ->
            val haystack = listOfNotNull(
                s.title, s.ocrText, s.keywords, s.category, s.note, s.fileName
            ).joinToString(" ").lowercase()
            tokens.all { it in haystack }
        }
    }
}
