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
import androidx.compose.foundation.gestures.animateScrollBy
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imhamiddev.musicplayer.data.Category
import com.imhamiddev.musicplayer.data.FolderGroup
import com.imhamiddev.musicplayer.data.Song
import com.imhamiddev.musicplayer.data.SortField
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

/** Songs picked with a long press; while any are picked, taps toggle instead of play. */
class SongSelection(val ids: Set<Long>, val toggle: (Long) -> Unit) {
    val active: Boolean get() = ids.isNotEmpty()
}

val LocalSelection = compositionLocalOf { SongSelection(emptySet()) {} }

/** Ids of the pinned songs, for the pin mark on a row and the Pin / Unpin menu entry. */
val LocalPinned = compositionLocalOf<Set<Long>> { emptySet() }

internal class EmptyInfo(val icon: ImageVector, val title: String, val body: String)

@Composable
fun LibraryScreen(
    songs: List<Song>,
    loaded: Boolean,
    favorites: Set<Long>,
    recents: List<Long>,
    categories: List<Category>,
    pinned: List<Long>,
    query: String,
    sortField: SortField,
    sortAsc: Boolean,
    currentId: Long?,
    isPlaying: Boolean,
    bottomInset: Dp,
    onQuery: (String) -> Unit,
    onSort: (SortField) -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    actions: SongActions,
    catActions: CategoryActions,
    onHideFolder: (String) -> Unit,
    onPlay: (Song, List<Song>) -> Unit,
    onOpenPlayer: () -> Unit,
    topInset: Dp = 0.dp,
) {
    val s = LocalStrings.current
    val pagerState = rememberPagerState(initialPage = 0) { 5 }
    val tab = pagerState.currentPage
    val tabScope = rememberCoroutineScope()
    var openFolder by rememberSaveable { mutableStateOf<String?>(null) }
    var openCategory by rememberSaveable { mutableStateOf<Long?>(null) }
    var sortOpen by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<Long>()) }

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
    val pinnedSet = remember(pinned) { pinned.toSet() }
    // Pinned songs go first in every list (in the chosen order), the rest follow.
    val sorted = remember(matches, comparator, pinnedSet) {
        val (top, rest) = matches.sortedWith(comparator).partition { it.id in pinnedSet }
        top + rest
    }
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
    val songIds = remember(songs) { songs.mapTo(HashSet()) { it.id } }
    val openCat = categories.firstOrNull { it.id == openCategory }
    val catSongs = remember(sorted, openCat) {
        val ids = openCat?.songIds?.toSet() ?: emptySet()
        sorted.filter { it.id in ids }
    }
    val visibleList: List<Song> = when (tab) {
        0 -> sorted
        1 -> openGroup?.songs ?: emptyList()
        2 -> favs
        3 -> recentList
        else -> if (openCat == null) emptyList() else catSongs
    }
    val selecting = selectedIds.isNotEmpty()
    // Leaving the list you picked from clears the picks.
    LaunchedEffect(tab, openFolder, openCategory) { selectedIds = emptySet() }
    // A category that was deleted while open: go back to the list.
    LaunchedEffect(openCat?.id, openCategory) { if (openCat == null && openCategory != null) openCategory = null }

    BackHandler(enabled = tab == 1 && openFolder != null) { openFolder = null }
    BackHandler(enabled = tab == 4 && openCategory != null) { openCategory = null }
    BackHandler(enabled = selecting) { selectedIds = emptySet() }

    var hideTarget by remember { mutableStateOf<FolderGroup?>(null) }
    rememberHeld(hideTarget)?.let { g ->
        SoftAlertDialog(
            visible = hideTarget != null,
            onDismissRequest = { hideTarget = null },
            title = { Text(s.hideFolder, color = Ui.Ink, fontWeight = FontWeight.Bold) },
            text = { Text(s.hideFolderBody(g.label(s), g.songs.size), color = Ui.Muted, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = {
                    onHideFolder(g.path)
                    if (openFolder == g.path) openFolder = null
                    hideTarget = null
                }) { Text(s.hide, color = Ui.Ink, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { hideTarget = null }) { Text(s.cancel, color = Ui.Muted) } },
        )
    }

    val subtitle = when (tab) {
        0 -> s.songs(sorted.size)
        1 -> if (openFolder == null) s.folders(folders.size) else s.songs(openGroup?.songs?.size ?: 0)
        2 -> s.favorites(favs.size)
        3 -> s.songs(recentList.size)
        else -> if (openCategory == null) s.categoriesCount(categories.size) else s.songs(catSongs.size)
    }
    val resultCount = when (tab) {
        0 -> sorted.size
        1 -> if (openFolder == null) folders.size else openGroup?.songs?.size ?: 0
        2 -> favs.size
        3 -> recentList.size
        else -> if (openCategory == null) categories.size else catSongs.size
    }

    val selection = SongSelection(selectedIds) { id ->
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }
    CompositionLocalProvider(LocalSelection provides selection, LocalPinned provides pinnedSet) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(top = topInset)) {
        AnimatedContent(
            targetState = selecting,
            transitionSpec = { fadeIn(tween(260, easing = Motion.Silk)) togetherWith fadeOut(tween(200, easing = Motion.Silk)) },
            label = "headerswap",
        ) { sel ->
        if (sel) {
            SelectionBar(
                count = selectedIds.size,
                inCategory = tab == 4 && openCat != null,
                onClose = { selectedIds = emptySet() },
                onAll = { selectedIds = visibleList.mapTo(HashSet()) { it.id } },
                onCategory = { catActions.onAddSelected(selectedIds); selectedIds = emptySet() },
                onFavorite = { catActions.onFavoriteSelected(selectedIds); selectedIds = emptySet() },
                onRemove = { openCat?.let { catActions.onRemoveSelected(it, selectedIds) }; selectedIds = emptySet() },
            )
        } else {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(s.library, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ui.Ink, letterSpacing = s.ls((-0.6).sp))
                AnimatedContent(
                    targetState = subtitle,
                    transitionSpec = {
                        (slideInVertically(Motion.soft()) { it / 2 } + fadeIn(tween(260, easing = Motion.Silk))) togetherWith
                            (slideOutVertically(Motion.soft()) { -it / 2 } + fadeOut(tween(260, easing = Motion.Silk)))
                    },
                    label = "subtitle",
                ) { s -> Text(s, fontSize = 12.sp, color = Ui.Muted) }
            }
            IconCircle(Icons.Rounded.BarChart, onOpenStats, size = 38.dp, iconSize = 19.dp, description = s.statsTitle)
            Spacer(Modifier.width(8.dp))
            IconCircle(Icons.Rounded.Settings, onOpenSettings, size = 38.dp, iconSize = 19.dp, description = s.settings)
            Spacer(Modifier.width(8.dp))
            AnimatedVisibility(
                visible = tab != 3,
                enter = scaleIn(animationSpec = Motion.soft()) + fadeIn(Motion.soft()),
                exit = scaleOut(Motion.soft()) + fadeOut(Motion.soft()),
            ) {
                Box {
                    IconCircle(Icons.Rounded.Sort, { sortOpen = true }, size = 38.dp, iconSize = 19.dp, description = "Sort")
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
        }
        }

        SearchPill(query, onQuery, resultCount, Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp))
        SegmentedTabs(
            labels = listOf(s.tabAll, s.tabFolders, s.tabFavorite, s.tabRecent, s.tabCategories),
            selected = tab,
            onSelect = { i -> tabScope.launch { pagerState.animateScrollToPage(i) } },
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 4.dp),
        )

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (!loaded) {
                Skeleton()
            } else {
                // Swipe left / right (or tap a tab) to move between the pages.
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { it },
                ) { t ->
                    when (t) {
                        0 -> SongList(
                            sorted, favorites, currentId, isPlaying, bottomInset,
                            if (q.isNotEmpty()) noResults(q, s) else EmptyInfo(Icons.Rounded.LibraryMusic, s.emptyMusicTitle, s.emptyMusicBody),
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
                                    if (q.isNotEmpty()) noResults(q, s) else EmptyInfo(Icons.Rounded.Folder, s.emptyFoldersTitle, s.emptyFoldersBody),
                                    onLongPress = { hideTarget = it },
                                ) { openFolder = it.path }
                            } else {
                                Column(Modifier.fillMaxSize()) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        IconCircle(Icons.Rounded.ChevronLeft, { openFolder = null }, modifier = Modifier.mirrorInRtl(), size = 42.dp, description = "Back")
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(openGroup?.label(s) ?: "", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ui.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(openGroup?.pathLabel(s) ?: "", fontSize = 12.sp, color = Ui.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        IconCircle(Icons.Rounded.VisibilityOff, { openGroup?.let { hideTarget = it } }, size = 42.dp, description = s.hideFolder)
                                    }
                                    Box(Modifier.weight(1f)) {
                                        SongList(
                                            openGroup?.songs ?: emptyList(), favorites, currentId, isPlaying, bottomInset,
                                            if (q.isNotEmpty()) noResults(q, s) else EmptyInfo(Icons.Rounded.Folder, s.emptyFolderTitle, s.emptyFolderBody),
                                            actions, onPlay, onOpenPlayer,
                                        )
                                    }
                                }
                            }
                        }
                        2 -> SongList(
                            favs, favorites, currentId, isPlaying, bottomInset,
                            if (q.isNotEmpty()) noResults(q, s) else EmptyInfo(Icons.Rounded.Favorite, s.emptyFavTitle, s.emptyFavBody),
                            actions, onPlay, onOpenPlayer,
                        )
                        3 -> SongList(
                            recentList, favorites, currentId, isPlaying, bottomInset,
                            if (q.isNotEmpty()) noResults(q, s) else EmptyInfo(Icons.Rounded.History, s.emptyRecentTitle, s.emptyRecentBody),
                            actions, onPlay, onOpenPlayer,
                        )
                        else -> AnimatedContent(
                            targetState = openCategory,
                            transitionSpec = {
                                if (targetState != null) {
                                    (slideInHorizontally(tween(416, easing = Motion.Silk)) { it / 3 } + fadeIn(tween(312, easing = Motion.Silk))) togetherWith
                                        (slideOutHorizontally(tween(338, easing = Motion.Silk)) { -it / 5 } + fadeOut(tween(260, easing = Motion.Silk)))
                                } else {
                                    (slideInHorizontally(tween(416, easing = Motion.Silk)) { -it / 5 } + fadeIn(tween(312, easing = Motion.Silk))) togetherWith
                                        (slideOutHorizontally(tween(338, easing = Motion.Silk)) { it / 3 } + fadeOut(tween(260, easing = Motion.Silk)))
                                }
                            },
                            label = "categories",
                        ) { id ->
                            if (id == null) {
                                CategoryList(
                                    categories = categories,
                                    songCount = { c -> c.songIds.count { it in songIds } },
                                    bottomInset = bottomInset,
                                    onNew = catActions.onNew,
                                ) { openCategory = it.id }
                            } else {
                                Column(Modifier.fillMaxSize()) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        IconCircle(Icons.Rounded.ChevronLeft, { openCategory = null }, modifier = Modifier.mirrorInRtl(), size = 38.dp, iconSize = 20.dp, description = "Back")
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(openCat?.name ?: "", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ui.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(s.songs(catSongs.size), fontSize = 12.sp, color = Ui.Muted, maxLines = 1)
                                        }
                                        IconCircle(Icons.Rounded.Add, { openCat?.let(catActions.onAddSongs) }, size = 38.dp, iconSize = 19.dp, description = s.addSongs)
                                        Spacer(Modifier.width(8.dp))
                                        IconCircle(Icons.Rounded.Edit, { openCat?.let(catActions.onRename) }, size = 38.dp, iconSize = 18.dp, description = s.renameCategory)
                                        Spacer(Modifier.width(8.dp))
                                        IconCircle(Icons.Rounded.Delete, { openCat?.let(catActions.onDelete) }, size = 38.dp, iconSize = 18.dp, tint = Ui.Danger, description = s.deleteCategory)
                                    }
                                    Box(Modifier.weight(1f)) {
                                        SongList(
                                            catSongs, favorites, currentId, isPlaying, bottomInset,
                                            if (q.isNotEmpty()) noResults(q, s) else EmptyInfo(Icons.Rounded.LibraryMusic, s.emptyCategoryTitle, s.emptyCategoryBody),
                                            actions, onPlay, onOpenPlayer,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    inCategory: Boolean,
    onClose: () -> Unit,
    onAll: () -> Unit,
    onCategory: () -> Unit,
    onFavorite: () -> Unit,
    onRemove: () -> Unit,
) {
    val s = LocalStrings.current
    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconCircle(Icons.Rounded.Close, onClose, size = 38.dp, iconSize = 19.dp, description = s.cancel)
            Spacer(Modifier.width(12.dp))
            Text(s.selectedCount(count), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ui.Ink, modifier = Modifier.weight(1f))
            ActionPill(null, s.selectAll, onAll)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionPill(Icons.Rounded.Add, s.pillCategory, onCategory, Modifier.weight(1f))
            ActionPill(Icons.Rounded.Favorite, s.pillFavorite, onFavorite, Modifier.weight(1f))
            if (inCategory) ActionPill(Icons.Rounded.Close, s.pillRemove, onRemove, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ActionPill(icon: ImageVector?, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.bouncyClick(0.95f, onClick = onClick).clip(RoundedCornerShape(50)).background(Ui.Tile).padding(horizontal = 14.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = Ui.Ink, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ui.Ink, maxLines = 1)
    }
}

private fun noResults(q: String, s: Strings) =
    EmptyInfo(Icons.Rounded.Sort, s.noResultsTitle, s.noResultsBody(q))

private fun FolderGroup.label(s: Strings): String = if (path.isEmpty()) s.storage else name

private fun FolderGroup.pathLabel(s: Strings): String = displayPath.ifEmpty { s.internalStorage }

// ---- Sort menu -----------------------------------------------------------------------------

@Composable
private fun SortMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    field: SortField,
    asc: Boolean,
    onSort: (SortField) -> Unit,
) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    SoftMenu(
        expanded = expanded,
        onDismiss = onDismiss,
        shape = RoundedCornerShape(26.dp),
        gap = 8.dp,
    ) {
        Column(Modifier.width(250.dp).padding(8.dp)) {
            Text(
                s.sortBy, fontSize = 12.sp, color = Ui.Muted, fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 8.dp),
            )
            SortOption(
                icon = Icons.Rounded.SortByAlpha,
                label = s.sortAlpha,
                detail = if (field == SortField.Alphabet && !asc) s.sortZA else s.sortAZ,
                selected = field == SortField.Alphabet,
                asc = asc,
            ) {
                onSort(SortField.Alphabet)
                scope.launch { delay(300); onDismiss() }
            }
            Spacer(Modifier.height(4.dp))
            SortOption(
                icon = Icons.Rounded.AccessTime,
                label = s.sortDate,
                detail = if (field == SortField.DateAdded && asc) s.sortOldest else s.sortNewest,
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
    val s = LocalStrings.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val scrollJob = remember { arrayOfNulls<Job>(1) }
    // Where the playing song sits in this list (-1 when it is not part of it).
    val playingIndex = remember(items, currentId) {
        if (currentId == null) -1 else items.indexOfFirst { it.id == currentId }
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 30.dp, end = 30.dp, top = 8.dp, bottom = bottomInset),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, song ->
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

        // Small floating button: wherever the list is scrolled, one tap brings the playing song into view.
        AnimatedVisibility(
            visible = playingIndex >= 0,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = bottomInset + 4.dp),
            enter = scaleIn(Motion.soft(), initialScale = 0.5f) + fadeIn(Motion.soft()),
            exit = scaleOut(Motion.soft(), targetScale = 0.5f) + fadeOut(Motion.soft()),
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .bouncyClick(0.88f) {
                        scrollJob[0]?.cancel()
                        scrollJob[0] = scope.launch {
                            listState.glideToCenter(
                                index = playingIndex.coerceAtLeast(0),
                                itemPx = with(density) { 72.dp.toPx() },
                                rowPx = with(density) { 64.dp.toPx() },
                            )
                        }
                    }
                    .shadow(10.dp, CircleShape, ambientColor = Color.Black.copy(0.12f), spotColor = Color.Black.copy(0.3f))
                    .clip(CircleShape)
                    .background(Ui.GreenBrush),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.MyLocation, s.locateSong, tint = Ui.OnAccent, modifier = Modifier.size(22.dp))
            }
        }
    }
}

