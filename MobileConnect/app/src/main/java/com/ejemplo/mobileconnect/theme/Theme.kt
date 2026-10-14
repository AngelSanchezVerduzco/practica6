package com.ejemplo.mobileconnect.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Blue = Color(0xFF1565C0)
private val Teal = Color(0xFF00897B)

private val LightColors = lightColorScheme(
    primary = Blue,
    secondary = Teal,
    background = Color(0xFFF4F6F8),
    surface = Color.White,
)

@Composable
fun MobileConnectTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
