package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RzDarkColorScheme = darkColorScheme(
    primary = BrandEmeraldPrimary,
    onPrimary = Color.Black,
    primaryContainer = BrandEmeraldContainer,
    onPrimaryContainer = BrandGoldLight,
    secondary = BrandGold,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF452B00),
    onSecondaryContainer = BrandGoldLight,
    tertiary = CustomerCyan,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = BrandSurfaceCardDark,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = BrandSurfaceBorderDark,
    error = ExpenseRose
)

private val RzLightColorScheme = lightColorScheme(
    primary = BrandEmeraldDarker,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = BrandGoldDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = CustomerCyan,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFFCBD5E1),
    error = ExpenseRose
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to the sleek dark theme from the poster!
    dynamicColor: Boolean = false, // Keep cohesive brand colors
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) RzDarkColorScheme else RzLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
