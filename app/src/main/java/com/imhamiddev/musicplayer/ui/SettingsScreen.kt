package com.imhamiddev.musicplayer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imhamiddev.musicplayer.data.AppLanguage
import com.imhamiddev.musicplayer.data.ProgressStyle
import com.imhamiddev.musicplayer.data.ThemeMode

/** Full-screen settings page: language, how the song progress looks, and the theme. */
@Composable
fun SettingsScreen(
    language: AppLanguage,
    progressStyle: ProgressStyle,
    themeMode: ThemeMode,
    onLanguage: (AppLanguage) -> Unit,
    onProgressStyle: (ProgressStyle) -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onOpenSound: () -> Unit,
    onClose: () -> Unit,
) {
    val s = LocalStrings.current
    Column(
        Modifier
            .fillMaxSize()
            .background(Ui.Bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconCircle(Icons.Rounded.ChevronLeft, onClose, modifier = Modifier.mirrorInRtl(), size = 48.dp, description = s.close)
            Spacer(Modifier.width(14.dp))
            Text(s.settings, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ui.Ink, letterSpacing = s.ls((-0.8).sp))
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .bouncyClick(0.97f, onClick = onOpenSound)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Ui.GreenTint)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(Ui.GreenBrush), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Tune, null, tint = Ui.OnAccent, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(s.soundTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ui.Ink)
                    Text(s.soundEntryHint, fontSize = 12.5.sp, color = Ui.Muted, lineHeight = 17.sp)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = Ui.Muted, modifier = Modifier.size(22.dp).mirrorInRtl())
            }

            Section(s.settingsLanguage, s.settingsLanguageHint) {
                // Each language is written in its own script, whatever language the app is in.
                SegmentedTabs(
                    labels = listOf("English", "فارسی"),
                    selected = if (language == AppLanguage.Persian) 1 else 0,
                    onSelect = { onLanguage(if (it == 1) AppLanguage.Persian else AppLanguage.English) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Section(s.settingsProgress, s.settingsProgressHint) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StyleCard(
                        label = s.progressWaveform,
                        selected = progressStyle == ProgressStyle.Waveform,
                        onClick = { onProgressStyle(ProgressStyle.Waveform) },
                        modifier = Modifier.weight(1f),
                    ) { WaveformPreview() }
                    StyleCard(
                        label = s.progressLine,
                        selected = progressStyle == ProgressStyle.Line,
                        onClick = { onProgressStyle(ProgressStyle.Line) },
                        modifier = Modifier.weight(1f),
                    ) { LinePreview() }
                }
            }

            Section(s.settingsTheme, null) {
                SegmentedTabs(
                    labels = listOf(s.themeSystem, s.themeLight, s.themeDark),
                    selected = when (themeMode) {
                        ThemeMode.System -> 0
                        ThemeMode.Light -> 1
                        ThemeMode.Dark -> 2
                    },
                    onSelect = { onTheme(when (it) { 0 -> ThemeMode.System; 1 -> ThemeMode.Light; else -> ThemeMode.Dark }) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Section(title: String, hint: String?, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ui.Ink)
            if (hint != null) Text(hint, fontSize = 13.sp, color = Ui.Muted, lineHeight = 18.sp)
        }
        content()
    }
}

@Composable
private fun StyleCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    preview: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .bouncyClick(0.97f, onClick = onClick)
            .clip(shape)
            .background(if (selected) Ui.GreenTint else Ui.Tile)
            .border(2.dp, if (selected) Ui.GreenA else Color.Transparent, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // The preview always sits on the dark deck colour, just like in the player.
        Column(
            Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(16.dp)).background(Ui.Deck).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.Center,
        ) { preview() }
        Text(
            label, fontSize = 14.sp, color = Ui.Ink,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

private val previewHeights = floatArrayOf(.35f, .6f, .9f, .5f, .75f, .4f, 1f, .55f, .3f, .7f, .85f, .45f, .6f, .35f, .8f, .5f, .25f, .65f)

@Composable
private fun WaveformPreview() {
    Canvas(Modifier.fillMaxWidth().height(40.dp)) {
        val n = previewHeights.size
        val gap = size.width / n
        val bar = 3.dp.toPx()
        for (i in 0 until n) {
            val x = gap * (i + 0.5f)
            val h = size.height * previewHeights[i]
            val color = if (i < n * 0.4f) Ui.GreenA else Color.White.copy(alpha = 0.22f)
            drawLine(color, Offset(x, (size.height - h) / 2f), Offset(x, (size.height + h) / 2f), bar, StrokeCap.Round)
        }
    }
}

@Composable
private fun LinePreview() {
    Canvas(Modifier.fillMaxWidth().height(40.dp)) {
        val y = size.height / 2f
        val pad = 8.dp.toPx()
        val end = pad + (size.width - 2 * pad) * 0.4f
        val h = 5.dp.toPx()
        drawLine(Color.White.copy(alpha = 0.22f), Offset(pad, y), Offset(size.width - pad, y), h, StrokeCap.Round)
        drawLine(Ui.GreenA, Offset(pad, y), Offset(end, y), h, StrokeCap.Round)
        drawCircle(Color.White, 7.dp.toPx(), Offset(end, y))
    }
}
