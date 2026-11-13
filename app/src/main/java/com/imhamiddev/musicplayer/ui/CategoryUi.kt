package com.imhamiddev.musicplayer.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imhamiddev.musicplayer.data.Category
import com.imhamiddev.musicplayer.data.Song

/** What the library screen can ask for on categories; the dialogs themselves live in [MusicApp]. */
class CategoryActions(
    val onNew: () -> Unit,
    val onRename: (Category) -> Unit,
    val onDelete: (Category) -> Unit,
    val onAddSongs: (Category) -> Unit,
    val onAddSelected: (Set<Long>) -> Unit,
    val onRemoveSelected: (Category, Set<Long>) -> Unit,
    val onFavoriteSelected: (Set<Long>) -> Unit,
)

// ---- List of categories (the "Lists" tab) -------------------------------------------------

@Composable
fun CategoryList(
    categories: List<Category>,
    songCount: (Category) -> Int,
    bottomInset: Dp,
    onNew: () -> Unit,
    onOpen: (Category) -> Unit,
) {
    val s = LocalStrings.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 30.dp, end = 30.dp, top = 8.dp, bottom = bottomInset),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "new") {
            Row(
                Modifier
                    .animateItem(fadeInSpec = Motion.soft(), placementSpec = Motion.soft(), fadeOutSpec = Motion.soft())
                    .fillMaxWidth()
                    .bouncyClick(0.97f, onClick = onNew)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Ui.GreenTint)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Ui.GreenBrush), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Add, null, tint = Ui.OnAccent, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(s.newCategory, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ui.Ink)
            }
        }
        if (categories.isEmpty()) {
            item(key = "empty") {
                Box(Modifier.fillMaxWidth().height(340.dp)) {
                    EmptyState(EmptyInfo(Icons.Rounded.LibraryMusic, s.emptyCategoriesTitle, s.emptyCategoriesBody))
                }
            }
        }
        items(categories, key = { it.id }) { c ->
            Row(
                Modifier
                    .animateItem(fadeInSpec = Motion.soft(), placementSpec = Motion.soft(), fadeOutSpec = Motion.soft())
                    .fillMaxWidth()
                    .bouncyClick(0.97f) { onOpen(c) }
                    .clip(RoundedCornerShape(22.dp))
                    .background(Ui.Card)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Ui.Tile), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.LibraryMusic, null, tint = Ui.Ink, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(c.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ui.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                    Text(s.songs(songCount(c)), fontSize = 12.sp, color = Ui.Muted, maxLines = 1)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = Ui.Muted, modifier = Modifier.size(22.dp).mirrorInRtl())
            }
        }
    }
}

// ---- Dialogs --------------------------------------------------------------------------------

@Composable
private fun NameField(value: String, onValue: (String) -> Unit, hint: String, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(16.dp)).background(Ui.Tile).padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) Text(hint, color = Ui.Muted, fontSize = 15.sp, maxLines = 1)
        BasicTextField(
            value = value,
            onValueChange = { if (it.length <= 40) onValue(it) },
            singleLine = true,
            textStyle = TextStyle(color = Ui.Ink, fontSize = 16.sp),
            cursorBrush = SolidColor(Ui.GreenA),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Used both to create a category (title "New category") and to rename one. */
@Composable
fun CategoryNameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val s = LocalStrings.current
    var text by remember(initial) { mutableStateOf(initial) }
    SoftAlertDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        title = { Text(title, color = Ui.Ink, fontWeight = FontWeight.Bold) },
        text = { NameField(text, { text = it }, s.categoryNameHint, Modifier.fillMaxWidth()) },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text) }) {
                Text(confirmLabel, color = if (text.isBlank()) Ui.Muted else Ui.Ink, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel, color = Ui.Muted) } },
    )
}

@Composable
fun DeleteCategoryDialog(category: Category, visible: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val s = LocalStrings.current
    SoftAlertDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        title = { Text(s.deleteCategory, color = Ui.Ink, fontWeight = FontWeight.Bold) },
        text = { Text(s.deleteCategoryBody(category.name), color = Ui.Muted, fontSize = 14.sp) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(s.delete, color = Ui.Danger, fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel, color = Ui.Muted) } },
    )
}

@Composable
internal fun CheckDot(checked: Boolean) {
    val bg by animateColorAsState(if (checked) Ui.GreenA else Ui.Tile, tween(260, easing = Motion.Silk), label = "dotbg")
    Box(Modifier.size(26.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        if (checked) Icon(Icons.Rounded.Check, null, tint = Ui.OnAccent, modifier = Modifier.size(16.dp))
    }
}

/** From a song's menu (or the selection bar): tick the categories the song(s) should be in, or make a new one. */
@Composable
fun CategoryPickerDialog(
    songIds: Set<Long>,
    categories: List<Category>,
    visible: Boolean,
    onToggle: (Category) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val s = LocalStrings.current
    var newName by remember(songIds) { mutableStateOf("") }
    SoftAlertDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        title = { Text(s.addToCategory, color = Ui.Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    if (categories.isEmpty()) s.noCategoriesYet else s.addToCategoryNote,
                    color = Ui.Muted, fontSize = 13.sp,
                )
                if (categories.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    LazyColumn(Modifier.heightIn(max = 230.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(categories, key = { it.id }) { c ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onToggle(c) }
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    c.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Ui.Ink,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                                )
                                CheckDot(songIds.isNotEmpty() && c.songIds.containsAll(songIds))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NameField(newName, { newName = it }, s.newCategory, Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    IconCircle(
                        Icons.Rounded.Add,
                        { if (newName.isNotBlank()) { onCreate(newName); newName = "" } },
                        size = 44.dp, container = Ui.GreenA, tint = Ui.OnAccent, description = s.create,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(s.done, color = Ui.Ink, fontWeight = FontWeight.SemiBold) } },
    )
}

/** From a category's page: tick any number of songs to put in it. */
@Composable
fun AddSongsDialog(
    category: Category,
    songs: List<Song>,
    visible: Boolean,
    onToggle: (Song) -> Unit,
    onDismiss: () -> Unit,
) {
    val s = LocalStrings.current
    SoftAlertDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        title = { Text("${s.addSongs} · ${category.name}", color = Ui.Ink, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            LazyColumn(Modifier.height(340.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(songs, key = { it.id }) { song ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onToggle(song) }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AlbumArt(song, Modifier.size(40.dp), RoundedCornerShape(12.dp), 120)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(song.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ui.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(s.artist(song.artist), fontSize = 12.sp, color = Ui.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.width(8.dp))
                        CheckDot(song.id in category.songIds)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(s.done, color = Ui.Ink, fontWeight = FontWeight.SemiBold) } },
    )
}
