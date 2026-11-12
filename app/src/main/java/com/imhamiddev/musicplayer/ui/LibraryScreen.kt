package com.imhamiddev.musicplayer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imhamiddev.musicplayer.data.FolderGroup
import com.imhamiddev.musicplayer.data.Song
import com.imhamiddev.musicplayer.data.SortField
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun plural(n: Int, word: String) = "$n $word" + if (n == 1) "" else "s"

private class EmptyInfo(val icon: ImageVector, val title: String, val body: String)

@Composable
fun LibraryScreen(
    songs: List<Song>,
    loaded: Boolean,
    favorites: Set<Long>,
    recents: List<Long>,
    query: String,
    sortField: SortField,
    sortAsc: Boolean,
    currentId: Long?,
    isPlaying: Boolean,
    bottomInset: Dp,
    onQuery: (String) -> Unit,
    onSort: (SortField) -> Unit,
    dark: Boolean,
    onToggleTheme: () -> Unit,
    actions: SongActions,
    onPlay: (Song, List<Song>) -> Unit,
    onOpenPlayer: () -> Unit,
    topInset: Dp = 0.dp,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var openFolder by rememberSaveable { mutableStateOf<String?>(null) }
    var sortOpen by remember { mutableStateOf(false) }

    val q = query.trim()
    val matches = remember(songs, q) {
        if (q.isEmpty()) songs else songs.filter {
            it.title.contains(q, true) || it.artist.contains(q, true) || it.album.contains(q, true)
        }
    }
    val comparator = remember(sortField, sortAsc) {
        when (sortField) {
            SortField.Alphabet -> Comparator<Song> { a, b ->
                val c = a.title.compareTo(b.title, ignoreCase = true)
                if (sortAsc) c else -c
            }
            SortField.DateAdded -> Comparator<Song> { a, b ->
                val c = a.dateAdded.compareTo(b.dateAdded)
                if (sortAsc) c else -c
            }
        }
    }
    val sorted = remember(matches, comparator) { matches.sortedWith(comparator) }
    val favs = remember(sorted, favorites) { sorted.filter { it.id in favorites } }
    val recentList = remember(matches, recents) {
        val byId = matches.associateBy { it.id }
        recents.mapNotNull { byId[it] }
    }
    val folders = remember(sorted, sortField, sortAsc) {
        val groups = sorted.groupBy { it.folderPath }.map { (p, l) -> FolderGroup(p, l.first().folderName, l) }
        when (sortField) {
            SortField.Alphabet -> groups.sortedWith { a, b ->
                val c = a.name.compareTo(b.name, ignoreCase = true)
                if (sortAsc) c else -c
            }
            SortField.DateAdded -> groups.sortedWith { a, b ->
                val c = a.latest.compareTo(b.latest)
                if (sortAsc) c else -c
            }
        }
    }
    val openGroup = folders.firstOrNull { it.path == openFolder }

    BackHandler(enabled = tab == 1 && openFolder != null) { openFolder = null }

    val subtitle = when (tab) {
        0 -> plural(sorted.size, "song")
        1 -> if (openFolder == null) plural(folders.size, "folder") else plural(openGroup?.songs?.size ?: 0, "song")
        2 -> plural(favs.size, "favorite")
        else -> plural(recentList.size, "song")
    }
    val resultCount = when (tab) {
        0 -> sorted.size
        1 -> if (openFolder == null) folders.size else openGroup?.songs?.size ?: 0
        2 -> favs.size
        else -> recentList.size
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(top = topInset)) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 20.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Library", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Ui.Ink, letterSpacing = (-1).sp)
                AnimatedContent(
                    targetState = subtitle,
                    transitionSpec = {
                        (slideInVertically(Motion.soft()) { it / 2 } + fadeIn(tween(260, easing = Motion.Silk))) togetherWith
                            (slideOutVertically(Motion.soft()) { -it / 2 } + fadeOut(tween(260, easing = Motion.Silk)))
                    },
                    label = "subtitle",
                ) { s -> Text(s, fontSize = 14.sp, color = Ui.Muted) }
            }
            IconCircle(
                if (dark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                onToggleTheme, size = 48.dp, description = "Theme",
            )
            Spacer(Modifier.width(10.dp))
            AnimatedVisibility(
                visible = tab != 3,
                enter = scaleIn(animationSpec = Motion.soft()) + fadeIn(Motion.soft()),
                exit = scaleOut(Motion.soft()) + fadeOut(Motion.soft()),
            ) {
                Box {
                    IconCircle(Icons.Rounded.Sort, { sortOpen = true }, size = 48.dp, description = "Sort")
                    SortMenu(
                        expanded = sortOpen,
                        onDismiss = { sortOpen = false },
                        field = sortField,
                        asc = sortAsc,
                        onSort = onSort,
                    )
                }
            }
        }

        SearchPill(query, onQuery, resultCount, Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp))
        SegmentedTabs(
            labels = listOf("All", "Folders", "Favorite", "Recent"),
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 6.dp),
        )

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (!loaded) {
                Skeleton()
            } else {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        (slideInHorizontally(tween(416, easing = Motion.Silk)) { dir * it / 5 } + fadeIn(tween(312, easing = Motion.Silk))) togetherWith
                            (slideOutHorizontally(tween(260, easing = Motion.Silk)) { -dir * it / 6 } + fadeOut(tween(260, easing = Motion.Silk)))
                    },
                    label = "tabs",
                ) { t ->
                    when (t) {
                        0 -> SongList(
                            sorted, favorites, currentId, isPlaying, bottomInset,
                            if (q.isNotEmpty()) noResults(q) else EmptyInfo(Icons.Rounded.LibraryMusic, "No music found", "Add some audio files to your device and they will show up here."),
                            actions, onPlay, onOpenPlayer,
                        )
                        1 -> AnimatedContent(
                            targetState = openFolder,
                            transitionSpec = {
                                if (targetState != null) {
                                    (slideInHorizontally(tween(416, easing = Motion.Silk)) { it / 3 } + fadeIn(tween(312, easing = Motion.Silk))) togetherWith
                                        (slideOutHorizontally(tween(338, easing = Motion.Silk)) { -it / 5 } + fadeOut(tween(260, easing = Motion.Silk)))
                                } else {
                                    (slideInHorizontally(tween(416, easing = Motion.Silk)) { -it / 5 } + fadeIn(tween(312, easing = Motion.Silk))) togetherWith
                                        (slideOutHorizontally(tween(338, easing = Motion.Silk)) { it / 3 } + fadeOut(tween(260, easing = Motion.Silk)))
                                }
                            },
                            label = "folders",
                        ) { path ->
                            if (path == null) {
                                FolderList(
                                    folders, bottomInset,
                                    if (q.isNotEmpty()) noResults(q) else EmptyInfo(Icons.Rounded.Folder, "No folders", "Folders that contain music will appear here."),
                                ) { openFolder = it.path }
                            } else {
                                Column(Modifier.fillMaxSize()) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        IconCircle(Icons.Rounded.ChevronLeft, { openFolder = null }, size = 42.dp, description = "Back")
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(openGroup?.name ?: "", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ui.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(openGroup?.displayPath ?: "", fontSize = 12.sp, color = Ui.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    Box(Modifier.weight(1f)) {
                                        SongList(
                                            openGroup?.songs ?: emptyList(), favorites, currentId, isPlaying, bottomInset,
                                            if (q.isNotEmpty()) noResults(q) else EmptyInfo(Icons.Rounded.Folder, "Empty folder", "There is no music in this folder."),
                                            actions, onPlay, onOpenPlayer,
                                        )
                                    }
                                }
                            }
                        }
                        2 -> SongList(
                            favs, favorites, currentId, isPlaying, bottomInset,
                            if (q.isNotEmpty()) noResults(q) else EmptyInfo(Icons.Rounded.Favorite, "No favorites yet", "Tap the heart on any song to keep it here."),
                            actions, onPlay, onOpenPlayer,
                        )
                        else -> SongList(
                            recentList, favorites, currentId, isPlaying, bottomInset,
                            if (q.isNotEmpty()) noResults(q) else EmptyInfo(Icons.Rounded.History, "Nothing played yet", "Songs you play will show up here."),
                            actions, onPlay, onOpenPlayer,
                        )
                    }
                }
            }
        }
    }
}

