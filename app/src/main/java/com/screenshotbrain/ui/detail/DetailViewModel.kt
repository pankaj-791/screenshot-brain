package com.screenshotbrain.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.screenshotbrain.database.AppDatabase
import com.screenshotbrain.database.ScreenshotEntity
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

    private val repository = ScreenshotRepository(
        context = application,
        dao = AppDatabase.get(application).screenshotDao()
    )

    val state: StateFlow<DetailState> = repository.observeById(id)
        .map { item -> if (item == null) DetailState.Missing else DetailState.Found(item) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailState.Loading)

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

                repository.updateDetails(id, title, category, note, price)
                        repository.updateDetails(id, title, category, note)
        viewModelScope.launch {
            try {
                repository.updateDetails(id, title, category, note)
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
