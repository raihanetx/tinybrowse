package com.tinybrowse.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    background = TinyBrowseColors.Background,
    surface = TinyBrowseColors.Surface,
    surfaceVariant = TinyBrowseColors.SurfaceVariant,
    onBackground = TinyBrowseColors.TextPrimary,
    onSurface = TinyBrowseColors.TextPrimary,
    onSurfaceVariant = TinyBrowseColors.TextSecondary,
    primary = TinyBrowseColors.Primary,
    onPrimary = Color.White,
    error = TinyBrowseColors.Error,
    onError = Color.White,
)

@Composable
fun TinyBrowseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = TinyBrowseTypography,
        content = content
    )
}
