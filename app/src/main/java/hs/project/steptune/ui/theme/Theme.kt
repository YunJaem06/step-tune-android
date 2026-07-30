package hs.project.steptune.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = StepGreen,
    onPrimary = StepBlack,
    primaryContainer = StepGreenContainer,
    onPrimaryContainer = StepText,
    secondary = StepLime,
    onSecondary = StepBlack,
    secondaryContainer = Color(0xFF1D3D23),
    onSecondaryContainer = StepText,
    tertiary = StepAmber,
    onTertiary = StepBlack,
    tertiaryContainer = StepAmberContainer,
    onTertiaryContainer = StepText,
    background = StepBlack,
    onBackground = StepText,
    surface = StepCharcoal,
    onSurface = StepText,
    surfaceVariant = StepCard,
    onSurfaceVariant = StepTextMuted,
    outline = StepStroke,
    error = StepError,
    errorContainer = Color(0xFF3E1D24),
    onErrorContainer = Color(0xFFFFD7DC)
)

private val LightColorScheme = lightColorScheme(
    primary = StepGreenDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1F8EA),
    onPrimaryContainer = Color(0xFF062315),
    secondary = StepGreen,
    onSecondary = Color.White,
    tertiary = StepAmber,
    onTertiary = StepBlack,
    background = Color(0xFFF4F8F5),
    onBackground = StepBlack,
    surface = Color.White,
    onSurface = StepBlack,
    surfaceVariant = Color(0xFFE8EEE9),
    onSurfaceVariant = Color(0xFF53605A),
    outline = Color(0xFFBAC6BF),
    error = Color(0xFFB3261E)
)

@Composable
fun StepTuneTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
