package com.imhamiddev.musicplayer.data

import android.content.Context
import java.util.TimeZone
import org.json.JSONArray
import org.json.JSONObject

/** One song's share of a period: how many times it was played and for how long. */
data class SongListen(val songId: Long, val plays: Int, val ms: Long)

data class PeriodStats(val plays: Int, val ms: Long, val songs: List<SongListen>)

data class StatsSnapshot(val today: PeriodStats, val week: PeriodStats, val month: PeriodStats) {
    companion object {
        val Empty = PeriodStats(0, 0L, emptyList()).let { StatsSnapshot(it, it, it) }
    }
}

/**
 * Listening history, kept per calendar day and per song.
 *
 * The playback service feeds it while music plays (so it keeps counting with the app closed) and the
 * stats screen reads from it. Both live in the same process, so one shared object is enough.
 * Only the last [KEEP_DAYS] days are stored, which is all the 30-day view needs.
 */
object ListenStats {
    private const val DAY_MS = 86_400_000L
    private const val KEEP_DAYS = 35L

    private var prefs: Prefs? = null

    // day number -> song id -> [plays, listened ms]
    private val days = HashMap<Long, HashMap<Long, LongArray>>()
    private var dirty = false

    @Synchronized
    fun init(context: Context) {
        if (prefs != null) return
        val p = Prefs(context.applicationContext)
        prefs = p
        runCatching {
            val root = JSONObject(p.statsJson())
            for (dayKey in root.keys()) {
                val songsObj = root.getJSONObject(dayKey)
                val perSong = HashMap<Long, LongArray>()
                for (songKey in songsObj.keys()) {
                    val pair = songsObj.getJSONArray(songKey)
                    perSong[songKey.toLong()] = longArrayOf(pair.getLong(0), pair.getLong(1))
                }
                days[dayKey.toLong()] = perSong
            }
        }
        prune(dayNumber(System.currentTimeMillis()))
    }

    /** Local calendar day number, so "today" follows the user's midnight, not UTC's. */
    private fun dayNumber(now: Long): Long = (now + TimeZone.getDefault().getOffset(now)) / DAY_MS

    private fun prune(today: Long) {
        val iter = days.keys.iterator()
        while (iter.hasNext()) {
            if (iter.next() < today - KEEP_DAYS) {
                iter.remove()
                dirty = true
            }
        }
    }

    /** Adds [ms] of listening to [songId]; [countPlay] marks that this listen just qualified as a play. */
    @Synchronized
    fun add(songId: Long, ms: Long, countPlay: Boolean, now: Long = System.currentTimeMillis()) {
        if (prefs == null || ms <= 0L && !countPlay) return
        val perSong = days.getOrPut(dayNumber(now)) { HashMap() }
        val entry = perSong.getOrPut(songId) { LongArray(2) }
        if (countPlay) entry[0] += 1L
        entry[1] += ms
        dirty = true
    }

    /** Writes pending changes to disk. Cheap when nothing changed. */
    @Synchronized
    fun flush() {
        val p = prefs ?: return
        if (!dirty) return
        prune(dayNumber(System.currentTimeMillis()))
        val root = JSONObject()
        for ((day, perSong) in days) {
            val obj = JSONObject()
            for ((songId, entry) in perSong) {
                obj.put(songId.toString(), JSONArray().put(entry[0]).put(entry[1]))
            }
            root.put(day.toString(), obj)
        }
        p.saveStatsJson(root.toString())
        dirty = false
    }

    /** Today, the last 7 days and the last 30 days (both counted back from today, inclusive). */
    @Synchronized
    fun snapshot(now: Long = System.currentTimeMillis()): StatsSnapshot {
        val today = dayNumber(now)
        return StatsSnapshot(
            today = period(today, today),
            week = period(today - 6L, today),
            month = period(today - 29L, today),
        )
    }

    private fun period(from: Long, to: Long): PeriodStats {
        val totals = HashMap<Long, LongArray>()
        for ((day, perSong) in days) {
            if (day < from || day > to) continue
            for ((songId, entry) in perSong) {
                val sum = totals.getOrPut(songId) { LongArray(2) }
                sum[0] += entry[0]
                sum[1] += entry[1]
            }
        }
        var plays = 0L
        var listened = 0L
        val ranked = ArrayList<SongListen>(totals.size)
        for ((songId, sum) in totals) {
            plays += sum[0]
            listened += sum[1]
            if (sum[0] > 0L) ranked.add(SongListen(songId, sum[0].toInt(), sum[1]))
        }
        ranked.sortWith(compareByDescending<SongListen> { it.plays }.thenByDescending { it.ms })
        return PeriodStats(plays.toInt(), listened, ranked)
    }
}
