package com.moneyprinter.turbo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF90CAF9),
    secondary = Color(0xFF80CBC4),
    tertiary = Color(0xFFFFCC80)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1976D2),
    secondary = Color(0xFF00897B),
    tertiary = Color(0xFFFFA000)
)

@Composable
fun MoneyPrinterTurboTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
