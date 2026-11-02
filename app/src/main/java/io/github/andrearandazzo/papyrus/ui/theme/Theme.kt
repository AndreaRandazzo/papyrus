package io.github.andrearandazzo.papyrus.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.andrearandazzo.papyrus.R
import io.github.andrearandazzo.papyrus.data.AppPalette

// Base colors from the design style sheet (design/papyrus-papyrus-style-sheet*.png); each palette
// (Settings > Colors) then sets its own accents and tints the surfaces, see [colorScheme].

/** Warm paper tone for document placeholders and illustrations, the same in light and dark. */
val Paper = Color(0xFFEEE5CE)

/** Word documents have no preview; they get a tile in Word's familiar blue. */
val WordBlue = Color(0xFF3E5FA8)

private val LightBase = lightColorScheme(
    primary = Color(0xFF526351),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5E8D0),
    onPrimaryContainer = Color(0xFF1E2B1D),
    secondary = Color(0xFF5A6359),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5E8D0),
    onSecondaryContainer = Color(0xFF1E2B1D),
    tertiary = Color(0xFF6E5E3B),
    tertiaryContainer = Paper,
    onTertiaryContainer = Color(0xFF2A2210),
    background = Color(0xFFFAF8F3),
    onBackground = Color(0xFF1C1C18),
    surface = Color(0xFFFAF8F3),
    onSurface = Color(0xFF1C1C18),
    surfaceVariant = Color(0xFFE4E3DB),
    onSurfaceVariant = Color(0xFF55594F),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF4F2EC),
    surfaceContainer = Color(0xFFEEEDE6),
    surfaceContainerHigh = Color(0xFFE9E8E1),
    surfaceContainerHighest = Color(0xFFE3E2DB),
    outline = Color(0xFF767B71),
    outlineVariant = Color(0xFFD6D5CC),
    // Snackbars use the inverse colors; without these their action text is Material's default purple.
    inverseSurface = Color(0xFF30322D),
    inverseOnSurface = Color(0xFFF2F1EA),
    inversePrimary = Color(0xFFB4CCB0),
    error = Color(0xFF9B442F),
    onError = Color.White,
    errorContainer = Color(0xFFF7DDD5),
    onErrorContainer = Color(0xFF3E0E03),
)

private val DarkBase = darkColorScheme(
    primary = Color(0xFFB4CCB0),
    onPrimary = Color(0xFF213520),
    primaryContainer = Color(0xFF3B4B3B),
    onPrimaryContainer = Color(0xFFD5E8D0),
    secondary = Color(0xFFBEC8BA),
    onSecondary = Color(0xFF283227),
    secondaryContainer = Color(0xFF3B4B3B),
    onSecondaryContainer = Color(0xFFD5E8D0),
    tertiary = Color(0xFFDDC9A0),
    tertiaryContainer = Color(0xFF4E4430),
    onTertiaryContainer = Paper,
    background = Color(0xFF131613),
    onBackground = Color(0xFFE3E3DC),
    surface = Color(0xFF131613),
    onSurface = Color(0xFFE3E3DC),
    surfaceVariant = Color(0xFF42483F),
    onSurfaceVariant = Color(0xFFC2C7BD),
    surfaceContainerLowest = Color(0xFF0E110E),
    surfaceContainerLow = Color(0xFF1A1D19),
    surfaceContainer = Color(0xFF1F231F),
    surfaceContainerHigh = Color(0xFF262A25),
    surfaceContainerHighest = Color(0xFF30342F),
    outline = Color(0xFF8C9288),
    outlineVariant = Color(0xFF42483F),
    inverseSurface = Color(0xFFE3E3DC),
    inverseOnSurface = Color(0xFF30322D),
    inversePrimary = Color(0xFF526351),
    error = Color(0xFFF2A48F),
    onError = Color(0xFF5A1A0C),
    errorContainer = Color(0xFF7A2E1C),
    onErrorContainer = Color(0xFFFFDAD2),
)

/** Nunito Sans (SIL Open Font License, see licenses/), one variable font file for every weight. */
private val NunitoSans = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold).map {
        Font(R.font.nunito_sans, it, variationSettings = FontVariation.Settings(FontVariation.weight(it.weight)))
    },
)

