package com.imhamiddev.musicplayer.playback

import android.content.Context
import com.imhamiddev.musicplayer.data.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the device's equalizer offers: centre frequency of each band (Hz) and the level range (millibel). */
class EqInfo(val freqsHz: List<Int>, val minLevel: Int, val maxLevel: Int)

/**
 * Sound settings shared by the playback service (which applies them) and the UI (which edits them).
 * The app and the service run in the same process, so a plain object with state flows is enough.
 */
object PlaybackFx {
    private var prefs: Prefs? = null

    /** Set by the service while it is alive; changes are pushed to it right away. */
    @Volatile
    var service: PlaybackService? = null

    private val _speed = MutableStateFlow(1f)
    val speed: StateFlow<Float> = _speed.asStateFlow()

    private val _eqEnabled = MutableStateFlow(false)
    val eqEnabled: StateFlow<Boolean> = _eqEnabled.asStateFlow()

    private val _eqLevels = MutableStateFlow<List<Float>>(emptyList())
    val eqLevels: StateFlow<List<Float>> = _eqLevels.asStateFlow()

    private val _bass = MutableStateFlow(0f)
    val bass: StateFlow<Float> = _bass.asStateFlow()

    private val _crossfadeOn = MutableStateFlow(false)
    val crossfadeOn: StateFlow<Boolean> = _crossfadeOn.asStateFlow()

    private val _crossfadeSec = MutableStateFlow(4)
    val crossfadeSec: StateFlow<Int> = _crossfadeSec.asStateFlow()

    /** Wall-clock time (ms) at which the sleep timer stops the music; 0 = no timer. */
    private val _sleepEndsAt = MutableStateFlow(0L)
    val sleepEndsAt: StateFlow<Long> = _sleepEndsAt.asStateFlow()

    /** Sleep timer set to "end of this song". */
    private val _sleepEndOfSong = MutableStateFlow(false)
    val sleepEndOfSong: StateFlow<Boolean> = _sleepEndOfSong.asStateFlow()

    /** Filled in by the service once it could create the effects; null when the device has none. */
    private val _eqInfo = MutableStateFlow<EqInfo?>(null)
    val eqInfo: StateFlow<EqInfo?> = _eqInfo.asStateFlow()

    private val _bassSupported = MutableStateFlow(false)
    val bassSupported: StateFlow<Boolean> = _bassSupported.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (prefs != null) return
        val p = Prefs(context.applicationContext)
        prefs = p
        _speed.value = p.speed()
        _eqEnabled.value = p.eqEnabled()
        _eqLevels.value = p.eqLevels()
        _bass.value = p.bass()
        _crossfadeOn.value = p.crossfadeOn()
        _crossfadeSec.value = p.crossfadeSec()
    }

    fun setSpeed(v: Float) {
        _speed.value = v
        prefs?.saveSpeed(v)
        service?.applyFx()
    }

    fun setEqEnabled(on: Boolean) {
        _eqEnabled.value = on
        prefs?.saveEqEnabled(on)
        service?.applyFx()
    }

    fun setEqLevels(levels: List<Float>) {
        _eqLevels.value = levels
        prefs?.saveEqLevels(levels)
        service?.applyFx()
    }

    fun setBass(v: Float) {
        _bass.value = v
        prefs?.saveBass(v)
        service?.applyFx()
    }

    fun setCrossfadeOn(on: Boolean) {
        _crossfadeOn.value = on
        prefs?.saveCrossfadeOn(on)
        service?.applyFx()
    }

    fun setCrossfadeSec(sec: Int) {
        _crossfadeSec.value = sec
        prefs?.saveCrossfadeSec(sec)
        service?.applyFx()
    }

    fun startSleep(minutes: Int) {
        _sleepEndOfSong.value = false
        _sleepEndsAt.value = System.currentTimeMillis() + minutes * 60_000L
        service?.applyFx()
    }

    fun startSleepAtEndOfSong() {
        _sleepEndsAt.value = 0L
        _sleepEndOfSong.value = true
        service?.applyFx()
    }

    fun cancelSleep() {
        _sleepEndsAt.value = 0L
        _sleepEndOfSong.value = false
        service?.applyFx()
    }

    // ---- Reported by the service ----------------------------------------------------------------

    internal fun publishEffects(eq: EqInfo?, bassSupported: Boolean) {
        _eqInfo.value = eq
        _bassSupported.value = bassSupported
    }

    internal fun clearSleepState() {
        _sleepEndsAt.value = 0L
        _sleepEndOfSong.value = false
    }
}
