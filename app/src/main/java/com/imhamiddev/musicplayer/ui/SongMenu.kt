package com.imhamiddev.musicplayer.ui

import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imhamiddev.musicplayer.data.Song
import java.text.DateFormat
import java.util.Date

/** Everything the three-dot menu on a song row can do. */
class SongActions(
    val onToggleFav: (Song) -> Unit,
    val onAddToCategory: (Song) -> Unit,
    val onPlayNext: (Song) -> Unit,
    val onRename: (Song) -> Unit,
    val onShare: (Song) -> Unit,
    val onDetails: (Song) -> Unit,
    val onDelete: (Song) -> Unit,
)

@Composable
fun SongMenuButton(song: Song, isFav: Boolean, actions: SongActions, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Box(Modifier.size(40.dp).bouncyClick(0.85f) { open = true }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.MoreVert, "More", tint = Ui.Muted, modifier = Modifier.size(22.dp))
        }
        SoftMenu(
            expanded = open,
            onDismiss = { open = false },
            shape = RoundedCornerShape(22.dp),
        ) {
            Column(Modifier.width(220.dp).padding(6.dp)) {
                MenuItem(Icons.Rounded.SkipNext, s.playNext) { open = false; actions.onPlayNext(song) }
                MenuItem(
                    if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    if (isFav) s.removeFav else s.addFav,
                    tint = if (isFav) Ui.Pink else Ui.Ink,
                ) { open = false; actions.onToggleFav(song) }
                MenuItem(Icons.Rounded.Add, s.addToCategory) { open = false; actions.onAddToCategory(song) }
                MenuItem(Icons.Rounded.Edit, s.rename) { open = false; actions.onRename(song) }
                MenuItem(Icons.Rounded.Share, s.share) { open = false; actions.onShare(song) }
                MenuItem(Icons.Rounded.Info, s.details) { open = false; actions.onDetails(song) }
                MenuItem(Icons.Rounded.Delete, s.delete, tint = Ui.Danger, textColor = Ui.Danger) { open = false; actions.onDelete(song) }
            }
        }
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    label: String,
    tint: Color = Ui.Ink,
    textColor: Color = Ui.Ink,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .bouncyClick(0.97f, onClick = onClick)
            .clip(RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ---- Dialogs -------------------------------------------------------------------------------

@Composable
fun RenameDialog(song: Song, visible: Boolean = true, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val s = LocalStrings.current
    var text by remember(song.id) { mutableStateOf(song.title) }
    SoftAlertDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        title = { Text(s.rename, color = Ui.Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(s.renameNote, color = Ui.Muted, fontSize = 13.sp)
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Ui.Tile).padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Ui.Ink, fontSize = 16.sp),
                        cursorBrush = SolidColor(Ui.GreenA),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text(s.save, color = Ui.Ink, fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel, color = Ui.Muted) } },
    )
}

@Composable
fun DetailsDialog(song: Song, visible: Boolean = true, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    SoftAlertDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        title = { Text(s.details, color = Ui.Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                DetailRow(s.detailTitle, song.title)
                DetailRow(s.detailArtist, s.artist(song.artist))
                DetailRow(s.detailAlbum, s.album(song.album))
                DetailRow(s.detailDuration, s.time(song.durationMs))
                DetailRow(s.detailFormat, song.path.substringAfterLast('.', "").uppercase().ifEmpty { s.unknown })
                DetailRow(s.detailAdded, s.digits(DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(song.dateAdded * 1000))))
                DetailRow(s.detailLocation, song.path.ifEmpty { s.unknown })
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(s.close, color = Ui.Ink, fontWeight = FontWeight.SemiBold) } },
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(Modifier.padding(bottom = 10.dp)) {
        Text(label, color = Ui.Muted, fontSize = 12.sp)
        Text(value, color = Ui.Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun DeleteDialog(song: Song, visible: Boolean = true, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val s = LocalStrings.current
    SoftAlertDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        title = { Text(s.deleteTitle, color = Ui.Ink, fontWeight = FontWeight.Bold) },
        text = { Text(s.deleteBody(song.title), color = Ui.Muted, fontSize = 14.sp) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(s.delete, color = Ui.Danger, fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel, color = Ui.Muted) } },
    )
}

