package com.imhamiddev.musicplayer

import android.app.Application
import android.content.ComponentName
import android.content.ContentUris
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.imhamiddev.musicplayer.data.AppLanguage
import com.imhamiddev.musicplayer.data.Category
import com.imhamiddev.musicplayer.data.ListenStats
import com.imhamiddev.musicplayer.data.MusicRepository
import com.imhamiddev.musicplayer.data.Prefs
import com.imhamiddev.musicplayer.data.ProgressStyle
import com.imhamiddev.musicplayer.data.StatsSnapshot
import com.imhamiddev.musicplayer.data.Song
import com.imhamiddev.musicplayer.data.SortField
import com.imhamiddev.musicplayer.data.ThemeMode
import com.imhamiddev.musicplayer.data.UpdateState
import com.imhamiddev.musicplayer.data.Updater
import android.net.ConnectivityManager
import android.net.Network
import com.imhamiddev.musicplayer.playback.PlaybackFx
import com.imhamiddev.musicplayer.playback.PlaybackService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)

    private val _rawSongs = MutableStateFlow<List<Song>>(emptyList())
    private val _titles = MutableStateFlow(prefs.titles())

    private val _hiddenFolders = MutableStateFlow(prefs.hiddenFolders())
    val hiddenFolders: StateFlow<Set<String>> = _hiddenFolders.asStateFlow()

    /** Library songs with the user's in-app renames applied, minus songs in hidden folders. */
    val songs: StateFlow<List<Song>> = combine(_rawSongs, _titles, _hiddenFolders) { raw, titles, hidden ->
        val list = if (hidden.isEmpty()) raw else raw.filter { it.folderPath !in hidden }
        if (titles.isEmpty()) list else list.map { s -> titles[s.id]?.let { s.copy(title = it) } ?: s }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _themeMode = MutableStateFlow(prefs.themeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _language = MutableStateFlow(prefs.language())
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _progressStyle = MutableStateFlow(prefs.progressStyle())
    val progressStyle: StateFlow<ProgressStyle> = _progressStyle.asStateFlow()

    private val _stats = MutableStateFlow(StatsSnapshot.Empty)
    val stats: StateFlow<StatsSnapshot> = _stats.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val _favorites = MutableStateFlow(prefs.favorites())
    val favorites: StateFlow<Set<Long>> = _favorites.asStateFlow()

    private val _categories = MutableStateFlow(prefs.categories())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

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

    // ---- App updates (GitHub releases) -------------------------------------------------------

    private val _update = MutableStateFlow<UpdateState>(UpdateState.None)
    val update: StateFlow<UpdateState> = _update.asStateFlow()
    private var checking = false
    private var dismissedVersion: String? = null
    private val netCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            checkForUpdate()
        }
    }

    fun checkForUpdate() {
        if (checking || _update.value != UpdateState.None) return
        val app = getApplication<Application>()
        if (!Updater.isOnline(app)) return
        checking = true
        viewModelScope.launch {
            val info = try { Updater.check(app) } finally { checking = false }
            if (info != null && info.version != dismissedVersion && _update.value == UpdateState.None) {
                _update.value = UpdateState.Available(info)
            }
        }
    }

    /** Only runs after the user taps Download. */
    fun downloadUpdate() {
        val info = when (val s = _update.value) {
            is UpdateState.Available -> s.info
            is UpdateState.Failed -> s.info
            else -> return
        }
        _update.value = UpdateState.Downloading(info, 0f)
        viewModelScope.launch {
            try {
                val file = Updater.download(getApplication(), info) { p ->
                    _update.value = UpdateState.Downloading(info, p)
                }
                _update.value = UpdateState.Ready(info, file)
            } catch (_: Exception) {
                _update.value = UpdateState.Failed(info)
            }
        }
    }

    fun installUpdate() {
        val s = _update.value as? UpdateState.Ready ?: return
        Updater.install(getApplication(), s.file)
    }

    fun dismissUpdate() {
        val s = _update.value
        val v = when (s) {
            is UpdateState.Available -> s.info.version
            is UpdateState.Failed -> s.info.version
            is UpdateState.Ready -> s.info.version
            else -> null
        }
        if (v != null) dismissedVersion = v
        if (s !is UpdateState.Downloading) _update.value = UpdateState.None
    }

    /** Real duration reported by the player (MediaStore's value is often wrong for VBR files). 0 = unknown. */
    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private fun syncDuration() {
        val d = controller?.duration ?: C.TIME_UNSET
        _durationMs.value = if (d == C.TIME_UNSET || d <= 0L) 0L else d
    }

    private var controller: MediaController? = null

    // Set while the saved queue is put back, so the restored song is not bumped to the top of "Recent".
    private var skipRecentId: Long? = null
    private var restoreDone = false
    private val controllerFuture: ListenableFuture<MediaController>

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val id = mediaItem?.mediaId?.toLongOrNull()
            _currentId.value = id
            _positionMs.value = 0L
            _durationMs.value = 0L
            syncDuration()
            if (id != null && id == skipRecentId) skipRecentId = null
            else if (id != null) addRecent(id)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            syncDuration()
        }

        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
            syncDuration()
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
                syncDuration()
                tryRestore()
            } catch (_: Exception) {
            }
        }, ContextCompat.getMainExecutor(app))

        ListenStats.init(app)
        PlaybackFx.init(app)
        refreshStats()

        runCatching { app.getSystemService(ConnectivityManager::class.java)?.registerDefaultNetworkCallback(netCallback) }

        viewModelScope.launch {
            while (isActive) {
                delay(250)
                controller?.let { _positionMs.value = it.currentPosition.coerceAtLeast(0L) }
                if (_durationMs.value == 0L) syncDuration()
            }
        }
    }

    // ---- Library -----------------------------------------------------------------------------

    fun loadSongs() {
        viewModelScope.launch {
            _rawSongs.value = MusicRepository.load(getApplication())
            _loaded.value = true
            tryRestore()
        }
    }

    fun refreshStats() {
        _stats.value = ListenStats.snapshot()
    }

    /**
     * Puts the last queue back (paused, at the saved position) when the app starts fresh, so playback
     * continues from the same song after the app was closed. Needs both the library and the player
     * connection, so it is tried from both places and runs once.
     */
    private fun tryRestore() {
        if (restoreDone) return
        val c = controller ?: return
        if (!_loaded.value) return
        restoreDone = true
        // The service is still alive with a queue (e.g. music kept playing): nothing to restore.
        if (c.mediaItemCount > 0) return
        val savedIds = prefs.playbackQueue()
        if (savedIds.isEmpty()) return
        val byId = _rawSongs.value.associateBy { it.id }
        val titles = _titles.value
        val queue = savedIds.mapNotNull { byId[it] }.map { song -> titles[song.id]?.let { song.copy(title = it) } ?: song }
        if (queue.isEmpty()) return
        val currentId = prefs.playbackCurrentId()
        val found = queue.indexOfFirst { it.id == currentId }
        val index = if (found >= 0) found else 0
        // The saved position belongs to the saved song; if it is gone, start the neighbour from 0.
        val position = if (found >= 0) prefs.playbackPosition() else 0L
        skipRecentId = queue[index].id
        c.setMediaItems(queue.map { toMediaItem(it) }, index, position)
        c.shuffleModeEnabled = prefs.playbackShuffle()
        c.repeatMode = prefs.playbackRepeat()
        c.prepare()
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

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.saveThemeMode(mode)
    }

    fun setLanguage(language: AppLanguage) {
        _language.value = language
        prefs.saveLanguage(language)
    }

    fun setProgressStyle(style: ProgressStyle) {
        _progressStyle.value = style
        prefs.saveProgressStyle(style)
    }

    fun renameSong(song: Song, newTitle: String) {
        val t = newTitle.trim()
        val next = _titles.value.toMutableMap()
        if (t.isEmpty()) next.remove(song.id) else next[song.id] = t
        _titles.value = next
        prefs.saveTitles(next)
    }

    /** Called after the files were really deleted: drops them from the queue, favorites and history. */
    fun onSongsDeleted(ids: Set<Long>) {
        controller?.let { c ->
            for (i in c.mediaItemCount - 1 downTo 0) {
                val id = c.getMediaItemAt(i).mediaId.toLongOrNull()
                if (id != null && id in ids) c.removeMediaItem(i)
            }
        }
        val favs = _favorites.value - ids
        _favorites.value = favs
        prefs.saveFavorites(favs)
        val rec = _recents.value.filter { it !in ids }
        _recents.value = rec
        prefs.saveRecents(rec)
        updateCategories { list -> list.map { c -> c.copy(songIds = c.songIds.filter { it !in ids }) } }
        val titles = _titles.value - ids
        _titles.value = titles
        prefs.saveTitles(titles)
        _rawSongs.value = _rawSongs.value.filter { it.id !in ids }
        loadSongs()
    }

    fun toggleFavorite(id: Long) {
        val next = _favorites.value.toMutableSet()
        if (!next.add(id)) next.remove(id)
        _favorites.value = next
        prefs.saveFavorites(next)
    }

    fun hideFolder(path: String) {
        val next = _hiddenFolders.value + path
        _hiddenFolders.value = next
        prefs.saveHiddenFolders(next)
    }

    fun showFolder(path: String) {
        val next = _hiddenFolders.value - path
        _hiddenFolders.value = next
        prefs.saveHiddenFolders(next)
    }

    // ---- Categories --------------------------------------------------------------------------

    private fun updateCategories(change: (List<Category>) -> List<Category>) {
        val next = change(_categories.value)
        _categories.value = next
        prefs.saveCategories(next)
    }

    /** Creates a category and returns its id, or null when the name is blank. */
    fun createCategory(name: String, songIds: List<Long> = emptyList()): Long? {
        val n = name.trim()
        if (n.isEmpty()) return null
        val id = System.currentTimeMillis()
        updateCategories { it + Category(id, n, songIds.distinct()) }
        return id
    }

    fun renameCategory(id: Long, name: String) {
        val n = name.trim()
        if (n.isEmpty()) return
        updateCategories { list -> list.map { if (it.id == id) it.copy(name = n) else it } }
    }

    fun deleteCategory(id: Long) = updateCategories { list -> list.filter { it.id != id } }

    /** Adds the song when it is not in the category yet, removes it when it is. */
    fun toggleSongInCategory(categoryId: Long, songId: Long) {
        updateCategories { list ->
            list.map { c ->
                if (c.id != categoryId) c
                else c.copy(songIds = if (songId in c.songIds) c.songIds - songId else c.songIds + songId)
            }
        }
    }

    fun addSongsToCategory(categoryId: Long, ids: Collection<Long>) {
        updateCategories { list ->
            list.map { c -> if (c.id == categoryId) c.copy(songIds = c.songIds + ids.filter { it !in c.songIds }) else c }
        }
    }

    fun removeSongsFromCategory(categoryId: Long, ids: Collection<Long>) {
        val gone = ids.toSet()
        updateCategories { list ->
            list.map { c -> if (c.id == categoryId) c.copy(songIds = c.songIds.filter { it !in gone }) else c }
        }
    }

    fun addToFavorites(ids: Collection<Long>) {
        val next = _favorites.value + ids
        _favorites.value = next
        prefs.saveFavorites(next)
    }

    /** Replaces the whole song list of a category (used by the multi-select "add songs" dialog). */
    fun setCategorySongs(categoryId: Long, songIds: List<Long>) {
        updateCategories { list -> list.map { if (it.id == categoryId) it.copy(songIds = songIds) else it } }
    }

    private fun addRecent(id: Long) {
        val next = (listOf(id) + _recents.value.filter { it != id }).take(60)
        _recents.value = next
        prefs.saveRecents(next)
    }

    // ---- Playback ----------------------------------------------------------------------------

    private fun toMediaItem(song: Song): MediaItem {
        // The song's own content URI: the notification loader asks the system for its cover (or a placeholder).
        val art: Uri = song.uri
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

    fun playNext(song: Song) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) {
            play(song, listOf(song))
            return
        }
        val key = song.id.toString()
        if (c.currentMediaItem?.mediaId == key) return
        for (i in c.mediaItemCount - 1 downTo 0) {
            if (c.getMediaItemAt(i).mediaId == key) c.removeMediaItem(i)
        }
        c.addMediaItem(c.currentMediaItemIndex + 1, toMediaItem(song))
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
        val real = _durationMs.value.takeIf { it > 0L } ?: durationMs
        val target = (fraction.coerceIn(0f, 1f) * real).toLong().coerceIn(0L, real)
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
        runCatching { getApplication<Application>().getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(netCallback) }
        controller?.removeListener(listener)
        MediaController.releaseFuture(controllerFuture)
        super.onCleared()
    }
}
