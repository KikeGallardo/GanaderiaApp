package com.ganaderia.ganaderiaapp.ui.theme

// ============================================
// Archivo: ui/theme/Theme.kt
// ============================================

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = GanadoColors.Primary,
    onPrimary = Color.White,
    primaryContainer = GanadoColors.Primary.copy(alpha = 0.1f),
    onPrimaryContainer = GanadoColors.Primary,

    secondary = GanadoColors.Secondary,
    onSecondary = Color.White,
    secondaryContainer = GanadoColors.Secondary.copy(alpha = 0.1f),
    onSecondaryContainer = GanadoColors.Secondary,

    tertiary = GanadoColors.Tertiary,
    onTertiary = Color.White,

    background = GanadoColors.Background,
    onBackground = GanadoColors.TextPrimary,

    surface = GanadoColors.Surface,
    onSurface = GanadoColors.TextPrimary,

    error = GanadoColors.Error,
    onError = Color.White,
    errorContainer = GanadoColors.Error.copy(alpha = 0.1f),
    onErrorContainer = GanadoColors.Error,

    outline = Color(0xFFE0E0E0),
    outlineVariant = Color(0xFFF5F5F5)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun GanadoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        shapes = AppShapes,
        typography = Typography(),
        content = content
    )
}