private val PapyrusTypography = Typography().let { base ->
    fun TextStyle.nunito(weight: FontWeight) = copy(fontFamily = NunitoSans, fontWeight = weight)
    Typography(
        displayLarge = base.displayLarge.nunito(FontWeight.Bold),
        displayMedium = base.displayMedium.nunito(FontWeight.Bold),
        displaySmall = TextStyle(fontFamily = NunitoSans, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
        headlineLarge = base.headlineLarge.nunito(FontWeight.ExtraBold),
        headlineMedium = base.headlineMedium.nunito(FontWeight.ExtraBold),
        headlineSmall = base.headlineSmall.nunito(FontWeight.ExtraBold),
        titleLarge = TextStyle(fontFamily = NunitoSans, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, lineHeight = 28.sp),
        titleMedium = base.titleMedium.nunito(FontWeight.ExtraBold).copy(fontSize = 17.sp),
        titleSmall = base.titleSmall.nunito(FontWeight.ExtraBold),
        bodyLarge = TextStyle(fontFamily = NunitoSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = base.bodyMedium.nunito(FontWeight.SemiBold),
        bodySmall = base.bodySmall.nunito(FontWeight.SemiBold),
        labelLarge = base.labelLarge.nunito(FontWeight.Bold),
        labelMedium = TextStyle(
            fontFamily = NunitoSans, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp,
            letterSpacing = 0.8.sp,
        ),
        labelSmall = base.labelSmall.nunito(FontWeight.Bold),
    )
}

private val PapyrusShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Accent colors of a palette for one of light or dark. */
private class Accents(
    val primary: Color,
    val onPrimary: Color,
    /** Selected chips, the selected tab, the "+" button. */
    val container: Color,
    val onContainer: Color,
    /** The other way round from [primary], for snackbar actions. */
    val inversePrimary: Color,
)

/** Light and dark accents, plus (light only) surfaces tinted towards the accent. */
private class PaletteColors(val light: Accents, val dark: Accents, val lightSurfaces: List<Color>? = null)

private val Palettes = mapOf(
    AppPalette.TEAL to PaletteColors(
        light = Accents(Color(0xFF0F6E66), Color.White, Color(0xFFBDEEE6), Color(0xFF00201D), Color(0xFF7FD8CC)),
        dark = Accents(Color(0xFF7FD8CC), Color(0xFF003732), Color(0xFF00504A), Color(0xFFBDEEE6), Color(0xFF0F6E66)),
        lightSurfaces = listOf(0xFFF6FAF9, 0xFFEFF5F3, 0xFFE8F0EE, 0xFFE2EBE9, 0xFFDCE5E3).map(::Color),
    ),
    AppPalette.SAGE to PaletteColors(
        light = Accents(Color(0xFF2F6B45), Color.White, Color(0xFFC2EBCB), Color(0xFF0B2915), Color(0xFF8FD5A4)),
        dark = Accents(Color(0xFF8FD5A4), Color(0xFF00391B), Color(0xFF1E5233), Color(0xFFC2EBCB), Color(0xFF2F6B45)),
        lightSurfaces = listOf(0xFFF8FAF4, 0xFFF0F5EE, 0xFFE9F0E6, 0xFFE3EBE0, 0xFFDDE6DA).map(::Color),
    ),
    // Green text and icons with warm apricot highlights, on the original warm paper surfaces.
    AppPalette.SUN to PaletteColors(
        light = Accents(Color(0xFF2F6B45), Color.White, Color(0xFFFFDDA8), Color(0xFF2B1700), Color(0xFF8FD5A4)),
        dark = Accents(Color(0xFF8FD5A4), Color(0xFF00391B), Color(0xFF5C3F12), Color(0xFFFFDDA8), Color(0xFF2F6B45)),
    ),
)

private fun colorScheme(palette: AppPalette, dark: Boolean): ColorScheme {
    val colors = Palettes.getValue(palette)
    val accents = if (dark) colors.dark else colors.light
    val scheme = (if (dark) DarkBase else LightBase).copy(
        primary = accents.primary,
        onPrimary = accents.onPrimary,
        primaryContainer = accents.container,
        onPrimaryContainer = accents.onContainer,
        secondaryContainer = accents.container,
        onSecondaryContainer = accents.onContainer,
        inversePrimary = accents.inversePrimary,
    )
    val surfaces = colors.lightSurfaces?.takeUnless { dark } ?: return scheme
    return scheme.copy(
        background = surfaces[0],
        surface = surfaces[0],
        surfaceContainerLow = surfaces[1],
        surfaceContainer = surfaces[2],
        surfaceContainerHigh = surfaces[3],
        surfaceContainerHighest = surfaces[4],
    )
}

/** The Papyrus look: the chosen palette, Nunito Sans, soft rounded shapes. */
@Composable
fun PapyrusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: AppPalette = AppPalette.TEAL,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = remember(palette, darkTheme) { colorScheme(palette, darkTheme) },
        typography = PapyrusTypography,
        shapes = PapyrusShapes,
        content = content,
    )
}