private fun noResults(q: String) =
    EmptyInfo(Icons.Rounded.Sort, "No results", "Nothing matches \u201C$q\u201D. Try a different title or artist.")

// ---- Sort menu -----------------------------------------------------------------------------

@Composable
private fun SortMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    field: SortField,
    asc: Boolean,
    onSort: (SortField) -> Unit,
) {
    val scope = rememberCoroutineScope()
    SoftMenu(
        expanded = expanded,
        onDismiss = onDismiss,
        shape = RoundedCornerShape(26.dp),
        gap = 8.dp,
    ) {
        Column(Modifier.width(250.dp).padding(8.dp)) {
            Text(
                "Sort by", fontSize = 12.sp, color = Ui.Muted, fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 8.dp),
            )
            SortOption(
                icon = Icons.Rounded.SortByAlpha,
                label = "Alphabetical",
                detail = if (field == SortField.Alphabet && !asc) "Z to A" else "A to Z",
                selected = field == SortField.Alphabet,
                asc = asc,
            ) {
                onSort(SortField.Alphabet)
                scope.launch { delay(300); onDismiss() }
            }
            Spacer(Modifier.height(4.dp))
            SortOption(
                icon = Icons.Rounded.AccessTime,
                label = "Date added",
                detail = if (field == SortField.DateAdded && asc) "Oldest first" else "Newest first",
                selected = field == SortField.DateAdded,
                asc = asc,
            ) {
                onSort(SortField.DateAdded)
                scope.launch { delay(300); onDismiss() }
            }
        }
    }
}

