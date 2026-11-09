package com.asad.breaktime.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asad.breaktime.data.BreakStore
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember { BreakStore.get(context) }
    val version by store.changes.collectAsState()
    val history = remember(version) { store.history }
    val streak = remember(version) { store.currentStreak() }
    val completed = history.filter { it.completed }
    val minutesAway = completed.sumOf { it.plannedMinutes }
    val fmt = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }

    LazyColumn(
        modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Your progress",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 20.dp),
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat("Day streak", "$streak", Modifier.weight(1f))
                Stat("Breaks", "${completed.size}", Modifier.weight(1f))
                Stat("Hours offline", "%.1f".format(minutesAway / 60.0), Modifier.weight(1f))
            }
        }
        if (history.isEmpty()) {
            item { Text("No breaks yet. Your first one starts on the Break tab.") }
        }
        items(history) { r ->
            Column {
                Text(fmt.format(Date(r.startedAt)), style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${r.plannedMinutes} min · " + if (r.completed) "Completed" else "Ended early",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (r.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(Modifier.padding(top = 12.dp))
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}
