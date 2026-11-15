package com.screenshotbrain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.screenshotbrain.navigation.ScreenshotBrainApp
import com.screenshotbrain.ui.theme.ScreenshotBrainTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ScreenshotBrainTheme {
                ScreenshotBrainApp()
            }
        }
    }
}
