package com.screenshotbrain.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.screenshotbrain.database.AppDatabase
import com.screenshotbrain.database.CollectionWithInfo
import com.screenshotbrain.database.ScreenshotEntity
import com.screenshotbrain.repository.CollectionRepository
import com.screenshotbrain.repository.ScreenshotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailState {
    object Loading : DetailState
    object Missing : DetailState
    data class Found(val item: ScreenshotEntity) : DetailState
}

class DetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val id: Long = savedStateHandle["id"] ?: -1L

    private val database = AppDatabase.get(application)

    private val repository = ScreenshotRepository(
        context = application,
        dao = database.screenshotDao()
    )

    private val collectionRepository = CollectionRepository(database.collectionDao())

    val state: StateFlow<DetailState> = repository.observeById(id)
        .map { item -> if (item == null) DetailState.Missing else DetailState.Found(item) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailState.Loading)

    val allCollections: StateFlow<List<CollectionWithInfo>> = collectionRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val memberOf: StateFlow<Set<Long>> = collectionRepository.observeCollectionIdsFor(id)
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun toggleFavourite(current: ScreenshotEntity) {
        viewModelScope.launch {
            try {
                repository.setFavourite(current.id, !current.isFavourite)
            } catch (e: Exception) {
                _message.value = "Couldn't update favourite."
            }
        }
    }

    fun setInCollection(collectionId: Long, add: Boolean) {
        viewModelScope.launch {
            try {
                if (add) {
                    collectionRepository.addScreenshots(collectionId, listOf(id))
                } else {
                    collectionRepository.removeScreenshot(collectionId, id)
                }
            } catch (e: Exception) {
                _message.value = "Couldn't update the collection."
            }
        }
    }

    fun save(title: String, category: String, note: String, price: String) {
        viewModelScope.launch {
            try {
                repository.updateDetails(id, title, category, note, price)
                _message.value = "Saved"
            } catch (e: Exception) {
                _message.value = "Couldn't save changes."
            }
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.delete(id)
                onDone()
            } catch (e: Exception) {
                _message.value = "Couldn't delete. Please try again."
            }
        }
    }

    fun messageShown() {
        _message.value = null
    }
}
