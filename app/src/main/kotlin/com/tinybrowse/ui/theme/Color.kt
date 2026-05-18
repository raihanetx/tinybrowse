package com.tinybrowse.ui.theme

import androidx.compose.ui.graphics.Color

// Light theme — minimal, no dynamic colors (saves memory)
object TinyBrowseColors {
    // Backgrounds
    val Background = Color(0xFFFAFAFA)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFF0F0F0)

    // Toolbar / Chrome
    val ToolbarBackground = Color(0xFFFFFFFF)
    val ToolbarBorder = Color(0xFFE0E0E0)

    // Text
    val TextPrimary = Color(0xFF1A1A1A)
    val TextSecondary = Color(0xFF666666)
    val TextHint = Color(0xFF999999)

    // Accent
    val Primary = Color(0xFF2196F3)
    val PrimaryVariant = Color(0xFF1976D2)

    // Status
    val Secure = Color(0xFF4CAF50)      // Green lock
    val Insecure = Color(0xFFFF9800)    // Orange warning

    // Tab bar
    val TabActive = Color(0xFFFFFFFF)
    val TabInactive = Color(0xFFE8E8E8)
    val TabBorder = Color(0xFFD0D0D0)

    // Error
    val Error = Color(0xFFD32F2F)

    // Incognito
    val IncognitoBackground = Color(0xFF1A1A2E)
    val IncognitoSurface = Color(0xFF16213E)
    val IncognitoText = Color(0xFFE0E0E0)
}