@Composable
private fun SortOption(
    icon: ImageVector,
    label: String,
    detail: String,
    selected: Boolean,
    asc: Boolean,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(if (selected) Ui.GreenTint else Color.Transparent, tween(260, easing = Motion.Silk), label = "sortbg")
    val rot by animateFloatAsState(if (asc) 0f else 180f, Motion.soft(), label = "sortrot")
    Row(
        Modifier
            .fillMaxWidth()
            .bouncyClick(0.97f, onClick = onClick)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Ui.Ink, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ui.Ink)
            Text(detail, fontSize = 12.sp, color = Ui.Muted)
        }
        AnimatedVisibility(visible = selected, enter = scaleIn(animationSpec = Motion.soft()) + fadeIn(Motion.soft()), exit = scaleOut(Motion.soft()) + fadeOut(Motion.soft())) {
            Box(Modifier.size(28.dp).clip(CircleShape).background(Ui.GreenBrush), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.ArrowUpward, null, tint = Ui.OnAccent,
                    modifier = Modifier.size(16.dp).graphicsLayer { rotationZ = rot },
                )
            }
        }
    }
}

// ---- Lists ---------------------------------------------------------------------------------

@Composable
private fun SongList(
    items: List<Song>,
    favorites: Set<Long>,
    currentId: Long?,
    isPlaying: Boolean,
    bottomInset: Dp,
    empty: EmptyInfo,
    actions: SongActions,
    onPlay: (Song, List<Song>) -> Unit,
    onOpenPlayer: () -> Unit,
) {
    if (items.isEmpty()) {
        EmptyState(empty, Modifier.padding(bottom = bottomInset))
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 30.dp, end = 30.dp, top = 8.dp, bottom = bottomInset),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(items, key = { _, s -> s.id }) { index, song ->
            val isCurrent = song.id == currentId
            SongRow(
                song = song,
                index = index,
                isCurrent = isCurrent,
                isPlaying = isPlaying,
                isFav = song.id in favorites,
                onClick = { if (isCurrent) onOpenPlayer() else onPlay(song, items) },
                actions = actions,
                modifier = Modifier.animateItem(fadeInSpec = Motion.soft(), placementSpec = Motion.soft(), fadeOutSpec = Motion.soft()),
            )
        }
    }
}

