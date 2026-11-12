package com.imhamiddev.musicplayer.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.imhamiddev.musicplayer.data.Song
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    song: Song,
    isPlaying: Boolean,
    isFav: Boolean,
    shuffle: Boolean,
    repeatMode: Int,
    positionFlow: StateFlow<Long>,
    durationFlow: StateFlow<Long>,
    onClose: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeek: (Float) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onFav: () -> Unit,
) {
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var heightPx by remember { mutableFloatStateOf(1f) }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { heightPx = it.height.toFloat() }
            .offset { IntOffset(0, offsetY.value.roundToInt()) }
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { d ->
                    scope.launch { offsetY.snapTo((offsetY.value + d).coerceAtLeast(0f)) }
                },
                onDragStopped = { v ->
                    if (offsetY.value > heightPx * 0.2f || v > 1800f) {
                        offsetY.animateTo(heightPx, tween(312, easing = Motion.Silk))
                        onClose()
                    } else {
                        offsetY.animateTo(0f, Motion.soft())
                    }
                },
            )
            .background(Ui.Bg)
            .drawBehind {
                val c = Offset(size.width * 0.5f, size.height * 0.2f)
                val r = size.width * 0.95f
                drawCircle(
                    brush = Brush.radialGradient(listOf(Ui.GreenA.copy(alpha = 0.17f), Color.Transparent), center = c, radius = r),
                    radius = r,
                    center = c,
                )
            },
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top bar
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconCircle(Icons.Rounded.KeyboardArrowDown, onClose, size = 44.dp, description = "Close")
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("NOW PLAYING", fontSize = 11.sp, color = Ui.Muted, fontWeight = FontWeight.SemiBold, letterSpacing = 1.6.sp)
                    Crossfade(song.album, label = "album") { a ->
                        Text(a, fontSize = 13.sp, color = Ui.Ink, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.width(44.dp))
            }

            // Artwork
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val side = minOf(maxWidth - 56.dp, maxHeight - 8.dp).coerceAtLeast(120.dp)
                val artScale by animateFloatAsState(
                    if (isPlaying) 1f else 0.9f, Motion.soft(), label = "artscale",
                )
                AnimatedContent(
                    targetState = song,
                    contentKey = { it.id },
                    transitionSpec = {
                        (fadeIn(tween(450, easing = Motion.Silk)) + scaleIn(tween(450, easing = Motion.Silk), initialScale = 0.9f)) togetherWith fadeOut(tween(260, easing = Motion.Silk))
                    },
                    modifier = Modifier
                        .size(side)
                        .graphicsLayer { scaleX = artScale; scaleY = artScale }
                        .shadow(28.dp, RoundedCornerShape(40.dp), ambientColor = Color.Black.copy(0.12f), spotColor = Color.Black.copy(0.3f)),
                    label = "art",
                ) { s ->
                    AlbumArt(s, Modifier.fillMaxSize(), RoundedCornerShape(40.dp), 720)
                }
            }

            // Title row
            Row(
                Modifier.fillMaxWidth().padding(start = 30.dp, end = 22.dp, top = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Crossfade(song, modifier = Modifier.weight(1f), label = "title") { s ->
                    Column {
                        Text(s.title, fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Ui.Ink, letterSpacing = (-0.6).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(2.dp))
                        Text(s.artist, fontSize = 16.sp, color = Ui.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                HeartButton(isFav, onFav, size = 48.dp)
            }

            // Dark deck (waveform + controls)
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(36.dp))
                    .background(Ui.Deck)
                    .padding(horizontal = 22.dp, vertical = 22.dp),
            ) {
                ProgressSection(song, isPlaying, positionFlow, durationFlow, onSeek)
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DeckToggle(Icons.Rounded.Shuffle, shuffle, onShuffle)
                    DeckSkip(Icons.Rounded.SkipPrevious, onPrev)
                    Box(
                        Modifier
                            .size(76.dp)
                            .bouncyClick(0.9f, onClick = onToggle)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        PlayPauseIcon(isPlaying, Ui.OnAccent, 38.dp)
                    }
                    DeckSkip(Icons.Rounded.SkipNext, onNext)
                    DeckToggle(
                        if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        repeatMode != Player.REPEAT_MODE_OFF,
                        onRepeat,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressSection(
    song: Song,
    playing: Boolean,
    positionFlow: StateFlow<Long>,
    durationFlow: StateFlow<Long>,
    onSeek: (Float) -> Unit,
) {
    val pos by positionFlow.collectAsState()
    val liveDur by durationFlow.collectAsState()
    val dur = if (liveDur > 0L) liveDur else song.durationMs
    var scrub by remember { mutableStateOf<Float?>(null) }
    val frac = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
    val shownMs = scrub?.let { (it * dur).toLong() } ?: pos
    Column {
        ScrubBubble(scrub, pos, dur)
        Waveform(
            seed = song.id,
            progress = frac,
            playing = playing,
            onSeek = onSeek,
            onScrub = { scrub = it },
            modifier = Modifier.fillMaxWidth().height(68.dp),
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(fmt(shownMs), fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Text(fmt(dur), fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun DeckSkip(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(56.dp).bouncyClick(0.85f, onClick = onClick).clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(34.dp))
    }
}

@Composable
private fun DeckToggle(icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    val tint by animateColorAsState(if (active) Ui.GreenB else Color.White.copy(alpha = 0.5f), tween(260, easing = Motion.Silk), label = "dtint")
    val bg by animateColorAsState(if (active) Color.White.copy(alpha = 0.12f) else Color.Transparent, tween(260, easing = Motion.Silk), label = "dbg")
    Box(
        Modifier.size(48.dp).bouncyClick(0.85f, onClick = onClick).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
    }
}
