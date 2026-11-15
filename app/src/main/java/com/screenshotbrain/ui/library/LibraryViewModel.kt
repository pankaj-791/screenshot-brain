package com.screenshotbrain.ui.library

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.screenshotbrain.database.AppDatabase
import com.screenshotbrain.database.ScreenshotEntity
import com.screenshotbrain.repository.ScreenshotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ScreenshotRepository(
        context = application,
        dao = AppDatabase.get(application).screenshotDao()
    )

    val screenshots: StateFlow<List<ScreenshotEntity>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    init {
        // Read the text of any screenshots that haven't been processed yet.
        viewModelScope.launch {
            try {
                repository.processPending()
            } catch (e: Exception) {
                // Not fatal: pending screenshots will be retried next time.
            }
        }
    }

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
