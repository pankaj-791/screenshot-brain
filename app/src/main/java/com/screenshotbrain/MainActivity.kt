package com.screenshotbrain

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.screenshotbrain.data.AppSettings
import com.screenshotbrain.data.SettingsStore
import com.screenshotbrain.data.ThemeMode
import com.screenshotbrain.navigation.ScreenshotBrainApp
import com.screenshotbrain.ui.theme.ScreenshotBrainTheme

class MainActivity : ComponentActivity() {

    private val store by lazy { SettingsStore(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val loaded by store.settings.collectAsStateWithLifecycle<AppSettings?>(
                initialValue = null
            )
            val settings = loaded
            if (settings != null) {
                val systemDark = isSystemInDarkTheme()
                val dark = when (settings.themeMode) {
                    ThemeMode.System -> systemDark
                    ThemeMode.Light -> false
                    ThemeMode.Dark -> true
                }

                DisposableEffect(dark) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.auto(
                            Color.TRANSPARENT,
                            Color.TRANSPARENT
                        ) { dark },
                        navigationBarStyle = SystemBarStyle.auto(
                            LIGHT_SCRIM,
                            DARK_SCRIM
                        ) { dark }
                    )
                    onDispose { }
                }

                ScreenshotBrainTheme(darkTheme = dark, accent = settings.accent) {
                    ScreenshotBrainApp()
                }
            }
        }
    }

    companion object {
        private val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        private val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
