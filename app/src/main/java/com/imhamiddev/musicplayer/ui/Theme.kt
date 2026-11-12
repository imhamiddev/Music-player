package com.imhamiddev.musicplayer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object Ui {
    val Bg = Color(0xFFF7F7F4)
    val Tile = Color(0xFFEFEFEA)
    val TileDeep = Color(0xFFE6E6E0)
    val Ink = Color(0xFF141414)
    val Muted = Color(0xFF8E8E88)
    val GreenA = Color(0xFF22C55E)
    val GreenB = Color(0xFFA3E635)
    val GreenTint = Color(0xFFE8F7DF)
    val Pink = Color(0xFFFF4F81)
    val GreenBrush = Brush.linearGradient(listOf(GreenA, GreenB))
}

@Composable
fun MusicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Ui.Ink,
            onPrimary = Color.White,
            background = Ui.Bg,
            onBackground = Ui.Ink,
            surface = Color.White,
            onSurface = Ui.Ink,
        ),
        content = content,
    )
}
