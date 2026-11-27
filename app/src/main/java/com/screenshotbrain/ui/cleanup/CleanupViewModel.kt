package com.screenshotbrain.ui.cleanup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.screenshotbrain.database.AppDatabase
import com.screenshotbrain.database.CleanupCandidate
import com.screenshotbrain.domain.CleanupSuggestions
import com.screenshotbrain.repository.CleanupRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CleanupUiState(
    val loaded: Boolean = false,
    val suggestions: CleanupSuggestions = CleanupSuggestions(),
    val unanalyzed: Int = 0,
    val selected: Set<Long> = emptySet(),
    val organizedSelected: Int = 0,
    val busy: Boolean = false
)

class CleanupViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CleanupRepository(application, AppDatabase.get(application))

    private val _selected = MutableStateFlow<Set<Long>>(emptySet())
    private val _busy = MutableStateFlow(false)

    val state: StateFlow<CleanupUiState> = combine(
        repository.observeSuggestions(),
        repository.observeUnanalyzedCount(),
        _selected,
        _busy
    ) { suggestions, unanalyzed, selected, busy ->
        val byId = allOf(suggestions).associateBy { it.id }
        val valid = selected.filter { it in byId }.toSet()
        CleanupUiState(
            loaded = true,
            suggestions = suggestions,
            unanalyzed = unanalyzed,
            selected = valid,
            organizedSelected = valid.count { id -> byId[id]?.let { isOrganized(it) } == true },
            busy = busy
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CleanupUiState())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    init {
        viewModelScope.launch {
            try {
                repository.startAnalysis()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Not fatal: unanalyzed screenshots are retried next time.
            }
        }
    }

    fun toggle(id: Long) = _selected.update { if (id in it) it - id else it + id }

    fun select(ids: Collection<Long>) = _selected.update { it + ids }

    fun clearSelection() {
        _selected.value = emptySet()
    }

    fun removeSelected() {
        val ids = _selected.value
        if (ids.isEmpty() || _busy.value) return
        viewModelScope.launch {
            _busy.value = true
            try {
                repository.removeFromApp(ids)
                _selected.value = emptySet()
                _message.value = "${ids.size} screenshot(s) removed from Screenshot Brain"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _message.value = "Couldn't remove them. Please try again."
            } finally {
                _busy.value = false
            }
        }
    }

    fun retry(ids: Collection<Long>) {
        viewModelScope.launch {
            try {
                repository.retry(ids)
                _message.value = "Checking those images again"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _message.value = "Couldn't check them again."
            }
        }
    }

    fun messageShown() {
        _message.value = null
    }

    private fun allOf(s: CleanupSuggestions): List<CleanupCandidate> =
        s.duplicateGroups.flatten() + s.similarGroups.flatten() +
            s.blurry + s.old + s.unused + s.unavailable

    private fun isOrganized(c: CleanupCandidate): Boolean =
        c.isFavourite || c.collectionCount > 0 || c.reminderCount > 0 || !c.note.isNullOrBlank()
}