@Composable
private fun SongRow(
    song: Song,
    index: Int,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isFav: Boolean,
    onClick: () -> Unit,
    actions: SongActions,
    modifier: Modifier = Modifier,
) {
    var shown by remember { mutableStateOf(index >= 12) }
    LaunchedEffect(Unit) {
        if (!shown) {
            delay(index * 30L)
            shown = true
        }
    }
    val a by animateFloatAsState(if (shown) 1f else 0f, tween(450, easing = Motion.Silk), label = "rowa")
    val bg by animateColorAsState(if (isCurrent) Ui.GreenTint else Ui.Card, tween(390, easing = Motion.Silk), label = "rowbg")
    Row(
        modifier
            .graphicsLayer { alpha = a; translationY = (1f - a) * 26.dp.toPx() }
            .fillMaxWidth()
            .bouncyClick(0.97f, onClick = onClick)
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(song, Modifier.size(48.dp), RoundedCornerShape(14.dp), 160)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ui.Ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${song.artist}  \u00B7  ${fmt(song.durationMs)}", fontSize = 12.5.sp, color = Ui.Muted,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        AnimatedVisibility(visible = isCurrent, enter = scaleIn(Motion.soft()) + fadeIn(Motion.soft()), exit = scaleOut(Motion.soft()) + fadeOut(Motion.soft())) {
            EqualizerBars(isPlaying, Modifier.padding(horizontal = 8.dp))
        }
        SongMenuButton(song, isFav, actions)
    }
}

@Composable
private fun FolderList(
    folders: List<FolderGroup>,
    bottomInset: Dp,
    empty: EmptyInfo,
    onOpen: (FolderGroup) -> Unit,
) {
    if (folders.isEmpty()) {
        EmptyState(empty, Modifier.padding(bottom = bottomInset))
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 30.dp, end = 30.dp, top = 8.dp, bottom = bottomInset),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(folders, key = { it.path }) { g ->
            Row(
                Modifier
                    .animateItem(fadeInSpec = Motion.soft(), placementSpec = Motion.soft(), fadeOutSpec = Motion.soft())
                    .fillMaxWidth()
                    .bouncyClick(0.97f) { onOpen(g) }
                    .clip(RoundedCornerShape(22.dp))
                    .background(Ui.Card)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Ui.Tile),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Folder, null, tint = Ui.Ink, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(g.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ui.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                    Text(g.displayPath, fontSize = 12.sp, color = Ui.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(Modifier.clip(RoundedCornerShape(50)).background(Ui.Tile).padding(horizontal = 11.dp, vertical = 5.dp)) {
                    Text("${g.songs.size}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ui.Ink)
                }
                Spacer(Modifier.width(2.dp))
                Icon(Icons.Rounded.ChevronRight, null, tint = Ui.Muted, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun EmptyState(info: EmptyInfo, modifier: Modifier = Modifier) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val s by animateFloatAsState(if (shown) 1f else 0.7f, Motion.soft(), label = "emptys")
    Column(
        modifier.fillMaxSize().padding(horizontal = 48.dp).graphicsLayer { scaleX = s; scaleY = s; alpha = (s - 0.7f) / 0.3f },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(84.dp).clip(CircleShape).background(Ui.Tile), contentAlignment = Alignment.Center) {
            Icon(info.icon, null, tint = Ui.Muted, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(info.title, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ui.Ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(info.body, fontSize = 14.sp, color = Ui.Muted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Skeleton() {
    val t = rememberInfiniteTransition(label = "sk")
    val a by t.animateFloat(0.45f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "ska")
    Column(Modifier.padding(horizontal = 30.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(8) {
            Box(Modifier.fillMaxWidth().height(64.dp).alpha(a).clip(RoundedCornerShape(22.dp)).background(Ui.Tile))
        }
    }
}
