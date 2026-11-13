package com.imhamiddev.musicplayer.ui

import android.content.ContentUris
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imhamiddev.musicplayer.data.ProgressStyle
import com.imhamiddev.musicplayer.data.Song
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

// ---- Micro-interaction primitives ----------------------------------------------------------

/** Springy press feedback + click, without ripple (matches the soft, tactile feel of the reference). */
fun Modifier.bouncyClick(
    pressedScale: Float = 0.92f,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = tween(Motion.Press, easing = Motion.Silk),
        label = "press",
    )
    this
        .graphicsLayer { scaleX = s; scaleY = s }
        .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
}

/** Flips directional icons (arrows, chevrons) when the layout runs right-to-left. */
@Composable
fun Modifier.mirrorInRtl(): Modifier =
    if (LocalLayoutDirection.current == LayoutDirection.Rtl) this.graphicsLayer { scaleX = -1f } else this

@Composable
fun IconCircle(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    container: Color = Ui.Tile,
    tint: Color = Ui.Ink,
    iconSize: Dp = 22.dp,
    description: String? = null,
) {
    Box(
        modifier
            .size(size)
            .bouncyClick(onClick = onClick)
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun PlayPauseIcon(playing: Boolean, tint: Color, size: Dp) {
    AnimatedContent(
        targetState = playing,
        transitionSpec = {
            (fadeIn(tween(260, easing = Motion.Silk)) + scaleIn(animationSpec = Motion.soft(), initialScale = 0.4f)) togetherWith
                (fadeOut(tween(260, easing = Motion.Silk)) + scaleOut(animationSpec = tween(260, easing = Motion.Silk), targetScale = 0.4f))
        },
        label = "playpause",
    ) { p ->
        Icon(
            if (p) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (p) "Pause" else "Play",
            tint = tint,
            modifier = Modifier.size(size),
        )
    }
}

@Composable
fun HeartButton(
    fav: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    idleTint: Color = Ui.Ink.copy(alpha = 0.5f),
) {
    val scale = remember { Animatable(1f) }
    var prev by remember { mutableStateOf(fav) }
    LaunchedEffect(fav) {
        if (fav && !prev) {
            scale.snapTo(0.55f)
            scale.animateTo(1f, Motion.soft())
        }
        prev = fav
    }
    val tint by animateColorAsState(if (fav) Ui.Pink else idleTint, tween(260, easing = Motion.Silk), label = "heart")
    Box(modifier.size(size).bouncyClick(0.85f, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(
            if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = "Favorite",
            tint = tint,
            modifier = Modifier
                .size(size * 0.58f)
                .graphicsLayer { scaleX = scale.value; scaleY = scale.value },
        )
    }
}

// ---- Segmented tabs (sliding white pill indicator) -----------------------------------------

@Composable
fun SegmentedTabs(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(50)
    BoxWithConstraints(
        modifier
            .height(42.dp)
            .clip(shape)
            .background(Ui.TileDeep.copy(alpha = 0.7f))
            .padding(4.dp),
    ) {
        val tabW = maxWidth / labels.size
        val x by animateDpAsState(tabW * selected, Motion.soft(), label = "tabx")
        Box(
            Modifier
                .offset(x = x)
                .width(tabW)
                .fillMaxHeight()
                .shadow(6.dp, shape, ambientColor = Color.Black.copy(0.08f), spotColor = Color.Black.copy(0.14f))
                .clip(shape)
                .background(Ui.Card),
        )
        Row(Modifier.fillMaxSize()) {
            labels.forEachIndexed { i, label ->
                val color by animateColorAsState(if (i == selected) Ui.Ink else Ui.Muted, tween(260, easing = Motion.Silk), label = "tabc")
                Box(
                    Modifier.weight(1f).fillMaxHeight().bouncyClick(0.94f) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, color = color, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
    }
}

// ---- Search pill with live result-count badge ----------------------------------------------

@Composable
fun SearchPill(query: String, onQuery: (String) -> Unit, resultCount: Int, modifier: Modifier = Modifier) {
    val focus = LocalFocusManager.current
    val s = LocalStrings.current
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .fillMaxWidth()
            .height(42.dp)
            .shadow(6.dp, shape, ambientColor = Color.Black.copy(0.04f), spotColor = Color.Black.copy(0.07f))
            .clip(shape)
            .background(Ui.Card)
            .padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = Ui.Muted, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(s.searchHint, color = Ui.Muted, fontSize = 13.5.sp, maxLines = 1)
            }
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = TextStyle(color = Ui.Ink, fontSize = 14.5.sp),
                cursorBrush = SolidColor(Ui.GreenA),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        AnimatedVisibility(
            visible = query.isNotEmpty(),
            enter = scaleIn(animationSpec = Motion.soft(), initialScale = 0.5f) + fadeIn(Motion.soft()),
            exit = scaleOut(Motion.soft(), targetScale = 0.5f) + fadeOut(Motion.soft()),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Ui.GreenBrush)
                        .padding(horizontal = 11.dp, vertical = 5.dp),
                ) {
                    AnimatedContent(
                        targetState = resultCount,
                        transitionSpec = {
                            (slideInVertically(Motion.soft()) { it } + fadeIn(Motion.soft())) togetherWith (slideOutVertically(Motion.soft()) { -it } + fadeOut(Motion.soft()))
                        },
                        label = "count",
                    ) { n ->
                        Text(
                            s.results(n),
                            color = Ui.OnAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
                IconCircle(Icons.Rounded.Close, { onQuery("") }, size = 28.dp, iconSize = 16.dp, description = "Clear")
            }
        }
    }
}

// ---- Equalizer bars (now playing indicator) ------------------------------------------------

@Composable
fun EqualizerBars(playing: Boolean, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "eq")
    val a by t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(520), RepeatMode.Reverse), label = "a")
    val b by t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(380), RepeatMode.Reverse), label = "b")
    val c by t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "c")
    val live by animateFloatAsState(if (playing) 1f else 0f, tween(390, easing = Motion.Silk), label = "live")
    Row(
        modifier.height(18.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        listOf(a, b, c).forEach { h ->
            val f = 0.25f + (h - 0.25f) * live
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight(f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Ui.GreenBrush),
            )
        }
    }
}

