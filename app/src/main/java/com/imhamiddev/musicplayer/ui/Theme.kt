package com.imhamiddev.musicplayer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Theme-aware palette. Reads a snapshot state, so every composable reading it follows the theme. */
object Ui {
    var dark by mutableStateOf(false)

    val Bg: Color get() = if (dark) Color(0xFF0E0E10) else Color(0xFFF7F7F4)
    val Card: Color get() = if (dark) Color(0xFF1B1B1F) else Color.White
    val Tile: Color get() = if (dark) Color(0xFF26262B) else Color(0xFFEFEFEA)
    val TileDeep: Color get() = if (dark) Color(0xFF303036) else Color(0xFFE6E6E0)
    val Ink: Color get() = if (dark) Color(0xFFF2F2EE) else Color(0xFF141414)
    val Muted: Color get() = if (dark) Color(0xFF9A9AA0) else Color(0xFF8E8E88)
    val GreenTint: Color get() = if (dark) Color(0xFF1E2E1D) else Color(0xFFE8F7DF)
    /** Background of the dark "deck" and mini player. */
    val Deck: Color get() = if (dark) Color(0xFF232328) else Color(0xFF141414)

    /** Dark ink for content placed on bright surfaces (green gradient, white buttons). */
    val OnAccent = Color(0xFF141414)
    val GreenA = Color(0xFF22C55E)
    val GreenB = Color(0xFFA3E635)
    val Pink = Color(0xFFFF4F81)
    val Danger = Color(0xFFEF4444)
    val GreenBrush = Brush.linearGradient(listOf(GreenA, GreenB))
}

@Composable
fun MusicTheme(dark: Boolean, content: @Composable () -> Unit) {
    // Written before any child reads it, so the whole tree composes with the right palette.
    Ui.dark = dark
    MaterialTheme(
        colorScheme = if (dark) {
            darkColorScheme(
                primary = Ui.Ink,
                onPrimary = Ui.Bg,
                background = Ui.Bg,
                onBackground = Ui.Ink,
                surface = Ui.Card,
                onSurface = Ui.Ink,
            )
        } else {
            lightColorScheme(
                primary = Ui.Ink,
                onPrimary = Color.White,
                background = Ui.Bg,
                onBackground = Ui.Ink,
                surface = Color.White,
                onSurface = Ui.Ink,
            )
        },
        content = content,
    )
}
