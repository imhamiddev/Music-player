package com.imhamiddev.musicplayer

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import com.imhamiddev.musicplayer.ui.Motion
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.imhamiddev.musicplayer.data.Song
import com.imhamiddev.musicplayer.ui.AlbumArt
import com.imhamiddev.musicplayer.ui.PlayPauseIcon
import com.imhamiddev.musicplayer.ui.ScrubBubble
import com.imhamiddev.musicplayer.ui.Ui
import com.imhamiddev.musicplayer.ui.Waveform
import com.imhamiddev.musicplayer.ui.bouncyClick
import com.imhamiddev.musicplayer.ui.fmt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-screen player shown over the keyguard while music is playing. It has its own, always-dark
 * UI; swiping up asks the system to dismiss the keyguard.
 */
class LockScreenActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()
    private var unlocking = false

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        getSystemService(NotificationManager::class.java)?.cancel(LOCK_NOTIFICATION_ID)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        ContextCompat.registerReceiver(
            this, unlockReceiver, IntentFilter(Intent.ACTION_USER_PRESENT), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        vm.loadSongs()
        setContent { LockScreenContent(vm, onUnlock = ::unlock) }
    }

    private fun unlock() {
        unlocking = true
        val km = getSystemService(KeyguardManager::class.java)
        if (km == null) {
            finish()
            return
        }
        km.requestDismissKeyguard(
            this,
            object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    finish()
                }

                override fun onDismissCancelled() {
                    unlocking = false
                }

                override fun onDismissError() {
                    finish()
                }
            },
        )
    }

    override fun onStop() {
        super.onStop()
        if (!unlocking && !isChangingConfigurations) finish()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(unlockReceiver) }
        super.onDestroy()
    }

    companion object {
        const val LOCK_NOTIFICATION_ID = 7001
    }
}

private val LockBg = listOf(Color(0xFF0D1F17), Color(0xFF070908), Color(0xFF040404))
private val OnDark = Color(0xFF141414)

@Composable
private fun LockScreenContent(vm: MainViewModel, onUnlock: () -> Unit) {
    val songs by vm.songs.collectAsState()
    val currentId by vm.currentId.collectAsState()
    val isPlaying by vm.isPlaying.collectAsState()
    val current: Song? = remember(songs, currentId) { songs.firstOrNull { it.id == currentId } }

    val ctx = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000L - now % 1000L)
        }
    }
    val timeText = remember(now / 60000L) { android.text.format.DateFormat.getTimeFormat(ctx).format(Date(now)) }
    val dateText = remember(now / 3600000L) { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(now)) }

    val dy = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(LockBg))
            .drawBehind {
                val c = Offset(size.width * 0.5f, size.height * 0.38f)
                val r = size.width * 1.1f
                drawCircle(
                    brush = Brush.radialGradient(listOf(Ui.GreenA.copy(alpha = 0.2f), Color.Transparent), center = c, radius = r),
                    radius = r,
                    center = c,
                )
            }
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { d -> scope.launch { dy.snapTo((dy.value + d).coerceAtMost(0f)) } },
                onDragStopped = { v ->
                    if (dy.value < -260f || v < -1600f) {
                        onUnlock()
                    }
                    dy.animateTo(0f, Motion.soft())
                },
            ),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .graphicsLayer {
                    translationY = dy.value * 0.5f
                    alpha = (1f + dy.value / 1400f).coerceIn(0.4f, 1f)
                }
                .padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(28.dp))
            Text(timeText, color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Light, letterSpacing = (-2).sp)
            Text(dateText, color = Color.White.copy(alpha = 0.65f), fontSize = 16.sp, fontWeight = FontWeight.Medium)

            if (current == null) {
                Spacer(Modifier.weight(1f))
            } else {
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val side = minOf(maxWidth, maxHeight - 8.dp).coerceIn(120.dp, 300.dp)
                    AlbumArt(
                        current,
                        Modifier
                            .size(side)
                            .shadow(30.dp, RoundedCornerShape(36.dp), ambientColor = Color.Black, spotColor = Color.Black),
                        RoundedCornerShape(36.dp),
                        720,
                    )
                }
                Text(
                    current.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    current.artist, color = Color.White.copy(alpha = 0.6f), fontSize = 15.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                LockProgress(current, isPlaying, vm)
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(60.dp).bouncyClick(0.85f, onClick = vm::previous).clip(CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(38.dp)) }
                    Box(
                        Modifier.size(76.dp).bouncyClick(0.9f, onClick = vm::togglePlay).clip(CircleShape).background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) { PlayPauseIcon(isPlaying, OnDark, 38.dp) }
                    Box(
                        Modifier.size(60.dp).bouncyClick(0.85f, onClick = vm::next).clip(CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(38.dp)) }
                }
            }

            Spacer(Modifier.height(18.dp))
            SwipeHint()
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun LockProgress(song: Song, playing: Boolean, vm: MainViewModel) {
    val pos by vm.positionMs.collectAsState()
    var scrub by remember { mutableStateOf<Float?>(null) }
    val frac = if (song.durationMs > 0) (pos.toFloat() / song.durationMs).coerceIn(0f, 1f) else 0f
    val shownMs = scrub?.let { (it * song.durationMs).toLong() } ?: pos
    Column(Modifier.fillMaxWidth()) {
        ScrubBubble(scrub, pos, song.durationMs)
        Waveform(
            seed = song.id,
            progress = frac,
            playing = playing,
            onSeek = { f -> vm.seekToFraction(f, song.durationMs) },
            onScrub = { scrub = it },
            modifier = Modifier.fillMaxWidth().height(60.dp),
        )
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(fmt(shownMs), fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Text(fmt(song.durationMs), fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SwipeHint() {
    val t = rememberInfiniteTransition(label = "hint")
    val bob by t.animateFloat(0f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "bob")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Rounded.KeyboardArrowUp, null, tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(26.dp).graphicsLayer { translationY = -6.dp.toPx() * bob },
        )
        Text("Swipe up to unlock", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