// ---- Album art -----------------------------------------------------------------------------

private val artCache = LruCache<String, ImageBitmap>(96)

private val palettes = listOf(
    listOf(Color(0xFFF472B6), Color(0xFFFB7185)),
    listOf(Color(0xFF34D399), Color(0xFFA3E635)),
    listOf(Color(0xFF60A5FA), Color(0xFF818CF8)),
    listOf(Color(0xFFFBBF24), Color(0xFFFB923C)),
    listOf(Color(0xFF3A3A3A), Color(0xFF6B6B6B)),
)

private fun loadArt(ctx: android.content.Context, song: Song, size: Int): ImageBitmap? = try {
    val bmp = if (Build.VERSION.SDK_INT >= 29) {
        ctx.contentResolver.loadThumbnail(song.uri, Size(size, size), null)
    } else {
        val uri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), song.albumId)
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    }
    bmp?.asImageBitmap()
} catch (_: Exception) {
    null
}

@Composable
fun AlbumArt(
    song: Song?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    sizePx: Int = 192,
) {
    val ctx = LocalContext.current
    val key = song?.let { "${it.id}_$sizePx" }
    var bmp by remember(key) { mutableStateOf(key?.let { artCache.get(it) }) }
    LaunchedEffect(key) {
        if (song != null && key != null && bmp == null) {
            val loaded = withContext(Dispatchers.IO) { loadArt(ctx, song, sizePx) }
            if (loaded != null) {
                artCache.put(key, loaded)
                bmp = loaded
            }
        }
    }
    val colors = palettes[(song?.id ?: 0L).mod(palettes.size.toLong()).toInt()]
    Box(
        modifier.clip(shape).background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(targetState = bmp, animationSpec = tween(364, easing = Motion.Silk), modifier = Modifier.fillMaxSize(), label = "art") { b ->
            if (b != null) {
                Image(bitmap = b, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.MusicNote, null, tint = Color.White.copy(alpha = 0.92f), modifier = Modifier.fillMaxSize(0.46f))
                }
            }
        }
    }
}

