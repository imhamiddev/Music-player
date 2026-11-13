package com.imhamiddev.musicplayer.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("music_player_prefs", Context.MODE_PRIVATE)

    private fun readIds(key: String): List<Long> =
        sp.getString(key, "").orEmpty().split(',').mapNotNull { it.toLongOrNull() }

    private fun writeIds(key: String, ids: Collection<Long>) {
        sp.edit().putString(key, ids.joinToString(",")).apply()
    }

    fun favorites(): Set<Long> = readIds("favorites").toSet()
    fun saveFavorites(ids: Set<Long>) = writeIds("favorites", ids)

    fun recents(): List<Long> = readIds("recents")
    fun saveRecents(ids: List<Long>) = writeIds("recents", ids)

    fun sortField(): SortField =
        runCatching { SortField.valueOf(sp.getString("sort_field", null) ?: "") }
            .getOrDefault(SortField.DateAdded)

    fun themeMode(): ThemeMode =
        runCatching { ThemeMode.valueOf(sp.getString("theme_mode", null) ?: "") }
            .getOrDefault(ThemeMode.System)

    fun saveThemeMode(mode: ThemeMode) {
        sp.edit().putString("theme_mode", mode.name).apply()
    }

    fun titles(): Map<Long, String> = runCatching {
        val o = JSONObject(sp.getString("titles", "{}") ?: "{}")
        o.keys().asSequence().associate { it.toLong() to o.getString(it) }
    }.getOrDefault(emptyMap())

    fun saveTitles(map: Map<Long, String>) {
        val o = JSONObject()
        map.forEach { (k, v) -> o.put(k.toString(), v) }
        sp.edit().putString("titles", o.toString()).apply()
    }

    fun fsiAsked(): Boolean = sp.getBoolean("fsi_asked", false)
    fun setFsiAsked() { sp.edit().putBoolean("fsi_asked", true).apply() }

    fun sortAsc(): Boolean = sp.getBoolean("sort_asc", false)

    fun saveSort(field: SortField, asc: Boolean) {
        sp.edit().putString("sort_field", field.name).putBoolean("sort_asc", asc).apply()
    }

    // ---- Categories (user-made song groups) ----------------------------------------------------

    fun categories(): List<Category> = runCatching {
        val arr = JSONArray(sp.getString("categories", "[]") ?: "[]")
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val ids = o.getJSONArray("songs")
            Category(o.getLong("id"), o.getString("name"), (0 until ids.length()).map { ids.getLong(it) })
        }
    }.getOrDefault(emptyList())

    fun saveCategories(list: List<Category>) {
        val arr = JSONArray()
        list.forEach { c ->
            arr.put(
                JSONObject()
                    .put("id", c.id)
                    .put("name", c.name)
                    .put("songs", JSONArray(c.songIds)),
            )
        }
        sp.edit().putString("categories", arr.toString()).apply()
    }

    // ---- Sound: speed, equalizer, bass, crossfade ----------------------------------------------

    fun speed(): Float = sp.getFloat("speed", 1f)
    fun saveSpeed(v: Float) { sp.edit().putFloat("speed", v).apply() }

    fun eqEnabled(): Boolean = sp.getBoolean("eq_enabled", false)
    fun saveEqEnabled(on: Boolean) { sp.edit().putBoolean("eq_enabled", on).apply() }

    /** Band levels as fractions of the device's range: -1 = fully down, +1 = fully up. */
    fun eqLevels(): List<Float> =
        sp.getString("eq_levels", "").orEmpty().split(',').mapNotNull { it.toFloatOrNull() }

    fun saveEqLevels(levels: List<Float>) {
        sp.edit().putString("eq_levels", levels.joinToString(",")).apply()
    }

    fun bass(): Float = sp.getFloat("bass", 0f)
    fun saveBass(v: Float) { sp.edit().putFloat("bass", v).apply() }

    fun crossfadeOn(): Boolean = sp.getBoolean("crossfade_on", false)
    fun saveCrossfadeOn(on: Boolean) { sp.edit().putBoolean("crossfade_on", on).apply() }

    fun crossfadeSec(): Int = sp.getInt("crossfade_sec", 4)
    fun saveCrossfadeSec(v: Int) { sp.edit().putInt("crossfade_sec", v).apply() }

    // ---- Language and player look --------------------------------------------------------------

    fun language(): AppLanguage =
        runCatching { AppLanguage.valueOf(sp.getString("language", null) ?: "") }
            .getOrDefault(AppLanguage.English)

    fun saveLanguage(language: AppLanguage) {
        sp.edit().putString("language", language.name).apply()
    }

    fun progressStyle(): ProgressStyle =
        runCatching { ProgressStyle.valueOf(sp.getString("progress_style", null) ?: "") }
            .getOrDefault(ProgressStyle.Waveform)

    fun saveProgressStyle(style: ProgressStyle) {
        sp.edit().putString("progress_style", style.name).apply()
    }

    // ---- Listening statistics (JSON owned by ListenStats) --------------------------------------

    fun statsJson(): String = sp.getString("listen_stats", "{}") ?: "{}"

    fun saveStatsJson(json: String) {
        sp.edit().putString("listen_stats", json).apply()
    }

    // ---- Last playback state, so the app reopens where it was left -----------------------------

    fun playbackQueue(): List<Long> = readIds("pb_queue")

    fun savePlaybackQueue(ids: List<Long>) = writeIds("pb_queue", ids)

    /** Media id of the song that was current when the state was saved. */
    fun playbackCurrentId(): Long = sp.getLong("pb_current", -1L)

    fun playbackPosition(): Long = sp.getLong("pb_position", 0L)

    fun playbackShuffle(): Boolean = sp.getBoolean("pb_shuffle", false)

    /** 0 = off, 1 = one, 2 = all (Player.REPEAT_MODE_*). */
    fun playbackRepeat(): Int = sp.getInt("pb_repeat", 0)

    fun savePlaybackPosition(currentId: Long, positionMs: Long, shuffle: Boolean, repeat: Int) {
        sp.edit()
            .putLong("pb_current", currentId)
            .putLong("pb_position", positionMs)
            .putBoolean("pb_shuffle", shuffle)
            .putInt("pb_repeat", repeat)
            .apply()
    }
}
