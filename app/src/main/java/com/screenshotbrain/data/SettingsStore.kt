package com.screenshotbrain.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

enum class ThemeMode(val label: String) {
    System("Follow system"),
    Light("Light"),
    Dark("Dark")
}

enum class AccentColor(val label: String, val argb: Long) {
    Violet("Violet", 0xFF6B4EE6),
    Blue("Blue", 0xFF2F6FED),
    Teal("Teal", 0xFF0F9D8A),
    Green("Green", 0xFF3E9B4F),
    Rose("Rose", 0xFFD6336C),
    Orange("Orange", 0xFFE8590C)
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val accent: AccentColor = AccentColor.Violet
)

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsStore(context: Context) {

    private val appContext = context.applicationContext
    private val themeKey = stringPreferencesKey("theme_mode")
    private val accentKey = stringPreferencesKey("accent")

    val settings: Flow<AppSettings> = appContext.settingsDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            AppSettings(
                themeMode = ThemeMode.values().firstOrNull { it.name == prefs[themeKey] }
                    ?: ThemeMode.System,
                accent = AccentColor.values().firstOrNull { it.name == prefs[accentKey] }
                    ?: AccentColor.Violet
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        appContext.settingsDataStore.edit { it[themeKey] = mode.name }
    }

    suspend fun setAccent(accent: AccentColor) {
        appContext.settingsDataStore.edit { it[accentKey] = accent.name }
    }

    suspend fun reset() {
        appContext.settingsDataStore.edit { it.clear() }
    }
}
