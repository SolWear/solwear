package com.solwear.mobile.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    secondary = SolanaGreen,
    tertiary = SolWearRed,
    background = SolWearBg,
    surface = SolWearSurface,
    surfaceVariant = SolWearCard,
    outline = SolWearBorder,
    onPrimary = Color(0xFF000000),
    onSecondary = SolWearBg,
    onTertiary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = SolWearTextDim,
    error = SolWearRed
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF000000),
    secondary = SolanaGreen,
    tertiary = SolWearRed,
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF2F2F2),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFF000000),
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF57606A),
    error = ErrorRed
)

@Composable
fun SolWearTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
