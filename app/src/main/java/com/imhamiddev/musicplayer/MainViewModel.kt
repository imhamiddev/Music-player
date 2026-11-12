package com.imhamiddev.musicplayer

import android.app.Application
import android.content.ComponentName
import android.content.ContentUris
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.imhamiddev.musicplayer.data.MusicRepository
import com.imhamiddev.musicplayer.data.Prefs
import com.imhamiddev.musicplayer.data.Song
import com.imhamiddev.musicplayer.data.SortField
import com.imhamiddev.musicplayer.playback.PlaybackService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val _favorites = MutableStateFlow(prefs.favorites())
    val favorites: StateFlow<Set<Long>> = _favorites.asStateFlow()

    private val _recents = MutableStateFlow(prefs.recents())
    val recents: StateFlow<List<Long>> = _recents.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _sortField = MutableStateFlow(prefs.sortField())
    val sortField: StateFlow<SortField> = _sortField.asStateFlow()

    private val _sortAsc = MutableStateFlow(prefs.sortAsc())
    val sortAsc: StateFlow<Boolean> = _sortAsc.asStateFlow()

    private val _currentId = MutableStateFlow<Long?>(null)
    val currentId: StateFlow<Long?> = _currentId.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()

    private val _repeat = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeat: StateFlow<Int> = _repeat.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private var controller: MediaController? = null
    private val controllerFuture: ListenableFuture<MediaController>

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val id = mediaItem?.mediaId?.toLongOrNull()
            _currentId.value = id
            _positionMs.value = 0L
            if (id != null) addRecent(id)
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _shuffle.value = shuffleModeEnabled
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeat.value = repeatMode
        }
    }

    init {
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(app, token).buildAsync()
        controllerFuture.addListener({
            try {
                val c = controllerFuture.get()
                controller = c
                c.addListener(listener)
                _currentId.value = c.currentMediaItem?.mediaId?.toLongOrNull()
                _isPlaying.value = c.isPlaying
                _shuffle.value = c.shuffleModeEnabled
                _repeat.value = c.repeatMode
                _positionMs.value = c.currentPosition.coerceAtLeast(0L)
            } catch (_: Exception) {
            }
        }, ContextCompat.getMainExecutor(app))

        viewModelScope.launch {
            while (isActive) {
                delay(250)
                controller?.let { _positionMs.value = it.currentPosition.coerceAtLeast(0L) }
            }
        }
    }

    // ---- Library -----------------------------------------------------------------------------

    fun loadSongs() {
        viewModelScope.launch {
            _songs.value = MusicRepository.load(getApplication())
            _loaded.value = true
        }
    }

    fun setQuery(q: String) {
        _query.value = q
    }

    fun setSort(field: SortField) {
        if (_sortField.value == field) {
            _sortAsc.value = !_sortAsc.value
        } else {
            _sortField.value = field
            _sortAsc.value = field == SortField.Alphabet
        }
        prefs.saveSort(_sortField.value, _sortAsc.value)
    }

    fun toggleFavorite(id: Long) {
        val next = _favorites.value.toMutableSet()
        if (!next.add(id)) next.remove(id)
        _favorites.value = next
        prefs.saveFavorites(next)
    }

    private fun addRecent(id: Long) {
        val next = (listOf(id) + _recents.value.filter { it != id }).take(60)
        _recents.value = next
        prefs.saveRecents(next)
    }

    // ---- Playback ----------------------------------------------------------------------------

    private fun toMediaItem(song: Song): MediaItem {
        val art: Uri = ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"), song.albumId,
        )
        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.uri)
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(song.uri).build())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(art)
                    .build(),
            )
            .build()
    }

    fun play(song: Song, queue: List<Song>) {
        val c = controller ?: return
        val index = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        val same = c.mediaItemCount == queue.size &&
            queue.indices.all { c.getMediaItemAt(it).mediaId == queue[it].id.toString() }
        if (same) {
            c.seekTo(index, 0L)
        } else {
            c.setMediaItems(queue.map { toMediaItem(it) }, index, 0L)
        }
        c.prepare()
        c.play()
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() {
        controller?.seekToNext()
    }

    fun previous() {
        controller?.seekToPrevious()
    }

    fun seekToFraction(fraction: Float, durationMs: Long) {
        val target = (fraction.coerceIn(0f, 1f) * durationMs).toLong()
        controller?.seekTo(target)
        _positionMs.value = target
    }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    override fun onCleared() {
        controller?.removeListener(listener)
        MediaController.releaseFuture(controllerFuture)
        super.onCleared()
    }
}
