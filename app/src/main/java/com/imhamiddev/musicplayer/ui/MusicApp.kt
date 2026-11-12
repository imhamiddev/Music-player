package com.imhamiddev.musicplayer.ui

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.imhamiddev.musicplayer.data.Prefs
import com.imhamiddev.musicplayer.data.Song
import com.imhamiddev.musicplayer.data.ThemeMode
import com.imhamiddev.musicplayer.data.UpdateState
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
    val themeMode by vm.themeMode.collectAsState()
    val updateState by vm.update.collectAsState()

    var renameTarget by remember { mutableStateOf<Song?>(null) }
    var detailsTarget by remember { mutableStateOf<Song?>(null) }
    var deleteTarget by remember { mutableStateOf<Song?>(null) }
    var pendingDelete by remember { mutableStateOf<Song?>(null) }
    var askFullScreen by remember { mutableStateOf(false) }

    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { res ->
        val song = pendingDelete
        pendingDelete = null
        if (song != null && res.resultCode == Activity.RESULT_OK) {
            if (Build.VERSION.SDK_INT < 30) {
                runCatching { ctx.contentResolver.delete(song.uri, null, null) }
            }
            vm.onSongsDeleted(setOf(song.id))
        }
    }
    fun startDelete(song: Song) {
        pendingDelete = song
        deleteSong(
            ctx, song,
            launch = { sender -> deleteLauncher.launch(IntentSenderRequest.Builder(sender).build()) },
            onDone = { pendingDelete = null; vm.onSongsDeleted(setOf(song.id)) },
        )
    }

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
        vm.checkForUpdate()
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

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 34) {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            val prefs = Prefs(ctx)
            if (nm != null && !nm.canUseFullScreenIntent() && !prefs.fsiAsked()) askFullScreen = true
        }
    }

    val actions = remember(vm) {
        SongActions(
            onToggleFav = { vm.toggleFavorite(it.id) },
            onPlayNext = { vm.playNext(it) },
            onRename = { renameTarget = it },
            onShare = { shareSong(ctx, it) },
            onDetails = { detailsTarget = it },
            onDelete = { if (Build.VERSION.SDK_INT >= 30) startDelete(it) else deleteTarget = it },
        )
    }

    val current: Song? = remember(songs, currentId) { songs.firstOrNull { it.id == currentId } }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomInset by animateDpAsState(
        if (current != null) navBottom + 94.dp else navBottom + 24.dp,
        Motion.soft(),
        label = "inset",
    )

    val bannerPush by animateDpAsState(
        if (updateState != UpdateState.None) UpdateBannerHeight + 3.dp else 0.dp,
        Motion.soft(Motion.Long),
        label = "bannerPush",
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
            dark = Ui.dark,
            onToggleTheme = { vm.setThemeMode(if (Ui.dark) ThemeMode.Light else ThemeMode.Dark) },
            actions = actions,
            onPlay = vm::play,
            onOpenPlayer = { playerOpen = true },
            topInset = bannerPush,
        )

        UpdateBanner(
            state = updateState,
            onDownload = vm::downloadUpdate,
            onInstall = vm::installUpdate,
            onDismiss = vm::dismissUpdate,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        AnimatedVisibility(
            visible = current != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(Motion.soft()) { it } + fadeIn(Motion.soft()),
            exit = slideOutVertically(Motion.soft()) { it } + fadeOut(Motion.soft()),
        ) {
            current?.let {
                MiniPlayer(
                    song = it,
                    isPlaying = isPlaying,
                    positionFlow = vm.positionMs,
                    onToggle = vm::togglePlay,
                    onNext = vm::next,
                    onOpen = { playerOpen = true },
                    modifier = Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = playerOpen && current != null,
            enter = slideInVertically(Motion.soft()) { it } + fadeIn(tween(260, easing = Motion.Silk)),
            exit = slideOutVertically(tween(364, easing = Motion.Silk)) { it } + fadeOut(tween(260, easing = Motion.Silk)),
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

    val rn = rememberHeld(renameTarget)
    rn?.let { t ->
        RenameDialog(t, visible = renameTarget != null, onDismiss = { renameTarget = null }) { name ->
            vm.renameSong(t, name)
            renameTarget = null
        }
    }
    val dt = rememberHeld(detailsTarget)
    dt?.let { t -> DetailsDialog(t, visible = detailsTarget != null) { detailsTarget = null } }
    val del = rememberHeld(deleteTarget)
    del?.let { t ->
        DeleteDialog(t, visible = deleteTarget != null, onDismiss = { deleteTarget = null }) {
            deleteTarget = null
            startDelete(t)
        }
    }
    SoftAlertDialog(
        visible = askFullScreen,
        onDismissRequest = { askFullScreen = false; Prefs(ctx).setFsiAsked() },
        title = { Text("Lock screen player", color = Ui.Ink, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Allow full-screen notifications so the player can take over your lock screen while music is playing.",
                color = Ui.Muted, fontSize = 14.sp,
            )
        },
        confirmButton = {
            TextButton(onClick = {
                askFullScreen = false
                Prefs(ctx).setFsiAsked()
                runCatching {
                    ctx.startActivity(
                        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.fromParts("package", ctx.packageName, null)),
                    )
                }
            }) { Text("Allow", color = Ui.Ink, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = { askFullScreen = false; Prefs(ctx).setFsiAsked() }) { Text("Not now", color = Ui.Muted) }
        },
    )
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
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier
            .fillMaxWidth()
            .bouncyClick(0.98f, onClick = onOpen)
            .shadow(20.dp, shape, ambientColor = Color.Black.copy(0.2f), spotColor = Color.Black.copy(0.3f))
            .clip(shape)
            .background(Ui.Deck)
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 9.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AlbumArt(song, Modifier.size(42.dp), RoundedCornerShape(14.dp), 160)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(1.dp))
                Text(song.artist, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(
                Modifier.size(40.dp).bouncyClick(0.88f, onClick = onToggle).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                PlayPauseIcon(isPlaying, Ui.OnAccent, 24.dp)
            }
            Box(
                Modifier.size(40.dp).bouncyClick(0.85f, onClick = onNext).clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        MiniProgress(song, positionFlow, Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp))
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
            enter = slideInVertically(Motion.soft()) { it / 6 } + fadeIn(tween(450, easing = Motion.Silk)),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(116.dp).clip(RoundedCornerShape(38.dp)).background(Ui.GreenBrush),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.MusicNote, null, tint = Ui.OnAccent, modifier = Modifier.size(56.dp))
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
                    Text(if (denied) "Open settings" else "Allow access", color = Ui.Bg, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
