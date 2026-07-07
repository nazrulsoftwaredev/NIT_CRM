package com.nit.crm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EnterprisePrimary,
    onPrimary = Color.Black,
    secondary = EnterpriseSecondary,
    onSecondary = Color.White,
    tertiary = EnterpriseAccent,
    background = EnterpriseBlack,
    surface = EnterpriseSurface,
    surfaceVariant = EnterpriseCard,
    onBackground = EnterprisePrimary,
    onSurface = EnterprisePrimary,
    onSurfaceVariant = EnterpriseSecondary,
    outline = EnterpriseBorder,
    error = EnterpriseError
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF18181B),
    onPrimary = Color.White,
    secondary = Color(0xFF71717A),
    onSecondary = Color.White,
    tertiary = EnterpriseAccent,
    background = Color(0xFFFAFAFA),
    surface = Color.White,
    surfaceVariant = Color(0xFFF4F4F5),
    onBackground = Color(0xFF18181B),
    onSurface = Color(0xFF18181B),
    onSurfaceVariant = Color(0xFF52525B),
    outline = Color(0xFFE4E4E7),
    error = EnterpriseError
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark Theme as requested for a premium professional designer look
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
