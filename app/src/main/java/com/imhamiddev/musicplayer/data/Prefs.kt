package com.imhamiddev.musicplayer.data

import android.content.Context

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

    fun sortAsc(): Boolean = sp.getBoolean("sort_asc", false)

    fun saveSort(field: SortField, asc: Boolean) {
        sp.edit().putString("sort_field", field.name).putBoolean("sort_asc", asc).apply()
    }
}
