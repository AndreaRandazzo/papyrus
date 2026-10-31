package io.github.andrearandazzo.papyrus.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF7A4E1D),
    secondary = Color(0xFF8B6F47),
    background = Color(0xFFFFF8F1),
    surface = Color(0xFFFFF8F1),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF0BD7E),
    secondary = Color(0xFFDCC2A0),
)

/** Uses the wallpaper-based colors on Android 12+, papyrus tones otherwise. */
@Composable
fun PapyrusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
