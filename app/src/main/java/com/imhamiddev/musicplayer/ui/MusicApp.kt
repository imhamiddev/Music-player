package com.imhamiddev.musicplayer.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.imhamiddev.musicplayer.MainViewModel
import com.imhamiddev.musicplayer.data.Song
import kotlinx.coroutines.flow.StateFlow

private fun audioPermission(): String =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

private fun requiredPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

private fun hasAudioPermission(ctx: Context) =
    ContextCompat.checkSelfPermission(ctx, audioPermission()) == PackageManager.PERMISSION_GRANTED

@Composable
fun MusicApp(vm: MainViewModel) {
    val ctx = LocalContext.current
    val songs by vm.songs.collectAsState()
    val loaded by vm.loaded.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val recents by vm.recents.collectAsState()
    val query by vm.query.collectAsState()
    val sortField by vm.sortField.collectAsState()
    val sortAsc by vm.sortAsc.collectAsState()
    val currentId by vm.currentId.collectAsState()
    val isPlaying by vm.isPlaying.collectAsState()
    val shuffle by vm.shuffle.collectAsState()
    val repeat by vm.repeat.collectAsState()

    var granted by remember { mutableStateOf(hasAudioPermission(ctx)) }
    var denied by remember { mutableStateOf(false) }
    var playerOpen by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = hasAudioPermission(ctx)
        denied = !granted
        if (granted) vm.loadSongs()
    }
    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(requiredPermissions())
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val g = hasAudioPermission(ctx)
        granted = g
        if (g) vm.loadSongs()
    }

    if (!granted) {
        PermissionScreen(denied) {
            if (denied) {
                ctx.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null)),
                )
            } else {
                launcher.launch(requiredPermissions())
            }
        }
        return
    }

    val current: Song? = remember(songs, currentId) { songs.firstOrNull { it.id == currentId } }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomInset by animateDpAsState(
        if (current != null) navBottom + 112.dp else navBottom + 24.dp,
        spring(dampingRatio = 0.85f, stiffness = 300f),
        label = "inset",
    )

    Box(Modifier.fillMaxSize().background(Ui.Bg)) {
        LibraryScreen(
            songs = songs,
            loaded = loaded,
            favorites = favorites,
            recents = recents,
            query = query,
            sortField = sortField,
            sortAsc = sortAsc,
            currentId = currentId,
            isPlaying = isPlaying,
            bottomInset = bottomInset,
            onQuery = vm::setQuery,
            onSort = vm::setSort,
            onToggleFav = vm::toggleFavorite,
            onPlay = vm::play,
            onOpenPlayer = { playerOpen = true },
        )

        AnimatedVisibility(
            visible = current != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(spring(dampingRatio = 0.75f, stiffness = 380f)) { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            current?.let {
                MiniPlayer(
                    song = it,
                    isPlaying = isPlaying,
                    positionFlow = vm.positionMs,
                    onToggle = vm::togglePlay,
                    onNext = vm::next,
                    onOpen = { playerOpen = true },
                    modifier = Modifier.navigationBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = playerOpen && current != null,
            enter = slideInVertically(spring(dampingRatio = 0.86f, stiffness = 320f)) { it } + fadeIn(tween(150)),
            exit = slideOutVertically(tween(280, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(200)),
        ) {
            current?.let { song ->
                PlayerScreen(
                    song = song,
                    isPlaying = isPlaying,
                    isFav = song.id in favorites,
                    shuffle = shuffle,
                    repeatMode = repeat,
                    positionFlow = vm.positionMs,
                    onClose = { playerOpen = false },
                    onToggle = vm::togglePlay,
                    onNext = vm::next,
                    onPrev = vm::previous,
                    onSeek = { f -> vm.seekToFraction(f, song.durationMs) },
                    onShuffle = vm::toggleShuffle,
                    onRepeat = vm::cycleRepeat,
                    onFav = { vm.toggleFavorite(song.id) },
                )
            }
        }
        // Registered after the library's own handler so it takes priority while the player is open.
        BackHandler(enabled = playerOpen) { playerOpen = false }
    }
}

@Composable
private fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    positionFlow: StateFlow<Long>,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(30.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(76.dp)
            .bouncyClick(0.98f, onClick = onOpen)
            .shadow(22.dp, shape, ambientColor = Color.Black.copy(0.2f), spotColor = Color.Black.copy(0.3f))
            .clip(shape)
            .background(Ui.Ink),
    ) {
        Row(Modifier.fillMaxSize().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AlbumArt(song, Modifier.size(54.dp), RoundedCornerShape(18.dp), 160)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(song.artist, color = Color.White.copy(alpha = 0.6f), fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(
                Modifier.size(46.dp).bouncyClick(0.88f, onClick = onToggle).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                PlayPauseIcon(isPlaying, Ui.Ink, 26.dp)
            }
            Box(
                Modifier.size(44.dp).bouncyClick(0.85f, onClick = onNext).clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(26.dp))
            }
        }
        MiniProgress(song, positionFlow, Modifier.align(Alignment.BottomCenter).padding(horizontal = 26.dp, vertical = 5.dp))
    }
}

@Composable
private fun MiniProgress(song: Song, positionFlow: StateFlow<Long>, modifier: Modifier) {
    val pos by positionFlow.collectAsState()
    val frac = if (song.durationMs > 0) (pos.toFloat() / song.durationMs).coerceIn(0f, 1f) else 0f
    Canvas(modifier.fillMaxWidth().height(3.dp)) {
        val y = size.height / 2f
        drawLine(Color.White.copy(alpha = 0.12f), Offset(0f, y), Offset(size.width, y), size.height, StrokeCap.Round)
        if (frac > 0.002f) {
            drawLine(Ui.GreenBrush, Offset(0f, y), Offset(size.width * frac, y), size.height, StrokeCap.Round)
        }
    }
}

@Composable
private fun PermissionScreen(denied: Boolean, onClick: () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    Column(
        Modifier.fillMaxSize().background(Ui.Bg).padding(horizontal = 36.dp).navigationBarsPadding(),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedVisibility(
            visible = shown,
            enter = slideInVertically(spring(dampingRatio = 0.7f, stiffness = 200f)) { it / 6 } + fadeIn(tween(400)),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(116.dp).clip(RoundedCornerShape(38.dp)).background(Ui.GreenBrush),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.MusicNote, null, tint = Ui.Ink, modifier = Modifier.size(56.dp))
                }
                Spacer(Modifier.height(32.dp))
                Text(
                    "Your music, beautifully played", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ui.Ink,
                    textAlign = TextAlign.Center, letterSpacing = (-0.8).sp, lineHeight = 33.sp,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    if (denied) "Access was denied. Open settings and allow audio access to see your songs."
                    else "Allow access to the audio files on your device. Videos are never shown.",
                    fontSize = 15.sp, color = Ui.Muted, textAlign = TextAlign.Center, lineHeight = 21.sp,
                )
                Spacer(Modifier.height(36.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .bouncyClick(0.96f, onClick = onClick)
                        .clip(RoundedCornerShape(50))
                        .background(Ui.Ink),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (denied) "Open settings" else "Allow access", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