/**
 * Scrolls so the row at [index] ends up in the middle of the screen, as one long, slow glide
 * (ease in, ease out) instead of the quick default animation. Rows are all the same height, so the
 * distance can be worked out up front even when the row is far off screen; a short second glide
 * then fixes any small difference.
 */
private suspend fun LazyListState.glideToCenter(index: Int, itemPx: Float, rowPx: Float) {
    // The first pass covers the long distance in one slow glide using the (uniform) row height to
    // work out where the row is. If that estimate was a little off, the next passes finish the
    // job, so one tap is always enough.
    repeat(4) { pass ->
        val visible = layoutInfo.visibleItemsInfo
        val first = visible.firstOrNull() ?: return
        val center = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
        val item = visible.firstOrNull { it.index == index }
        val delta = if (item != null) {
            item.offset + item.size / 2f - center
        } else {
            first.offset + (index - first.index) * itemPx + rowPx / 2f - center
        }
        if (abs(delta) < 2f) return
        val ms = if (item != null && pass > 0) {
            320
        } else {
            val rows = abs(delta) / itemPx
            (800 + 120 * sqrt(rows.toDouble())).toInt().coerceIn(800, 2000)
        }
        val easing = if (item != null && pass > 0) Motion.Silk else Motion.Glide
        val consumed = animateScrollBy(delta, tween(ms, easing = easing))
        // Hit the start or end of the list: the row cannot be centred any further.
        if (abs(consumed) < 1f) return
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
    val s = LocalStrings.current
    val sel = LocalSelection.current
    val selected = song.id in sel.ids
    var shown by remember { mutableStateOf(index >= 12) }
    LaunchedEffect(Unit) {
        if (!shown) {
            delay(index * 30L)
            shown = true
        }
    }
    val a by animateFloatAsState(if (shown) 1f else 0f, tween(450, easing = Motion.Silk), label = "rowa")
    val bg by animateColorAsState(if (isCurrent || selected) Ui.GreenTint else Ui.Card, tween(390, easing = Motion.Silk), label = "rowbg")
    Row(
        modifier
            .graphicsLayer { alpha = a; translationY = (1f - a) * 26.dp.toPx() }
            .fillMaxWidth()
            .bouncyClick(0.97f, onLongClick = { sel.toggle(song.id) }) { if (sel.active) sel.toggle(song.id) else onClick() }
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(song, Modifier.size(48.dp), RoundedCornerShape(14.dp), 160)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    song.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ui.Ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                )
                if (song.id in LocalPinned.current) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Rounded.PushPin, null, tint = Ui.GreenA, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                "${s.artist(song.artist)}  \u00B7  ${s.time(song.durationMs)}", fontSize = 12.5.sp, color = Ui.Muted,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        if (sel.active) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { CheckDot(selected) }
        } else {
            AnimatedVisibility(visible = isCurrent, enter = scaleIn(Motion.soft()) + fadeIn(Motion.soft()), exit = scaleOut(Motion.soft()) + fadeOut(Motion.soft())) {
                EqualizerBars(isPlaying, Modifier.padding(horizontal = 8.dp))
            }
            SongMenuButton(song, isFav, actions)
        }
    }
}

