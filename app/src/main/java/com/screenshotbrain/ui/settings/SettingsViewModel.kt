package com.screenshotbrain.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.screenshotbrain.data.AccentColor
import com.screenshotbrain.data.AppSettings
import com.screenshotbrain.data.DataManager
import com.screenshotbrain.data.SettingsStore
import com.screenshotbrain.data.StorageStats
import com.screenshotbrain.data.ThemeMode
import com.screenshotbrain.notifications.NotificationHelper
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val stats: StorageStats? = null,
    val notificationsEnabled: Boolean = true,
    val busy: Boolean = false
)

private data class Extras(
    val stats: StorageStats? = null,
    val notificationsEnabled: Boolean = true,
    val busy: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application
    private val store = SettingsStore(application)
    private val data = DataManager(application)

    val versionName: String = data.versionName()

    private val extras = MutableStateFlow(Extras())

    val state: StateFlow<SettingsUiState> = combine(store.settings, extras) { settings, e ->
        SettingsUiState(settings, e.stats, e.notificationsEnabled, e.busy)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    /** Call whenever the screen comes back into view. */
    fun refresh() {
        extras.update { it.copy(notificationsEnabled = NotificationHelper.canNotify(app)) }
        viewModelScope.launch {
            try {
                val stats = data.stats()
                extras.update { it.copy(stats = stats) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Storage numbers are optional. Leave the old ones.
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { store.setThemeMode(mode) }
    }

    fun setAccent(accent: AccentColor) {
        viewModelScope.launch { store.setAccent(accent) }
    }

    fun exportTo(uri: Uri) {
        if (extras.value.busy) return
        viewModelScope.launch {
            extras.update { it.copy(busy = true) }
            try {
                val json = data.exportJson()
                withContext(Dispatchers.IO) {
                    val stream = app.contentResolver.openOutputStream(uri, "wt")
                        ?: throw IOException("Could not open the file.")
                    stream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                }
                _message.value = "Data exported"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _message.value = "Couldn't export your data."
            } finally {
                extras.update { it.copy(busy = false) }
            }
        }
    }

    fun deleteAll() {
        if (extras.value.busy) return
        viewModelScope.launch {
            extras.update { it.copy(busy = true) }
            try {
                data.deleteAll()
                _message.value = "All app data deleted"
                refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _message.value = "Couldn't delete everything. Please try again."
            } finally {
                extras.update { it.copy(busy = false) }
            }
        }
    }

    fun messageShown() {
        _message.value = null
    }
}
