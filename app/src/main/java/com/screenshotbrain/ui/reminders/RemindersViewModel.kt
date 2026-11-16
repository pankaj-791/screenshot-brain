package com.screenshotbrain.ui.reminders

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.screenshotbrain.database.AppDatabase
import com.screenshotbrain.database.ReminderWithInfo
import com.screenshotbrain.notifications.NotificationHelper
import com.screenshotbrain.repository.ReminderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RemindersState(
    val loaded: Boolean = false,
    val upcoming: List<ReminderWithInfo> = emptyList(),
    val done: List<ReminderWithInfo> = emptyList()
)

class RemindersViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application

    private val repository = ReminderRepository(
        context = application,
        dao = AppDatabase.get(application).reminderDao()
    )

    val state: StateFlow<RemindersState> = repository.observeAll()
        .map { all ->
            RemindersState(
                loaded = true,
                upcoming = all.filter { !it.isDone }.sortedBy { it.remindAt },
                done = all.filter { it.isDone }.sortedByDescending { it.remindAt }
            )
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RemindersState())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    init {
        viewModelScope.launch {
            try {
                repository.rescheduleAll()
            } catch (e: Exception) {
                // Not fatal: the scheduled jobs are still kept by Android.
            }
        }
    }

    fun notificationsEnabled(): Boolean = NotificationHelper.canNotify(app)

    fun create(title: String, note: String?, remindAt: Long, screenshotId: Long? = null) {
        if (remindAt <= System.currentTimeMillis()) {
            _message.value = "Please choose a time in the future."
            return
        }
        viewModelScope.launch {
            try {
                val id = repository.create(title, note, screenshotId, remindAt)
                _message.value = when {
                    id == null -> "Please enter a title."
                    !NotificationHelper.canNotify(app) ->
                        "Reminder saved, but notifications are off, so it can't alert you. " +
                            "Turn them on in Android settings."
                    else -> "Reminder set"
                }
            } catch (e: Exception) {
                _message.value = "Couldn't save the reminder."
            }
        }
    }

    fun update(
        id: Long,
        title: String,
        note: String?,
        remindAt: Long,
        screenshotId: Long? = null
    ) {
        if (remindAt <= System.currentTimeMillis()) {
            _message.value = "Please choose a time in the future."
            return
        }
        viewModelScope.launch {
            try {
                val ok = repository.update(id, title, note, screenshotId, remindAt)
                _message.value = if (ok) "Reminder updated" else "Please enter a title."
            } catch (e: Exception) {
                _message.value = "Couldn't update the reminder."
            }
        }
    }

    fun markDone(id: Long) {
        viewModelScope.launch {
            try {
                repository.markDone(id)
            } catch (e: Exception) {
                _message.value = "Couldn't update the reminder."
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            try {
                repository.delete(id)
                _message.value = "Reminder deleted"
            } catch (e: Exception) {
                _message.value = "Couldn't delete the reminder."
            }
        }
    }

    fun messageShown() {
        _message.value = null
    }
}
