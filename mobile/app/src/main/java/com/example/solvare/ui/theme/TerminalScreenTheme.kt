package com.example.solvare.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val TerminalGreen = Color(0xFF14F195)
val TerminalAmber = Color(0xFFFFB800)

private val TerminalDarkBg = Color(0xFF0D1117)
private val TerminalDarkSurface = Color(0xFF161B22)
private val TerminalDarkCard = Color(0xFF21262D)
private val TerminalTextPrimary = Color(0xFFE6EDF3)
private val TerminalTextSecondary = Color(0xFF8B949E)

private val TerminalColorScheme = darkColorScheme(
    primary = TerminalGreen,
    secondary = TerminalAmber,
    background = TerminalDarkBg,
    surface = TerminalDarkSurface,
    surfaceVariant = TerminalDarkCard,
    onPrimary = TerminalDarkBg,
    onBackground = TerminalTextPrimary,
    onSurface = TerminalTextPrimary,
    onSurfaceVariant = TerminalTextSecondary
)

private val TerminalTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp
    )
)

@Composable
fun TerminalScreenTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TerminalColorScheme,
        typography = TerminalTypography,
        content = content
    )
}
