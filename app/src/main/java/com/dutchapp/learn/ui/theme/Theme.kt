package com.dutchapp.learn.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Orange,
    onPrimary = SurfaceLight,
    primaryContainer = OrangeContainer,
    onPrimaryContainer = OrangeDark,
    secondary = SkyBlue,
    onSecondary = SurfaceLight,
    secondaryContainer = SkyBlueContainer,
    onSecondaryContainer = DeepBlue,
    tertiary = LeafGreen,
    onTertiary = SurfaceLight,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = OrangeContainer,
    onSurfaceVariant = OnSurfaceLight,
    outline = OutlineLight,
    error = ErrorRed,
    onError = SurfaceLight
)

private val DarkColors = darkColorScheme(
    primary = OrangeLight,
    onPrimary = BackgroundDark,
    primaryContainer = OrangeDark,
    onPrimaryContainer = OrangeContainer,
    secondary = SkyBlue,
    onSecondary = BackgroundDark,
    tertiary = LeafGreen,
    onTertiary = BackgroundDark,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = OnSurfaceDark,
    outline = OutlineDark,
    error = ErrorRed,
    onError = BackgroundDark
)

@Composable
fun DutchLearnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}
