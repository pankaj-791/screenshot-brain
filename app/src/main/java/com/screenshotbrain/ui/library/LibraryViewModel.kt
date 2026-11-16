package com.screenshotbrain.ui.library

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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DateFilter(val label: String, val days: Int?) {
    All("All time", null),
    Week("7 days", 7),
    Month("30 days", 30)
}

data class LibraryFilters(
    val query: String = "",
    val category: String? = null,
    val dateFilter: DateFilter = DateFilter.All,
    val newestFirst: Boolean = true,
    val favouritesOnly: Boolean = false
) {
    val isActive: Boolean
        get() = query.isNotBlank() || category != null ||
            dateFilter != DateFilter.All || favouritesOnly
}

data class LibraryUiState(
    val items: List<ScreenshotEntity> = emptyList(),
    val total: Int = 0
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ScreenshotRepository(
        context = application,
        dao = AppDatabase.get(application).screenshotDao()
    )

    private val _filters = MutableStateFlow(LibraryFilters())
    val filters: StateFlow<LibraryFilters> = _filters

    val uiState: StateFlow<LibraryUiState> = combine(repository.observeAll(), _filters) { all, f ->
        LibraryUiState(items = applyFilters(all, f), total = all.size)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    init {
        // Read the text of any screenshots that haven't been processed yet.
        viewModelScope.launch {
            try {
                repository.processPending()
                repository.refreshPrices()
            } catch (e: Exception) {
                // Not fatal: pending screenshots will be retried next time.
            }
        }
    }
    init {
        // A category tapped on the Home screen arrives here.
        viewModelScope.launch {
            LibraryRequests.pendingCategory.collect { category ->
                if (category != null) {
                    LibraryRequests.pendingCategory.value = null
                    _filters.update { LibraryFilters(category = category, newestFirst = it.newestFirst) }
                }
            }
        }
    }
    
    
    fun setQuery(query: String) = _filters.update { it.copy(query = query) }

    fun setCategory(category: String?) = _filters.update { it.copy(category = category) }

    fun setDateFilter(filter: DateFilter) = _filters.update { it.copy(dateFilter = filter) }

    fun toggleSort() = _filters.update { it.copy(newestFirst = !it.newestFirst) }

    fun toggleFavouritesOnly() = _filters.update { it.copy(favouritesOnly = !it.favouritesOnly) }

    fun clearFilters() = _filters.update {
        LibraryFilters(newestFirst = it.newestFirst)
    }

    fun toggleFavourite(screenshot: ScreenshotEntity) {
        viewModelScope.launch {
            try {
                repository.setFavourite(screenshot.id, !screenshot.isFavourite)
            } catch (e: Exception) {
                _message.value = "Couldn't update favourite."
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

    private fun effectiveDate(s: ScreenshotEntity): Long = s.takenAt ?: s.importedAt

    private fun applyFilters(
        all: List<ScreenshotEntity>,
        f: LibraryFilters
    ): List<ScreenshotEntity> {
        val tokens = f.query.trim().lowercase()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
        val cutoff = f.dateFilter.days?.let {
            System.currentTimeMillis() - it * 24L * 60L * 60L * 1000L
        }

        val filtered = all.filter { s ->
            (!f.favouritesOnly || s.isFavourite) &&
                (f.category == null || s.category == f.category) &&
                (cutoff == null || effectiveDate(s) >= cutoff) &&
                (tokens.isEmpty() || matchesAll(s, tokens))
        }

        return if (f.newestFirst) {
            filtered.sortedByDescending { effectiveDate(it) }
        } else {
            filtered.sortedBy { effectiveDate(it) }
        }
    }

    private fun matchesAll(s: ScreenshotEntity, tokens: List<String>): Boolean {
        val haystack = listOfNotNull(
            s.title, s.ocrText, s.keywords, s.category, s.note, s.fileName
        ).joinToString(" ").lowercase()
        return tokens.all { it in haystack }
    }

    companion object {
        val CATEGORIES = listOf(
            "Shopping", "Payments", "Education", "Social Media", "Travel",
            "Food", "Technology", "Documents", "Other"
        )
    }
}

/** Lets other screens (like Home) open the Library on a chosen category. */
object LibraryRequests {
    val pendingCategory = MutableStateFlow<String?>(null)
}
