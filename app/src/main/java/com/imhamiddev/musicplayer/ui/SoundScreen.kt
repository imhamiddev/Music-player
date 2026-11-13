package com.imhamiddev.musicplayer.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imhamiddev.musicplayer.playback.PlaybackFx
import kotlinx.coroutines.delay
import kotlin.math.abs

private class Preset(val name: (Strings) -> String, val points: FloatArray)

private val presets = listOf(
    Preset({ it.presetFlat }, floatArrayOf(0f, 0f, 0f, 0f, 0f)),
    Preset({ it.presetBass }, floatArrayOf(0.7f, 0.45f, 0f, 0f, 0f)),
    Preset({ it.presetTreble }, floatArrayOf(0f, 0f, 0f, 0.4f, 0.7f)),
    Preset({ it.presetVocal }, floatArrayOf(-0.2f, 0f, 0.45f, 0.35f, -0.1f)),
    Preset({ it.presetRock }, floatArrayOf(0.5f, 0.2f, -0.2f, 0.25f, 0.5f)),
    Preset({ it.presetChill }, floatArrayOf(0.25f, 0.1f, 0f, -0.15f, -0.3f)),
)

/** Spreads a 5-point curve over however many bands the device has. */
private fun curve(points: FloatArray, n: Int): List<Float> = List(n) { i ->
    if (n == 1) points[0] else {
        val pos = i * (points.size - 1) / (n - 1).toFloat()
        val lo = pos.toInt().coerceAtMost(points.size - 2)
        val t = pos - lo
        points[lo] * (1 - t) + points[lo + 1] * t
    }
}

private val speeds = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)
private val sleepChoices = listOf(15, 30, 45, 60)
private val fadeChoices = listOf(2, 4, 6)

/** Full-screen page: sleep timer, playback speed, equalizer, bass boost and smooth song changes. */
@Composable
fun SoundScreen(onClose: () -> Unit) {
    val s = LocalStrings.current
    Column(
        Modifier.fillMaxSize().background(Ui.Bg).statusBarsPadding().navigationBarsPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconCircle(Icons.Rounded.ChevronLeft, onClose, modifier = Modifier.mirrorInRtl(), size = 38.dp, iconSize = 20.dp, description = s.close)
            Spacer(Modifier.width(12.dp))
            Text(s.soundTitle, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ui.Ink, letterSpacing = s.ls((-0.6).sp))
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SleepCard()
            SpeedCard()
            EqualizerCard()
            BassCard()
            CrossfadeCard()
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Card(title: String, hint: String?, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Ui.Card)
            .padding(18.dp)
            .animateContentSize(Motion.soft()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ui.Ink)
                if (hint != null) Text(hint, fontSize = 12.5.sp, color = Ui.Muted, lineHeight = 17.sp)
            }
            trailing?.invoke()
        }
        content()
    }
}

@Composable
fun SoftSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val bg by animateColorAsState(if (checked) Ui.GreenA else Ui.TileDeep, tween(260, easing = Motion.Silk), label = "swbg")
    val x by animateDpAsState(if (checked) 24.dp else 2.dp, Motion.soft(), label = "swx")
    Box(
        Modifier.width(52.dp).height(30.dp).bouncyClick(0.95f) { onChange(!checked) }.clip(RoundedCornerShape(50)).background(bg),
    ) {
        Box(Modifier.offset(x = x, y = 2.dp).size(26.dp).clip(CircleShape).background(Color.White))
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Ui.GreenTint else Ui.Tile, tween(260, easing = Motion.Silk), label = "chipbg")
    val fg by animateColorAsState(if (selected) Ui.Ink else Ui.Muted, tween(260, easing = Motion.Silk), label = "chipfg")
    Box(
        modifier.bouncyClick(0.94f, onClick = onClick).clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = fg, maxLines = 1) }
}

