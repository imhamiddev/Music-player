package com.imhamiddev.musicplayer.ui

import android.app.RecoverableSecurityException
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
    val onPlayNext: (Song) -> Unit,
    val onRename: (Song) -> Unit,
    val onShare: (Song) -> Unit,
    val onDetails: (Song) -> Unit,
    val onDelete: (Song) -> Unit,
)

@Composable
fun SongMenuButton(song: Song, isFav: Boolean, actions: SongActions, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Box(Modifier.size(40.dp).bouncyClick(0.85f) { open = true }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.MoreVert, "More", tint = Ui.Muted, modifier = Modifier.size(22.dp))
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            offset = DpOffset(0.dp, 0.dp),
            shape = RoundedCornerShape(22.dp),
            containerColor = Ui.Card,
            tonalElevation = 0.dp,
            shadowElevation = 18.dp,
        ) {
            Column(Modifier.width(220.dp).padding(6.dp)) {
                MenuItem(Icons.Rounded.SkipNext, "Play next") { open = false; actions.onPlayNext(song) }
                MenuItem(
                    if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    if (isFav) "Remove from favorites" else "Add to favorites",
                    tint = if (isFav) Ui.Pink else Ui.Ink,
                ) { open = false; actions.onToggleFav(song) }
                MenuItem(Icons.Rounded.Edit, "Rename") { open = false; actions.onRename(song) }
                MenuItem(Icons.Rounded.Share, "Share") { open = false; actions.onShare(song) }
                MenuItem(Icons.Rounded.Info, "Details") { open = false; actions.onDetails(song) }
                MenuItem(Icons.Rounded.Delete, "Delete", tint = Ui.Danger, textColor = Ui.Danger) { open = false; actions.onDelete(song) }
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
fun RenameDialog(song: Song, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember(song.id) { mutableStateOf(song.title) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Ui.Card,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Rename", color = Ui.Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Changes the title shown in this app. The file on your device is not modified.", color = Ui.Muted, fontSize = 13.sp)
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
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("Save", color = Ui.Ink, fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Ui.Muted) } },
    )
}

@Composable
fun DetailsDialog(song: Song, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Ui.Card,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Details", color = Ui.Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                DetailRow("Title", song.title)
                DetailRow("Artist", song.artist)
                DetailRow("Album", song.album)
                DetailRow("Duration", fmt(song.durationMs))
                DetailRow("Format", song.path.substringAfterLast('.', "").uppercase().ifEmpty { "Unknown" })
                DetailRow("Added", DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(song.dateAdded * 1000)))
                DetailRow("Location", song.path.ifEmpty { "Unknown" })
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = Ui.Ink, fontWeight = FontWeight.SemiBold) } },
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
fun DeleteDialog(song: Song, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Ui.Card,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Delete song?", color = Ui.Ink, fontWeight = FontWeight.Bold) },
        text = { Text("\u201C${song.title}\u201D will be permanently deleted from your device.", color = Ui.Muted, fontSize = 14.sp) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete", color = Ui.Danger, fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Ui.Muted) } },
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

/**
 * Deletes the file. Android 11+ asks the system for a confirmation sheet ([launch] receives it);
 * on 10 it may need a per-file grant; older versions can delete directly. [onDone] runs when the
 * deletion already happened synchronously.
 */
fun deleteSong(ctx: Context, song: Song, launch: (IntentSender) -> Unit, onDone: () -> Unit) {
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
            Toast.makeText(ctx, "Can't delete this file", Toast.LENGTH_SHORT).show()
        }
    }
}
