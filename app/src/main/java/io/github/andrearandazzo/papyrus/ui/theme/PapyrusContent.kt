package io.github.andrearandazzo.papyrus.ui.theme

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.andrearandazzo.papyrus.PapyrusApp
import io.github.andrearandazzo.papyrus.data.ThemeMode

/**
 * Shows [content] edge to edge in the Papyrus theme, following the theme chosen in Settings.
 * Every activity uses this, so they all look and behave the same.
 */
fun ComponentActivity.setPapyrusContent(content: @Composable () -> Unit) {
    enableEdgeToEdge()
    val preferences = (application as PapyrusApp).preferences
    setContent {
        val themeMode by preferences.themeMode.collectAsStateWithLifecycle()
        val darkTheme = when (themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
        // Status and navigation bar icons follow the app's theme, not only the phone's.
        DisposableEffect(darkTheme) {
            val style = if (darkTheme) {
                SystemBarStyle.dark(Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
            }
            enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            onDispose {}
        }
        PapyrusTheme(darkTheme = darkTheme, content = content)
    }
}
