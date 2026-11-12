package com.imhamiddev.musicplayer.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads audio files only (never video) from MediaStore. */
object MusicRepository {

    suspend fun load(context: Context): List<Song> = withContext(Dispatchers.IO) {
        val result = ArrayList<Song>()
        val collection = if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA,
        )
        val selection = "${MediaStore.Audio.Media.IS_RINGTONE} = 0 AND " +
            "${MediaStore.Audio.Media.IS_NOTIFICATION} = 0 AND " +
            "${MediaStore.Audio.Media.IS_ALARM} = 0 AND " +
            "${MediaStore.Audio.Media.DURATION} >= 10000"
        try {
            context.contentResolver.query(
                collection, projection, selection, null,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC",
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val addedCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val path = c.getString(dataCol) ?: ""
                    val rawTitle = c.getString(titleCol)
                    val title = if (rawTitle.isNullOrBlank()) {
                        path.substringAfterLast('/').substringBeforeLast('.')
                    } else rawTitle
                    val rawArtist = c.getString(artistCol)
                    val artist = if (rawArtist.isNullOrBlank() || rawArtist == MediaStore.UNKNOWN_STRING) {
                        "Unknown artist"
                    } else rawArtist
                    val rawAlbum = c.getString(albumCol)
                    val album = if (rawAlbum.isNullOrBlank() || rawAlbum == MediaStore.UNKNOWN_STRING) {
                        "Unknown album"
                    } else rawAlbum
                    result.add(
                        Song(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            albumId = c.getLong(albumIdCol),
                            durationMs = c.getLong(durCol),
                            dateAdded = c.getLong(addedCol),
                            path = path,
                            uri = ContentUris.withAppendedId(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id,
                            ),
                        ),
                    )
                }
            }
        } catch (_: Exception) {
        }
        result
    }
}
