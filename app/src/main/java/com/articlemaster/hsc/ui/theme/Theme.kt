package com.articlemaster.hsc.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val IndigoPrimary = Color(0xFF1D4ED8)
val IndigoContainer = Color(0xFFDBEAFE)
val TealSecondary = Color(0xFF0D9488)
val TealContainer = Color(0xFFCCFBF1)
val AmberAccent = Color(0xFFD97706)
val AmberContainer = Color(0xFFFEF3C7)
val EmeraldSuccess = Color(0xFF059669)
val EmeraldContainer = Color(0xFFD1FAE5)
val RoseError = Color(0xFFDC2626)
val RoseContainer = Color(0xFFFEE2E2)

private val LightColors = lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = IndigoContainer,
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = TealSecondary,
    onSecondary = Color.White,
    secondaryContainer = TealContainer,
    onSecondaryContainer = Color(0xFF115E59),
    tertiary = AmberAccent,
    tertiaryContainer = AmberContainer,
    error = RoseError,
    errorContainer = RoseContainer,
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF2DD4BF),
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF134E4A),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = Color(0xFFFBBF24),
    tertiaryContainer = Color(0xFF78350F),
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF7F1D1D),
    background = Color(0xFF0B1120),
    surface = Color(0xFF111827),
    surfaceVariant = Color(0xFF1E293B),
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155)
)

@Composable
fun ArticleMasterTheme(
    darkThemeOverride: Boolean? = null,
    content: @Composable () -> Unit
) {
    val useDark = darkThemeOverride ?: isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (useDark) DarkColors else LightColors,
        content = content
    )
}
