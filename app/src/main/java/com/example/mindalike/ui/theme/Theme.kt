package com.example.mindalike.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light Theme Palette
private val LightColorScheme = lightColorScheme(
    primary = PrimaryBrown,
    onPrimary = Color.White,
    primaryContainer = BrandCream,
    onPrimaryContainer = DarkText,
    secondary = AccentTerracotta,
    onSecondary = Color.White,
    background = SoftCreamBackground,
    onBackground = DarkText,
    surface = Color.White,
    onSurface = DarkText
)

// Dark Theme Palette
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD4A373), // Warmer light caramel for dark mode contrast
    onPrimary = Color(0xFF2B1E16),
    primaryContainer = Color(0xFF4A2810),
    onPrimaryContainer = BrandCream,
    secondary = AccentTerracotta,
    onSecondary = Color.White,
    background = Color(0xFF1A1410),
    onBackground = Color(0xFFF5EBE6),
    surface = Color(0xFF241C16),
    onSurface = Color(0xFFF5EBE6)
)

@Composable
fun MindAlikeTheme(
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