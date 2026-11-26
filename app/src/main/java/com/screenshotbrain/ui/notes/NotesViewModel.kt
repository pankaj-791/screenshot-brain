package com.screenshotbrain.ui.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.screenshotbrain.database.AppDatabase
import com.screenshotbrain.domain.ChecklistItem
import com.screenshotbrain.domain.NoteTools
import com.screenshotbrain.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotesUiState(
    val loaded: Boolean = false,
    val missing: Boolean = false,
    val title: String = "",
    val ocrText: String = "",
    val hasSavedNote: Boolean = false,
    val checklistMode: Boolean = false,
    val text: String = "",
    val items: List<ChecklistItem> = emptyList(),
    val canSave: Boolean = false,
    val edited: Boolean = false
)

/** The note as it would be saved right now. */
fun NotesUiState.content(): String =
    if (checklistMode) {
        NoteTools.formatChecklist(items.filter { it.text.isNotBlank() })
    } else {
        text
    }

class NotesViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val id: Long = savedStateHandle["id"] ?: -1L

    private val repository = NoteRepository(AppDatabase.get(application).screenshotDao())

    private val _state = MutableStateFlow(NotesUiState())
    val state: StateFlow<NotesUiState> = _state

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private var started = false
    private var savedContent = ""
    private var baseline = ""

    init {
        viewModelScope.launch {
            repository.observe(id).collect { screenshot ->
                if (screenshot == null) {
                    _state.update { it.copy(loaded = true, missing = true) }
                    return@collect
                }
                val saved = screenshot.note.orEmpty()
                savedContent = saved
                val ocr = screenshot.ocrText.orEmpty()
                val title = screenshot.title ?: screenshot.fileName

                if (!started) {
                    started = true
                    val asChecklist = saved.isNotEmpty() && NoteTools.isChecklist(saved)
                    val initial = NotesUiState(
                        loaded = true,
                        title = title,
                        ocrText = ocr,
                        hasSavedNote = saved.isNotEmpty(),
                        checklistMode = asChecklist,
                        text = if (asChecklist) "" else saved.ifEmpty { ocr },
                        items = if (asChecklist) NoteTools.parseChecklist(saved) else emptyList()
                    )
                    baseline = initial.content()
                    _state.value = initial
                } else {
                    _state.update {
                        it.copy(title = title, ocrText = ocr, hasSavedNote = saved.isNotEmpty())
                    }
                }
                refresh()
            }
        }
    }

    fun setText(value: String) = change { it.copy(text = value) }

    fun toggleItem(index: Int) = change { s ->
        s.copy(
            items = s.items.mapIndexed { i, item ->
                if (i == index) item.copy(checked = !item.checked) else item
            }
        )
    }

    fun editItem(index: Int, value: String) = change { s ->
        s.copy(
            items = s.items.mapIndexed { i, item ->
                if (i == index) item.copy(text = value) else item
            }
        )
    }

    fun addItem() = change { it.copy(items = it.items + ChecklistItem("", false)) }

    fun removeItem(index: Int) = change { s ->
        s.copy(items = s.items.filterIndexed { i, _ -> i != index })
    }

    fun makeChecklist() {
        val items = NoteTools.parseChecklist(_state.value.text)
        if (items.isEmpty()) {
            _message.value = "There is no text to turn into a checklist."
            return
        }
        change { it.copy(checklistMode = true, items = items) }
    }

    fun backToText() = change { it.copy(checklistMode = false, text = it.content()) }

    fun useExtractedText() {
        val ocr = _state.value.ocrText
        if (ocr.isBlank()) {
            _message.value = "This screenshot has no extracted text."
            return
        }
        change { s ->
            if (s.checklistMode) {
                s.copy(items = NoteTools.parseChecklist(ocr))
            } else {
                s.copy(text = ocr)
            }
        }
    }

    fun addSummary() {
        val s = _state.value
        val source = if (s.checklistMode) s.items.joinToString("\n") { it.text } else s.text
        val summary = NoteTools.summarize(source)
        if (summary == null) {
            _message.value = "The text is too short to shorten."
            return
        }
        val base = s.content().trim()
        val joined = (if (base.isEmpty()) "" else "$base\n\n") + "Key lines:\n" + summary
        change { it.copy(checklistMode = false, text = joined) }
    }

    fun save() {
        val content = _state.value.content().trim()
        viewModelScope.launch {
            try {
                repository.saveNote(id, content)
                baseline = content
                refresh()
                _message.value = if (content.isEmpty()) "Note cleared" else "Note saved"
            } catch (e: Exception) {
                _message.value = "Couldn't save the note."
            }
        }
    }

    fun messageShown() {
        _message.value = null
    }

    private fun change(block: (NotesUiState) -> NotesUiState) {
        _state.update(block)
        refresh()
    }

    private fun refresh() {
        _state.update { s ->
            val content = s.content().trim()
            s.copy(
                canSave = content != savedContent.trim(),
                edited = content != baseline.trim()
            )
        }
    }
}
