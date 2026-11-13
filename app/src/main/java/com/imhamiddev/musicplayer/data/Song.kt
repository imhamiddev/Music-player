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

    companion object {
        /** Placeholders stored in [artist] / [album]; the UI swaps them for the localized text. */
        const val UNKNOWN_ARTIST = "Unknown artist"
        const val UNKNOWN_ALBUM = "Unknown album"
    }
}

data class FolderGroup(
    val path: String,
    val name: String,
    val songs: List<Song>,
) {
    val latest: Long = songs.maxOfOrNull { it.dateAdded } ?: 0L
    /** Path relative to internal storage; empty for the storage root (the UI shows a localized label). */
    val displayPath: String =
        path.removePrefix("/storage/emulated/0").removePrefix("/")
}

enum class SortField { Alphabet, DateAdded }

enum class ThemeMode { System, Light, Dark }

enum class AppLanguage { English, Persian }

/** How the song progress is drawn in the player: the waveform or a plain line. */
enum class ProgressStyle { Waveform, Line }
