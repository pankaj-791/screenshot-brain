package com.screenshotbrain.ui.collections

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.screenshotbrain.database.AppDatabase
import com.screenshotbrain.database.CollectionWithInfo
import com.screenshotbrain.repository.CollectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CollectionsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CollectionRepository(
        AppDatabase.get(application).collectionDao()
    )

    /** null = still loading, empty list = no collections yet. */
    val collections: StateFlow<List<CollectionWithInfo>?> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun create(name: String) {
        viewModelScope.launch {
            try {
                val id = repository.create(name)
                _message.value = if (id == null) "Please enter a name." else "Collection created"
            } catch (e: Exception) {
                _message.value = "Couldn't create the collection."
            }
        }
    }

    fun rename(id: Long, name: String) {
        viewModelScope.launch {
            try {
                val ok = repository.rename(id, name)
                _message.value = if (ok) "Renamed" else "Please enter a name."
            } catch (e: Exception) {
                _message.value = "Couldn't rename the collection."
            }
        }
    }

    fun toggleFavourite(collection: CollectionWithInfo) {
        viewModelScope.launch {
            try {
                repository.setFavourite(collection.id, !collection.isFavourite)
            } catch (e: Exception) {
                _message.value = "Couldn't update favourite."
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            try {
                repository.delete(id)
                _message.value = "Collection deleted. Its screenshots are still in your library."
            } catch (e: Exception) {
                _message.value = "Couldn't delete the collection."
            }
        }
    }

    fun messageShown() {
        _message.value = null
    }
}
