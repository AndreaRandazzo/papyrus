package io.github.andrearandazzo.papyrus.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.andrearandazzo.papyrus.R

// Palette from the design style sheet (design/papyrus-papyrus-style-sheet*.png).

/** Warm paper tone for document placeholders and illustrations, the same in light and dark. */
val Paper = Color(0xFFEEE5CE)

/** Word documents have no preview; they get a tile in Word's familiar blue. */
val WordBlue = Color(0xFF3E5FA8)

private val LightColors = lightColorScheme(
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

private val DarkColors = darkColorScheme(
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
        titleMedium = base.titleMedium.nunito(FontWeight.Bold).copy(fontSize = 17.sp),
        titleSmall = base.titleSmall.nunito(FontWeight.Bold),
        bodyLarge = TextStyle(fontFamily = NunitoSans, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = base.bodyMedium.nunito(FontWeight.Medium),
        bodySmall = base.bodySmall.nunito(FontWeight.Medium),
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

/** The Papyrus look: sage green on warm paper, Nunito Sans, soft rounded shapes. */
@Composable
fun PapyrusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = PapyrusTypography,
        shapes = PapyrusShapes,
        content = content,
    )
}