@Composable
private fun SleepCard() {
    val s = LocalStrings.current
    val endsAt by PlaybackFx.sleepEndsAt.collectAsState()
    val endOfSong by PlaybackFx.sleepEndOfSong.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(endsAt) {
        while (endsAt > 0L) {
            now = System.currentTimeMillis()
            delay(500)
        }
    }
    val active = endsAt > 0L || endOfSong
    Card(s.sleepTitle, s.sleepHint) {
        AnimatedVisibility(visible = active, enter = fadeIn(Motion.soft()) + expandVertically(Motion.soft()), exit = fadeOut(Motion.soft()) + shrinkVertically(Motion.soft())) {
            val left = (endsAt - now).coerceAtLeast(0L) / 1000L
            val clock = String.format(java.util.Locale.US, "%d:%02d", left / 60, left % 60)
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Ui.GreenTint).padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(
                    if (endOfSong) s.sleepStopsAtEnd else s.sleepStopsIn(clock),
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ui.Ink,
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            sleepChoices.forEach { m ->
                Chip(s.sleepMinutes(m), false, Modifier.weight(1f)) { PlaybackFx.startSleep(m) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(s.sleepEndOfSong, endOfSong, Modifier.weight(1f)) { PlaybackFx.startSleepAtEndOfSong() }
            Chip(s.sleepOff, false, Modifier.weight(1f).alpha(if (active) 1f else 0.5f)) { PlaybackFx.cancelSleep() }
        }
    }
}

@Composable
private fun SpeedCard() {
    val s = LocalStrings.current
    val speed by PlaybackFx.speed.collectAsState()
    Card(s.speedTitle, s.speedHint) {
        SegmentedTabs(
            labels = speeds.map { s.speedLabel(it) },
            selected = speeds.indexOfFirst { abs(it - speed) < 0.01f }.coerceAtLeast(1),
            onSelect = { PlaybackFx.setSpeed(speeds[it]) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = Ui.GreenA,
    activeTrackColor = Ui.GreenA,
    inactiveTrackColor = Ui.TileDeep,
    disabledThumbColor = Ui.Muted,
    disabledActiveTrackColor = Ui.Muted.copy(alpha = 0.5f),
    disabledInactiveTrackColor = Ui.Tile,
)

@Composable
private fun EqualizerCard() {
    val s = LocalStrings.current
    val info by PlaybackFx.eqInfo.collectAsState()
    val enabled by PlaybackFx.eqEnabled.collectAsState()
    val stored by PlaybackFx.eqLevels.collectAsState()
    val eq = info
    Card(s.eqTitle, if (eq == null) s.eqUnsupported else null, trailing = {
        if (eq != null) SoftSwitch(enabled) { PlaybackFx.setEqEnabled(it) }
    }) {
        if (eq != null) {
            val n = eq.freqsHz.size
            val levels = List(n) { stored.getOrElse(it) { 0f } }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { p ->
                    val target = curve(p.points, n)
                    val selected = enabled && levels.indices.all { abs(levels[it] - target[it]) < 0.03f }
                    Chip(p.name(s), selected) {
                        PlaybackFx.setEqLevels(target)
                        PlaybackFx.setEqEnabled(true)
                    }
                }
            }
            Column(Modifier.alpha(if (enabled) 1f else 0.45f)) {
                eq.freqsHz.forEachIndexed { i, hz ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.freqLabel(hz), fontSize = 12.sp, color = Ui.Muted, modifier = Modifier.width(72.dp))
                        Slider(
                            value = levels[i],
                            onValueChange = { v -> PlaybackFx.setEqLevels(levels.toMutableList().also { it[i] = v }) },
                            valueRange = -1f..1f,
                            enabled = enabled,
                            colors = sliderColors(),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BassCard() {
    val s = LocalStrings.current
    val supported by PlaybackFx.bassSupported.collectAsState()
    val bass by PlaybackFx.bass.collectAsState()
    Card(s.bassTitle, if (supported) null else s.bassUnsupported) {
        if (supported) {
            Slider(
                value = bass,
                onValueChange = { PlaybackFx.setBass(it) },
                valueRange = 0f..1f,
                colors = sliderColors(),
            )
        }
    }
}

@Composable
private fun CrossfadeCard() {
    val s = LocalStrings.current
    val on by PlaybackFx.crossfadeOn.collectAsState()
    val sec by PlaybackFx.crossfadeSec.collectAsState()
    Card(s.crossfadeTitle, s.crossfadeHint, trailing = { SoftSwitch(on) { PlaybackFx.setCrossfadeOn(it) } }) {
        AnimatedVisibility(visible = on, enter = fadeIn(Motion.soft()) + expandVertically(Motion.soft()), exit = fadeOut(Motion.soft()) + shrinkVertically(Motion.soft())) {
            SegmentedTabs(
                labels = fadeChoices.map { s.crossfadeSecs(it) },
                selected = fadeChoices.indexOf(sec).coerceAtLeast(0),
                onSelect = { PlaybackFx.setCrossfadeSec(fadeChoices[it]) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
