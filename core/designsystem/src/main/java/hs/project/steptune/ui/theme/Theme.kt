package hs.project.steptune.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFBFC1FF),
    onPrimary = Color(0xFF202363),
    primaryContainer = Color(0xFF353982),
    onPrimaryContainer = Color(0xFFE2E2FF),
    secondary = Color(0xFF78D9C0),
    onSecondary = Color(0xFF00382D),
    secondaryContainer = Color(0xFF155043),
    onSecondaryContainer = Color(0xFF9EF5DA),
    tertiary = Color(0xFFFFB59F),
    onTertiary = Color(0xFF5D1708),
    background = StepBackgroundDark,
    onBackground = StepTextDark,
    surface = StepSurfaceDark,
    onSurface = StepTextDark,
    surfaceVariant = StepSurfaceSoftDark,
    onSurfaceVariant = StepMutedDark,
    outline = StepOutlineDark,
    error = Color(0xFFFFB4AB)
)

private val LightColorScheme = lightColorScheme(
    primary = StepPurpleDark,
    onPrimary = Color.White,
    primaryContainer = StepLavender,
    onPrimaryContainer = Color(0xFF24275F),
    secondary = StepMint,
    onSecondary = Color.White,
    secondaryContainer = StepMintSoft,
    onSecondaryContainer = Color(0xFF103C32),
    tertiary = StepCoral,
    onTertiary = Color(0xFF4B160B),
    background = StepBackgroundLight,
    onBackground = StepInk,
    surface = StepSurfaceLight,
    onSurface = StepInk,
    surfaceVariant = StepSurfaceSoftLight,
    onSurfaceVariant = StepMuted,
    outline = StepOutlineLight,
    error = Color(0xFFBA1A1A)
)

private val StepTuneShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun StepTuneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = StepTuneShapes,
        content = content
    )
}
