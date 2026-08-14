package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EditorialDarkColorScheme = darkColorScheme(
    primary = EditorialPrimary,
    onPrimary = EditorialOnPrimary,
    primaryContainer = EditorialPrimaryContainer,
    onPrimaryContainer = EditorialOnPrimaryContainer,
    secondary = EditorialSecondary,
    onSecondary = EditorialOnSecondary,
    secondaryContainer = EditorialSecondaryContainer,
    onSecondaryContainer = EditorialOnSecondaryContainer,
    tertiary = EditorialTertiary,
    onTertiary = EditorialOnTertiary,
    background = EditorialBackground,
    onBackground = EditorialTextPrimary,
    surface = EditorialSurface,
    onSurface = EditorialTextPrimary,
    surfaceVariant = EditorialSurfaceVariant,
    onSurfaceVariant = EditorialTextSecondary,
    outline = EditorialOutline,
    error = EditorialError,
    onError = Color(0xFF601410)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = EditorialDarkColorScheme,
        typography = Typography,
        content = content
    )
}
