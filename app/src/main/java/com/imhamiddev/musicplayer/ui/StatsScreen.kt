package com.imhamiddev.musicplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imhamiddev.musicplayer.data.PeriodStats
import com.imhamiddev.musicplayer.data.Song
import com.imhamiddev.musicplayer.data.StatsSnapshot
import kotlinx.coroutines.delay

/**
 * How much music was played today, in the last 7 days and the last 30 days, plus the 10 songs
 * played the most in the chosen period.
 */
@Composable
fun StatsScreen(
    stats: StatsSnapshot,
    songs: List<Song>,
    onRefresh: () -> Unit,
    onClose: () -> Unit,
) {
    val s = LocalStrings.current
    var period by rememberSaveable { mutableIntStateOf(0) }

    // Keeps the numbers live while a song plays with this page open.
    LaunchedEffect(Unit) {
        while (true) {
            onRefresh()
            delay(2000)
        }
    }

    val data: PeriodStats = when (period) {
        0 -> stats.today
        1 -> stats.week
        else -> stats.month
    }
    val byId = remember(songs) { songs.associateBy { it.id } }
    // Songs deleted from the device no longer have a title, so they are left out of the ranking.
    val top = remember(data, byId) {
        data.songs.mapNotNull { l -> byId[l.songId]?.let { it to l } }.take(10)
    }
    val maxPlays = top.maxOfOrNull { it.second.plays } ?: 1

    Column(
        Modifier
            .fillMaxSize()
            .background(Ui.Bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconCircle(Icons.Rounded.ChevronLeft, onClose, modifier = Modifier.mirrorInRtl(), size = 48.dp, description = s.close)
            Spacer(Modifier.width(14.dp))
            Text(s.statsTitle, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ui.Ink, letterSpacing = s.ls((-0.8).sp))
        }

        SegmentedTabs(
            labels = listOf(s.periodToday, s.periodWeek, s.periodMonth),
            selected = period,
            onSelect = { period = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
        )

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "summary") { Summary(data) }

            if (top.isEmpty()) {
                item(key = "empty") {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            Modifier.size(72.dp).clip(CircleShape).background(Ui.Tile),
                            contentAlignment = Alignment.Center,
                        ) {
                            androidx.compose.material3.Icon(Icons.Rounded.BarChart, null, tint = Ui.Muted, modifier = Modifier.size(32.dp))
                        }
                        Text(s.statsEmptyTitle, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ui.Ink)
                        Text(s.statsEmptyBody, fontSize = 14.sp, color = Ui.Muted)
                    }
                }
            } else {
                item(key = "top-title") {
                    Text(
                        s.statsTop, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ui.Ink,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                    )
                }
                itemsIndexed(top, key = { _, (song, _) -> song.id }) { index, (song, listen) ->
                    TopRow(index + 1, song, listen.plays, listen.ms, listen.plays.toFloat() / maxPlays)
                }
            }
        }
    }
}

@Composable
private fun Summary(data: PeriodStats) {
    val s = LocalStrings.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile(s.num(data.plays), s.statsSongsPlayed, Modifier.weight(1f), highlighted = true)
        StatTile(s.duration(data.ms), s.statsTime, Modifier.weight(1f), highlighted = false)
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier, highlighted: Boolean) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier
            .clip(shape)
            .background(if (highlighted) Ui.GreenBrush else androidx.compose.ui.graphics.SolidColor(Ui.Tile), shape)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            value, fontSize = 28.sp, fontWeight = FontWeight.Bold,
            color = if (highlighted) Ui.OnAccent else Ui.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Text(
            label, fontSize = 13.sp,
            color = if (highlighted) Ui.OnAccent.copy(alpha = 0.7f) else Ui.Muted,
        )
    }
}

@Composable
private fun TopRow(rank: Int, song: Song, plays: Int, ms: Long, share: Float) {
    val s = LocalStrings.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Ui.Card)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            s.num(rank), fontSize = 16.sp, fontWeight = FontWeight.Bold,
            color = if (rank <= 3) Ui.GreenA else Ui.Muted,
            modifier = Modifier.width(28.dp),
        )
        AlbumArt(song, Modifier.size(46.dp), RoundedCornerShape(14.dp), 140)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(song.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ui.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${s.artist(song.artist)}  ·  ${s.duration(ms)}",
                fontSize = 12.sp, color = Ui.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            // Bar relative to the most played song of the period.
            Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(4.dp).clip(CircleShape).background(Ui.Tile)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(share.coerceIn(0.04f, 1f)).clip(CircleShape).background(Ui.GreenBrush))
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(s.playsCount(plays), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ui.Ink)
    }
}
