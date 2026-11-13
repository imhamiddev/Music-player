package com.imhamiddev.musicplayer.playback

import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.imhamiddev.musicplayer.LockScreenActivity
import com.imhamiddev.musicplayer.MainActivity
import com.imhamiddev.musicplayer.data.ListenStats
import com.imhamiddev.musicplayer.data.Prefs

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var exo: ExoPlayer? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var fxSessionId = C.AUDIO_SESSION_ID_UNSET
    private lateinit var prefs: Prefs
    private val handler = Handler(Looper.getMainLooper())

    // ---- Listening stats + "resume where you left off" ------------------------------------------

    private var lastTickAt = 0L
    private var itemId: Long? = null
    private var itemListenedMs = 0L
    private var itemCounted = false
    private var sinceSaveMs = 0L

    private val tick = object : Runnable {
        override fun run() {
            val player = mediaSession?.player ?: return
            record(player)
            if (player.isPlaying) handler.postDelayed(this, TICK_MS)
        }
    }

    /** Books the time since the last tick as listening time for the current song. */
    private fun record(player: Player) {
        val now = SystemClock.elapsedRealtime()
        val delta = (now - lastTickAt).coerceIn(0L, 3 * TICK_MS)
        lastTickAt = now
        val id = itemId
        if (id == null || delta <= 0L || !player.isPlaying) return
        itemListenedMs += delta
        // A song counts as "played" once you really listened to part of it, not on a skip.
        val dur = player.duration
        val needed = if (dur > 0L) minOf(PLAY_MS, maxOf(MIN_PLAY_MS, dur / 2)) else PLAY_MS
        val countsNow = !itemCounted && itemListenedMs >= needed
        if (countsNow) itemCounted = true
        ListenStats.add(id, delta, countsNow)
        sinceSaveMs += delta
        if (sinceSaveMs >= SAVE_EVERY_MS) {
            sinceSaveMs = 0L
            ListenStats.flush()
            savePosition(player)
        }
    }

    private fun saveQueue(player: Player) {
        if (player.mediaItemCount == 0) return
        val ids = ArrayList<Long>(player.mediaItemCount)
        for (i in 0 until player.mediaItemCount) {
            player.getMediaItemAt(i).mediaId.toLongOrNull()?.let { ids.add(it) }
        }
        if (ids.isNotEmpty()) prefs.savePlaybackQueue(ids)
    }

    private fun savePosition(player: Player) {
        val id = player.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        prefs.savePlaybackPosition(id, player.currentPosition.coerceAtLeast(0L), player.shuffleModeEnabled, player.repeatMode)
    }

    private fun saveState(player: Player) {
        ListenStats.flush()
        saveQueue(player)
        savePosition(player)
    }

    private val stateListener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            rebuildEffects(audioSessionId)
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            // "End of this song" sleep timer: the player paused itself at the end of the song.
            if (!playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM &&
                PlaybackFx.sleepEndOfSong.value
            ) {
                PlaybackFx.clearSleepState()
                applyFx()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            scheduleFx()
            val player = mediaSession?.player ?: return
            handler.removeCallbacks(tick)
            if (isPlaying) {
                lastTickAt = SystemClock.elapsedRealtime()
                handler.postDelayed(tick, TICK_MS)
            } else {
                saveState(player)
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val player = mediaSession?.player ?: return
            // Book what was heard of the previous song before the counters reset.
            if (player.isPlaying) record(player)
            itemId = mediaItem?.mediaId?.toLongOrNull()
            itemListenedMs = 0L
            itemCounted = false
            lastTickAt = SystemClock.elapsedRealtime()
            savePosition(player)
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            mediaSession?.player?.let { saveQueue(it) }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            if (reason == Player.DISCONTINUITY_REASON_SEEK) mediaSession?.player?.let { savePosition(it) }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            mediaSession?.player?.let { savePosition(it) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            mediaSession?.player?.let { savePosition(it) }
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_ON) showLockPlayer()
        }
    }

    /** When the screen wakes on the keyguard while music plays, bring up the full-screen player. */
    private fun showLockPlayer() {
        val player = mediaSession?.player ?: return
        if (!player.isPlaying) return
        val km = getSystemService(KeyguardManager::class.java) ?: return
        if (!km.isKeyguardLocked) return
        val launch = Intent(this, LockScreenActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        // Direct start works when the system allows it; the full-screen notification is the fallback.
        runCatching { startActivity(launch) }
        val nm = getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(LOCK_CHANNEL, "Lock screen player", NotificationManager.IMPORTANCE_HIGH).apply {
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(false)
                },
            )
        }
        val pi = PendingIntent.getActivity(
            this, 1, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val title = player.mediaMetadata.title?.toString() ?: "Music Player"
        val n = NotificationCompat.Builder(this, LOCK_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText("Now playing")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .setAutoCancel(true)
            .setTimeoutAfter(5000)
            .setFullScreenIntent(pi, true)
            .build()
        try {
            nm.notify(LockScreenActivity.LOCK_NOTIFICATION_ID, n)
        } catch (_: SecurityException) {
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        ListenStats.init(this)
        PlaybackFx.init(this)
        // Exact seeking: without this ExoPlayer snaps to the nearest sync point, which is off by seconds on many MP3s.
        val extractors = DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)
        val player = ExoPlayer.Builder(this, DefaultMediaSourceFactory(this, extractors))
            .setSeekParameters(SeekParameters.EXACT)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        exo = player

        player.addListener(stateListener)
        PlaybackFx.service = this
        rebuildEffects(player.audioSessionId)
        applyFx()

        ContextCompat.registerReceiver(
            this, screenReceiver, IntentFilter(Intent.ACTION_SCREEN_ON), ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        setMediaNotificationProvider(ReadableNotificationProvider(this))
        mediaSession = MediaSession.Builder(this, player)
            .setBitmapLoader(ThumbBitmapLoader(this))
            .setSessionActivity(openApp)
            .setCallback(object : MediaSession.Callback {
                override fun onAddMediaItems(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    mediaItems: MutableList<MediaItem>,
                ): ListenableFuture<MutableList<MediaItem>> {
                    val resolved = mediaItems.map { item ->
                        val uri = item.localConfiguration?.uri ?: item.requestMetadata.mediaUri
                        if (uri != null) item.buildUpon().setUri(uri).build() else item
                    }.toMutableList()
                    return Futures.immediateFuture(resolved)
                }
            })
            .build()
    }

    // ---- Sound: speed, equalizer, bass, crossfade, sleep timer -----------------------------------

    private fun releaseEffects() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        equalizer = null
        bassBoost = null
        fxSessionId = C.AUDIO_SESSION_ID_UNSET
    }

    private fun rebuildEffects(sessionId: Int) {
        if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId == fxSessionId) return
        releaseEffects()
        fxSessionId = sessionId
        equalizer = runCatching { Equalizer(0, sessionId) }.getOrNull()
        bassBoost = runCatching { BassBoost(0, sessionId) }.getOrNull()
        val eq = equalizer
        val info = eq?.let { e ->
            runCatching {
                val range = e.bandLevelRange
                EqInfo(
                    freqsHz = (0 until e.numberOfBands.toInt()).map { e.getCenterFreq(it.toShort()) / 1000 },
                    minLevel = range[0].toInt(),
                    maxLevel = range[1].toInt(),
                )
            }.getOrNull()
        }
        val bassOk = bassBoost?.let { b -> runCatching { b.strengthSupported }.getOrDefault(false) } ?: false
        PlaybackFx.publishEffects(info, bassOk)
        applyEffectValues()
    }

    private fun applyEffectValues() {
        val info = PlaybackFx.eqInfo.value
        equalizer?.let { e ->
            runCatching {
                if (info != null) {
                    val levels = PlaybackFx.eqLevels.value
                    for (i in info.freqsHz.indices) {
                        val f = levels.getOrElse(i) { 0f }.coerceIn(-1f, 1f)
                        val mb = if (f >= 0f) f * info.maxLevel else -f * info.minLevel
                        e.setBandLevel(i.toShort(), mb.toInt().coerceIn(info.minLevel, info.maxLevel).toShort())
                    }
                }
                e.setEnabled(PlaybackFx.eqEnabled.value)
            }
        }
        bassBoost?.let { b ->
            runCatching {
                if (b.strengthSupported) {
                    val strength = (PlaybackFx.bass.value * 1000f).toInt().coerceIn(0, 1000)
                    b.setStrength(strength.toShort())
                    b.setEnabled(strength > 0)
                }
            }
        }
    }

    /** Re-reads every sound setting and the sleep timer from [PlaybackFx] and applies them. */
    fun applyFx() {
        val player = exo ?: return
        val speed = PlaybackFx.speed.value
        if (player.playbackParameters.speed != speed) player.setPlaybackSpeed(speed)
        player.setPauseAtEndOfMediaItems(PlaybackFx.sleepEndOfSong.value)
        applyEffectValues()
        scheduleFx()
        applyVolume()
    }

    private fun needsFxTick(): Boolean {
        val player = exo ?: return false
        return PlaybackFx.sleepEndsAt.value > 0L || (PlaybackFx.crossfadeOn.value && player.isPlaying)
    }

    private fun scheduleFx() {
        handler.removeCallbacks(fxTick)
        if (needsFxTick()) handler.postDelayed(fxTick, FX_TICK_MS)
        else applyVolume()
    }

    private val fxTick = object : Runnable {
        override fun run() {
            val player = exo ?: return
            val end = PlaybackFx.sleepEndsAt.value
            if (end > 0L && System.currentTimeMillis() >= end) {
                PlaybackFx.clearSleepState()
                player.pause()
            }
            applyVolume()
            if (needsFxTick()) handler.postDelayed(this, FX_TICK_MS)
        }
    }

    private fun smooth(x: Float): Float {
        val t = x.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /** Volume = the lower of the sleep-timer fade-out and the fade-out/fade-in around song changes. */
    private fun applyVolume() {
        val player = exo ?: return
        var v = 1f
        val end = PlaybackFx.sleepEndsAt.value
        if (end > 0L) {
            val left = end - System.currentTimeMillis()
            v = minOf(v, smooth(left.toFloat() / SLEEP_FADE_MS))
        }
        if (PlaybackFx.crossfadeOn.value) {
            val dur = player.duration
            val pos = player.currentPosition
            if (dur > 0L && dur != C.TIME_UNSET) {
                // Never let the fades eat more than a third of a short song.
                val fade = minOf(PlaybackFx.crossfadeSec.value * 1000L, dur / 3L).coerceAtLeast(1L)
                v = minOf(v, smooth(pos.toFloat() / fade))
                if (player.repeatMode != Player.REPEAT_MODE_ONE) {
                    v = minOf(v, smooth((dur - pos).toFloat() / fade))
                }
            }
        }
        if (player.volume != v) player.volume = v
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player != null) saveState(player)
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        handler.removeCallbacks(fxTick)
        if (PlaybackFx.service === this) PlaybackFx.service = null
        releaseEffects()
        PlaybackFx.publishEffects(null, false)
        runCatching { unregisterReceiver(screenReceiver) }
        mediaSession?.run {
            saveState(player)
            player.removeListener(stateListener)
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private companion object {
        const val LOCK_CHANNEL = "lock_player"
        const val TICK_MS = 1000L
        const val PLAY_MS = 20_000L
        const val MIN_PLAY_MS = 5_000L
        const val SAVE_EVERY_MS = 10_000L
        const val FX_TICK_MS = 100L
        const val SLEEP_FADE_MS = 8_000f
    }
}