// ---- Waveform scrubber (inspired by the voice-note card) -----------------------------------

@Composable
fun Waveform(
    seed: Long,
    progress: Float,
    playing: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    bars: Int = 46,
    unplayed: Color = Color.White.copy(alpha = 0.2f),
    onScrub: (Float?) -> Unit = {},
) {
    val heights = remember(seed, bars) {
        val r = Random(seed)
        List(bars) { i ->
            val base = 0.3f + 0.7f * r.nextFloat()
            val env = 0.72f + 0.28f * sin(i * 0.38f + (seed % 11).toFloat())
            (base * env).coerceIn(0.16f, 1f)
        }
    }
    var dragging by remember { mutableStateOf(false) }
    var dragFrac by remember { mutableFloatStateOf(0f) }
    val smooth by animateFloatAsState(progress, tween(260, easing = LinearEasing), label = "wfp")
    val live by animateFloatAsState(if (playing) 1f else 0f, tween(390, easing = Motion.Silk), label = "wfl")
    val inf = rememberInfiniteTransition(label = "wf")
    val phase by inf.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "phase")

    Canvas(
        modifier.pointerInput(Unit) {
            // Touch-down shows the target time immediately; dragging updates it live; lifting seeks.
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                down.consume()
                dragging = true
                dragFrac = (down.position.x / size.width).coerceIn(0f, 1f)
                onScrub(dragFrac)
                var cancelled = false
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id }
                    if (change == null) {
                        cancelled = true
                        break
                    }
                    dragFrac = (change.position.x / size.width).coerceIn(0f, 1f)
                    onScrub(dragFrac)
                    change.consume()
                    if (!change.pressed) break
                }
                dragging = false
                onScrub(null)
                if (!cancelled) onSeek(dragFrac)
            }
        },
    ) {
        val n = heights.size
        val slot = size.width / n
        val stroke = slot * 0.54f
        val shown = if (dragging) dragFrac else smooth
        val brush = Brush.horizontalGradient(listOf(Ui.GreenA, Ui.GreenB), startX = 0f, endX = size.width)
        val midY = size.height / 2f
        for (i in 0 until n) {
            val x = slot * (i + 0.5f)
            val played = (i + 0.5f) / n <= shown
            var h = heights[i]
            val dist = abs(i - shown * n)
            if (live > 0f && dist < 7f) h *= 1f + 0.2f * live * sin(phase + i * 0.7f) * (1f - dist / 7f)
            val half = (size.height * 0.5f * h.coerceAtMost(1f)).coerceAtLeast(stroke / 2f)
            if (played) {
                drawLine(brush, Offset(x, midY - half), Offset(x, midY + half), stroke, StrokeCap.Round)
            } else {
                drawLine(unplayed, Offset(x, midY - half), Offset(x, midY + half), stroke, StrokeCap.Round)
            }
        }
    }
}

