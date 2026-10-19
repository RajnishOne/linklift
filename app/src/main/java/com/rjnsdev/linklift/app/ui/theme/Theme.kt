package com.rjnsdev.linklift.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = LinkLiftTextPrimary,
    onPrimary = LinkLiftBackground,
    secondary = LinkLiftTextPrimary,
    onSecondary = LinkLiftBackground,
    secondaryContainer = Color(0xFF272727),
    onSecondaryContainer = LinkLiftTextPrimary,
    tertiary = LinkLiftBlue,
    background = LinkLiftBackground,
    onBackground = LinkLiftTextPrimary,
    surface = LinkLiftSurface,
    onSurface = LinkLiftTextPrimary,
    surfaceVariant = LinkLiftSurfaceAlt,
    onSurfaceVariant = LinkLiftTextSecondary,
    outline = LinkLiftOutline,
    error = LinkLiftError,
    onError = LinkLiftTextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = LinkLiftLightTextPrimary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = LinkLiftLightTextPrimary,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = Color(0xFFE5E5E5),
    onSecondaryContainer = LinkLiftLightTextPrimary,
    tertiary = LinkLiftBlue,
    background = LinkLiftLightBackground,
    onBackground = LinkLiftLightTextPrimary,
    surface = LinkLiftLightSurface,
    onSurface = LinkLiftLightTextPrimary,
    surfaceVariant = LinkLiftLightSurfaceAlt,
    onSurfaceVariant = LinkLiftLightTextSecondary,
    outline = LinkLiftLightOutline,
    error = LinkLiftError,
    onError = androidx.compose.ui.graphics.Color.White
)

@Composable
fun LinkLiftTheme(
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