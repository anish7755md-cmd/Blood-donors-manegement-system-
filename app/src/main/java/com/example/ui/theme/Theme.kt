package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CrimsonPrimaryDark,
    secondary = CrimsonSecondaryDark,
    tertiary = CrimsonTertiaryDark,
    onTertiary = OnCrimsonTertiaryDark,
    background = CrimsonBackgroundDark,
    surface = CrimsonSurfaceDark,
    surfaceVariant = CrimsonSurfaceVariantDark,
    onPrimary = CrimsonBackgroundDark,
    onBackground = OnCrimsonBackgroundDark,
    onSurface = OnCrimsonSurfaceDark,
    onSurfaceVariant = OnCrimsonSurfaceVariantDark,
    outline = CrimsonOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = CrimsonPrimary,
    secondary = CrimsonSecondary,
    tertiary = CrimsonTertiary,
    onTertiary = OnCrimsonTertiary,
    background = CrimsonBackground,
    surface = CrimsonSurface,
    surfaceVariant = CrimsonSurfaceVariant,
    onPrimary = CrimsonSurface,
    onBackground = OnCrimsonBackground,
    onSurface = OnCrimsonSurface,
    onSurfaceVariant = OnCrimsonSurfaceVariant,
    outline = CrimsonOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
