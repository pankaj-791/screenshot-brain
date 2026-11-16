package com.screenshotbrain.ui.home

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.screenshotbrain.database.AppDatabase
import com.screenshotbrain.database.ScreenshotEntity
import com.screenshotbrain.repository.ScreenshotRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val total: Int = 0,
    val recent: List<ScreenshotEntity> = emptyList(),
    val favourites: List<ScreenshotEntity> = emptyList(),
    val categoryCounts: Map<String, Int> = emptyMap()
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ScreenshotRepository(
        context = application,
        dao = AppDatabase.get(application).screenshotDao()
    )

    val uiState: StateFlow<HomeUiState> = repository.observeAll()
        .map { all ->
            HomeUiState(
                total = all.size,
                recent = all.take(8),
                favourites = all.filter { it.isFavourite }.take(8),
                categoryCounts = all.mapNotNull { it.category }
                    .groupingBy { it }
                    .eachCount()
            )
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun importUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            try {
                val added = repository.importUris(uris)
                val skipped = uris.size - added
                _message.value = when {
                    added == 0 -> "Already imported"
                    skipped == 0 -> "$added screenshot(s) added"
                    else -> "$added added, $skipped already imported"
                }
            } catch (e: Exception) {
                _message.value = "Import failed. Please try again."
            }
        }
    }

    fun messageShown() {
        _message.value = null
    }
}
