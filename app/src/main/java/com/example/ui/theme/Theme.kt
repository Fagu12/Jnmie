package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class JustAnimeColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val cardBorder: Color,
    val sheetBackground: Color,
    val navPillBackground: Color,
    val isAmoled: Boolean
)

val AmoledPureDarkColors = JustAnimeColors(
    background = Color(0xFF000000), // Pure Black for OLED
    surface = Color(0xFF0C0D15),
    surfaceVariant = Color(0xFF141524),
    cardBorder = Color(0xFF222438),
    sheetBackground = Color(0xFF0A0B12),
    navPillBackground = Color(0xFF10111D),
    isAmoled = true
)

val MidnightSlateColors = JustAnimeColors(
    background = Color(0xFF10121F), // Rich Midnight Slate Dark
    surface = Color(0xFF1A1C30),
    surfaceVariant = Color(0xFF242742),
    cardBorder = Color(0xFF34385C),
    sheetBackground = Color(0xFF16182B),
    navPillBackground = Color(0xFF1E2138),
    isAmoled = false
)

val LocalAppColors = staticCompositionLocalOf { AmoledPureDarkColors }

object AppTheme {
    val colors: JustAnimeColors
        @Composable
        get() = LocalAppColors.current
}

private val JustAnimeLightColorScheme = lightColorScheme(
    primary = AnimeSecondary,
    onPrimary = Color.White,
    primaryContainer = AnimePrimaryVariant,
    onPrimaryContainer = Color.Black,
    secondary = AnimePrimary,
    onSecondary = Color.White,
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF0F111E),
    surface = Color.White,
    onSurface = Color(0xFF0F111E),
    surfaceVariant = Color(0xFFEBEFF8),
    onSurfaceVariant = Color(0xFF4A4E69),
    outline = Color(0xFFD6DBEC)
)

@Composable
fun JustAnimeTheme(
    darkTheme: Boolean = true,
    isAmoled: Boolean = true,
    content: @Composable () -> Unit
) {
    val appColors = if (isAmoled) AmoledPureDarkColors else MidnightSlateColors
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = AnimePrimary,
            onPrimary = Color.White,
            primaryContainer = AnimeSecondary,
            onPrimaryContainer = Color.White,
            secondary = AnimePrimaryVariant,
            onSecondary = Color.Black,
            secondaryContainer = appColors.surfaceVariant,
            onSecondaryContainer = TextPrimary,
            tertiary = AnimeAccent,
            background = appColors.background,
            onBackground = TextPrimary,
            surface = appColors.surface,
            onSurface = TextPrimary,
            surfaceVariant = appColors.surfaceVariant,
            onSurfaceVariant = TextSecondary,
            outline = appColors.cardBorder
        )
    } else {
        JustAnimeLightColorScheme
    }

    CompositionLocalProvider(
        LocalAppColors provides appColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

// Alias for template compatibility
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    JustAnimeTheme(darkTheme = true, isAmoled = true, content = content)
}