// ---- Helpers -------------------------------------------------------------------------------

fun shareSong(ctx: Context, song: Song) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "audio/*"
        putExtra(Intent.EXTRA_STREAM, song.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { ctx.startActivity(Intent.createChooser(send, song.title)) }
}

/** File names cannot hold / \\ : * ? " < > | and should not end with a dot. */
private fun safeFileName(title: String): String =
    title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().trimEnd('.').trim()

/** Renames the real file (and its title tag in the media database) to [newTitle], keeping the extension. */
fun applySongRename(ctx: Context, song: Song, newTitle: String): Boolean {
    val base = safeFileName(newTitle)
    if (base.isEmpty()) return false
    val ext = song.path.substringAfterLast('.', "")
    val values = ContentValues().apply {
        put(MediaStore.Audio.Media.DISPLAY_NAME, if (ext.isNotEmpty()) "$base.$ext" else base)
        put(MediaStore.Audio.Media.TITLE, newTitle.trim())
    }
    return runCatching { ctx.contentResolver.update(song.uri, values, null, null) > 0 }.getOrDefault(false)
}

/**
 * Starts a rename. Android 11+ asks the system for permission to modify the file ([launch] gets the
 * sheet; call [applySongRename] when it returns OK). On 10 a per-file grant may be needed; older
 * versions can rename directly. [onDone] runs when the rename already happened.
 */
fun renameSongFile(ctx: Context, song: Song, newTitle: String, failMessage: String, launch: (IntentSender) -> Unit, onDone: () -> Unit) {
    if (Build.VERSION.SDK_INT >= 30) {
        launch(MediaStore.createWriteRequest(ctx.contentResolver, listOf(song.uri)).intentSender)
        return
    }
    val base = safeFileName(newTitle)
    if (base.isEmpty()) return
    val ext = song.path.substringAfterLast('.', "")
    val values = ContentValues().apply {
        put(MediaStore.Audio.Media.DISPLAY_NAME, if (ext.isNotEmpty()) "$base.$ext" else base)
        put(MediaStore.Audio.Media.TITLE, newTitle.trim())
    }
    try {
        if (ctx.contentResolver.update(song.uri, values, null, null) > 0) onDone()
        else Toast.makeText(ctx, failMessage, Toast.LENGTH_SHORT).show()
    } catch (e: SecurityException) {
        if (Build.VERSION.SDK_INT >= 29 && e is RecoverableSecurityException) {
            launch(e.userAction.actionIntent.intentSender)
        } else {
            Toast.makeText(ctx, failMessage, Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(ctx, failMessage, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Deletes the file. Android 11+ asks the system for a confirmation sheet ([launch] receives it);
 * on 10 it may need a per-file grant; older versions can delete directly. [onDone] runs when the
 * deletion already happened synchronously.
 */
fun deleteSong(ctx: Context, song: Song, failMessage: String, launch: (IntentSender) -> Unit, onDone: () -> Unit) {
    if (Build.VERSION.SDK_INT >= 30) {
        val pi = MediaStore.createDeleteRequest(ctx.contentResolver, listOf(song.uri))
        launch(pi.intentSender)
        return
    }
    try {
        ctx.contentResolver.delete(song.uri, null, null)
        onDone()
    } catch (e: SecurityException) {
        if (Build.VERSION.SDK_INT >= 29 && e is RecoverableSecurityException) {
            launch(e.userAction.actionIntent.intentSender)
        } else {
            Toast.makeText(ctx, failMessage, Toast.LENGTH_SHORT).show()
        }
    }
}
