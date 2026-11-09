package com.asad.breaktime.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.asad.breaktime.data.BreakRecord
import com.asad.breaktime.data.BreakStore
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

data class DayBar(val label: String, val minutes: Int, val isToday: Boolean)

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember { BreakStore.get(context) }
    val version by store.changes.collectAsState()
    val history = remember(version) { store.history }
    val streak = remember(version) { store.currentStreak() }
    val completed = history.filter { it.completed }
    val week = remember(version) { lastSevenDays(history) }
    val today = LocalDate.now()
    val range = "${today.minusDays(6).format(DateTimeFormatter.ofPattern("MMM d"))}–${today.dayOfMonth}"

    ProgressContent(
        streak = streak,
        breaks = completed.size,
        hoursOffline = completed.sumOf { it.plannedMinutes } / 60.0,
        week = week,
        rangeLabel = range,
        history = history,
        modifier = modifier,
    )
}

private fun lastSevenDays(history: List<BreakRecord>): List<DayBar> {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()
    val byDay = history.filter { it.completed }
        .groupBy { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }
        .mapValues { (_, v) -> v.sumOf { it.plannedMinutes } }
    return (6 downTo 0).map { back ->
        val d = today.minusDays(back.toLong())
        DayBar(d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), byDay[d] ?: 0, back == 0)
    }
}

@Composable
fun ProgressContent(
    streak: Int,
    breaks: Int,
    hoursOffline: Double,
    week: List<DayBar>,
    rangeLabel: String,
    history: List<BreakRecord>,
    modifier: Modifier = Modifier,
) {
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }
    val zone = remember { ZoneId.systemDefault() }

    LazyColumn(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        item { BrandHeader() }
        item {
            Spacer(Modifier.height(8.dp))
            ScreenTitle("Your progress", "A little time back adds up.")
            Spacer(Modifier.height(16.dp))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(Icons.Rounded.WbSunny, "$streak", "Day streak", Palette.SoftSage, Modifier.weight(1f))
                StatCard(Icons.Rounded.AccessTime, "$breaks", "Breaks taken", Palette.MorningSky, Modifier.weight(1f))
                StatCard(Icons.Rounded.Spa, "%.1f".format(hoursOffline), "Hours offline", Palette.Sunlight.copy(alpha = 0.7f), Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }
        item {
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Time offline", style = MaterialTheme.typography.titleMedium, color = Palette.DeepPine, modifier = Modifier.weight(1f))
                        Text("$rangeLabel · minutes", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
                    }
                    Spacer(Modifier.height(14.dp))
                    WeekChart(week)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
        item {
            Text("Break history", style = MaterialTheme.typography.titleMedium, color = Palette.DeepPine)
            Spacer(Modifier.height(6.dp))
        }
        if (history.isEmpty()) {
            item {
                Text(
                    "No breaks yet. Your first one starts on the Break tab.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.Muted,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        }
        items(history) { r ->
            val date = Instant.ofEpochMilli(r.startedAt).atZone(zone).toLocalDate()
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (r.completed) Palette.SoftSage else Palette.Sunlight.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(if (r.completed) Icons.Rounded.Check else Icons.Rounded.Pause, null, tint = Palette.Forest, modifier = Modifier.size(15.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(date.format(dateFmt), style = MaterialTheme.typography.titleSmall, color = Palette.DeepPine)
                    Text(formatDuration(r.plannedMinutes), style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
                }
                if (r.completed) {
                    Pill("Completed")
                } else {
                    Pill("Ended early", container = Palette.Sunlight.copy(alpha = 0.6f), content = Palette.DeepPine)
                }
            }
            HorizontalDivider(color = Palette.Line)
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun StatCard(icon: ImageVector, value: String, label: String, bg: Color, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .padding(12.dp),
    ) {
        Icon(icon, null, tint = Palette.Forest, modifier = Modifier.size(16.dp))
        Spacer(Modifier.height(10.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, color = Palette.DeepPine)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Palette.Muted, maxLines = 1)
    }
}

@Composable
private fun WeekChart(week: List<DayBar>) {
    val max = (week.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(30)
    Row(
        Modifier
            .fillMaxWidth()
            .height(120.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        week.forEach { d ->
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                val frac = (d.minutes.toFloat() / max).coerceIn(0.04f, 1f)
                Box(
                    Modifier
                        .weight(1f, fill = false)
                        .fillMaxHeight(frac)
                        .padding(horizontal = 9.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                        .background(if (d.isToday) Palette.Forest else Palette.SageDeep.copy(alpha = 0.6f))
                )
                Spacer(Modifier.height(6.dp))
                Text(d.label, style = MaterialTheme.typography.labelMedium, color = Palette.Muted)
            }
        }
    }
}