@Composable
private fun FolderList(
    folders: List<FolderGroup>,
    bottomInset: Dp,
    empty: EmptyInfo,
    onLongPress: (FolderGroup) -> Unit,
    onOpen: (FolderGroup) -> Unit,
) {
    if (folders.isEmpty()) {
        EmptyState(empty, Modifier.padding(bottom = bottomInset))
        return
    }
    val s = LocalStrings.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 30.dp, end = 30.dp, top = 8.dp, bottom = bottomInset),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(folders, key = { _, g -> g.path }) { index, g ->
            Row(
                Modifier
                    .animateItem(fadeInSpec = Motion.soft(), placementSpec = Motion.soft(), fadeOutSpec = Motion.soft())
                    .staggeredAppear(index)
                    .fillMaxWidth()
                    .bouncyClick(0.97f, onLongClick = { onLongPress(g) }) { onOpen(g) }
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
                    Text(g.label(s), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ui.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                    Text(g.pathLabel(s), fontSize = 12.sp, color = Ui.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(Modifier.clip(RoundedCornerShape(50)).background(Ui.Tile).padding(horizontal = 11.dp, vertical = 5.dp)) {
                    Text(s.num(g.songs.size), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ui.Ink)
                }
                Spacer(Modifier.width(2.dp))
                Icon(Icons.Rounded.ChevronRight, null, tint = Ui.Muted, modifier = Modifier.size(22.dp).mirrorInRtl())
            }
        }
    }
}

@Composable
internal fun EmptyState(info: EmptyInfo, modifier: Modifier = Modifier) {
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