/** Pill shown above the waveform while the finger is down: the time under the finger and the jump. */
@Composable
fun ScrubBubble(scrub: Float?, posMs: Long, durationMs: Long, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val last = remember { FloatArray(1) }
    if (scrub != null) last[0] = scrub
    val a by animateFloatAsState(if (scrub != null) 1f else 0f, tween(260, easing = Motion.Silk), label = "bubble")
    val target = (last[0] * durationMs).toLong()
    val delta = target - posMs
    val sign = if (delta >= 0) "+" else "-"
    Box(
        modifier.fillMaxWidth().height(38.dp).graphicsLayer { alpha = a },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.clip(RoundedCornerShape(50)).background(Ui.GreenBrush).padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(s.time(target), color = Ui.OnAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text("$sign${s.time(abs(delta))}", color = Ui.OnAccent.copy(alpha = 0.65f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ---- Simple line progress (alternative to the waveform, chosen in Settings) -----------------

/**
 * A plain progress line with a round thumb. Same gestures as [Waveform]: touching shows the target
 * time at once, dragging moves it, lifting seeks.
 */
@Composable
fun LineProgress(
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    unplayed: Color = Color.White.copy(alpha = 0.2f),
    onScrub: (Float?) -> Unit = {},
) {
    var dragging by remember { mutableStateOf(false) }
    var dragFrac by remember { mutableFloatStateOf(0f) }
    val smooth by animateFloatAsState(progress, tween(260, easing = LinearEasing), label = "lnp")
    val grow by animateFloatAsState(if (dragging) 1f else 0f, tween(200, easing = Motion.Silk), label = "lng")
    val seek by rememberUpdatedState(onSeek)
    val scrub by rememberUpdatedState(onScrub)

    Canvas(
        modifier.pointerInput(Unit) {
            // The line is inset by the thumb radius so the thumb never gets cut off at either end.
            val pad = LineThumbRoom.toPx()
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                down.consume()
                val span = (size.width - 2f * pad).coerceAtLeast(1f)
                dragging = true
                dragFrac = ((down.position.x - pad) / span).coerceIn(0f, 1f)
                scrub(dragFrac)
                var cancelled = false
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id }
                    if (change == null) {
                        cancelled = true
                        break
                    }
                    dragFrac = ((change.position.x - pad) / span).coerceIn(0f, 1f)
                    scrub(dragFrac)
                    change.consume()
                    if (!change.pressed) break
                }
                dragging = false
                scrub(null)
                if (!cancelled) seek(dragFrac)
            }
        },
    ) {
        val pad = LineThumbRoom.toPx()
        val span = (size.width - 2f * pad).coerceAtLeast(1f)
        val shown = if (dragging) dragFrac else smooth
        val midY = size.height / 2f
        val trackH = 6.dp.toPx()
        val endX = pad + span * shown
        drawLine(unplayed, Offset(pad, midY), Offset(pad + span, midY), trackH, StrokeCap.Round)
        if (shown > 0.002f) {
            val brush = Brush.horizontalGradient(listOf(Ui.GreenA, Ui.GreenB), startX = pad, endX = pad + span)
            drawLine(brush, Offset(pad, midY), Offset(endX, midY), trackH, StrokeCap.Round)
        }
        val r = (6.dp + 3.dp * grow).toPx()
        if (grow > 0.01f) drawCircle(Ui.GreenA.copy(alpha = 0.28f * grow), r * 2.1f, Offset(endX, midY))
        drawCircle(Color.White, r, Offset(endX, midY))
    }
}

private val LineThumbRoom = 10.dp

/** The song progress in the style picked in Settings. */
@Composable
fun SeekBar(
    style: ProgressStyle,
    seed: Long,
    progress: Float,
    playing: Boolean,
    onSeek: (Float) -> Unit,
    onScrub: (Float?) -> Unit,
    waveHeight: Dp,
    modifier: Modifier = Modifier,
) {
    when (style) {
        ProgressStyle.Waveform -> Waveform(
            seed = seed,
            progress = progress,
            playing = playing,
            onSeek = onSeek,
            onScrub = onScrub,
            modifier = modifier.height(waveHeight),
        )
        ProgressStyle.Line -> LineProgress(
            progress = progress,
            onSeek = onSeek,
            onScrub = onScrub,
            modifier = modifier.height(44.dp),
        )
    }
}
