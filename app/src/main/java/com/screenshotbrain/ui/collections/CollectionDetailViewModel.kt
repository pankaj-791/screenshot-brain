package com.screenshotbrain.ui.collections

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.screenshotbrain.database.AppDatabase
import com.screenshotbrain.database.CollectionEntity
import com.screenshotbrain.database.ScreenshotEntity
import com.screenshotbrain.repository.CollectionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CollectionDetailState(
    val loaded: Boolean = false,
    val collection: CollectionEntity? = null,
    val items: List<ScreenshotEntity> = emptyList(),
    val total: Int = 0
)

class CollectionDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val collectionId: Long = savedStateHandle["id"] ?: -1L

    private val repository = CollectionRepository(
        AppDatabase.get(application).collectionDao()
    )

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val state: StateFlow<CollectionDetailState> = combine(
        repository.observeById(collectionId),
        repository.observeScreenshots(collectionId),
        _query
    ) { collection, all, q ->
        CollectionDetailState(
            loaded = true,
            collection = collection,
            items = filter(all, q),
            total = all.size
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionDetailState())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun setQuery(value: String) {
        _query.value = value
    }

    fun remove(screenshotId: Long) {
        viewModelScope.launch {
            try {
                repository.removeScreenshot(collectionId, screenshotId)
                _message.value = "Removed from collection"
            } catch (e: Exception) {
                _message.value = "Couldn't remove it."
            }
        }
    }

    fun setCover(screenshotId: Long) {
        viewModelScope.launch {
            try {
                repository.setCover(collectionId, screenshotId)
                _message.value = "Cover updated"
            } catch (e: Exception) {
                _message.value = "Couldn't set the cover."
            }
        }
    }

    fun messageShown() {
        _message.value = null
    }

    private fun filter(all: List<ScreenshotEntity>, query: String): List<ScreenshotEntity> {
        val tokens = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return all
        return all.filter { s ->
            val haystack = listOfNotNull(
                s.title, s.ocrText, s.keywords, s.category, s.note, s.fileName
            ).joinToString(" ").lowercase()
            tokens.all { it in haystack }
        }
    }
}
