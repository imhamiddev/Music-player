package com.imhamiddev.musicplayer.playback

import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
        override fun onIsPlayingChanged(isPlaying: Boolean) {
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

        player.addListener(stateListener)

        ContextCompat.registerReceiver(
            this, screenReceiver, IntentFilter(Intent.ACTION_SCREEN_ON), ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        mediaSession = MediaSession.Builder(this, player)
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

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player != null) saveState(player)
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
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
    }
}
