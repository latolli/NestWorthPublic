package com.yourname.nestworth.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = DarkBrown,
    onPrimary = Yellow,
    secondary = LightBrown,
    onSecondary = NaturalWhite,
    background = BackgroundLight,
    onBackground = DarkBrown,
    surface = SurfaceLight,
    onSurface = DarkBrown,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = LightBrown,
)

private val DarkColorScheme = darkColorScheme(
    primary = Yellow,
    onPrimary = DarkBrown,
    secondary = NaturalWhite,
    onSecondary = DarkBrown,
    background = BackgroundDark,
    onBackground = NaturalWhite,
    surface = SurfaceDark,
    onSurface = NaturalWhite,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = NaturalWhite,
)

@Composable
fun NestWorthTheme(
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