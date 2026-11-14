package com.imhamiddev.musicplayer.data

/** A user-made group of songs, like "Gym" or "Chill". Songs are kept in the order they were added. */
data class Category(val id: Long, val name: String, val songIds: List<Long>, val icon: String = "music")
