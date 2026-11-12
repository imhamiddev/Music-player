package com.imhamiddev.musicplayer.data

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val dateAdded: Long,
    val path: String,
    val uri: Uri,
) {
    val folderPath: String = path.substringBeforeLast('/', "")
    val folderName: String = folderPath.substringAfterLast('/').ifEmpty { "Storage" }
}

data class FolderGroup(
    val path: String,
    val name: String,
    val songs: List<Song>,
) {
    val latest: Long = songs.maxOfOrNull { it.dateAdded } ?: 0L
    val displayPath: String =
        path.removePrefix("/storage/emulated/0").removePrefix("/").ifEmpty { "Internal storage" }
}

enum class SortField { Alphabet, DateAdded }

enum class ThemeMode { System, Light, Dark }
