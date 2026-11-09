package com.asad.breaktime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.asad.breaktime.data.BreakStore
import com.asad.breaktime.data.Schedule
import com.asad.breaktime.service.BreakScheduler
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** "9:00 PM" (or "21:00", following the phone's format). */
fun formatClock(minuteOfDay: Int): String =
    LocalTime.of(minuteOfDay / 60, minuteOfDay % 60).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

fun describeSchedule(s: Schedule): String =
    "${formatClock(s.startMinute)} – ${formatClock(s.endMinute)} · ${Schedule.describeDays(s.days)}"

/** Scheduled breaks page: list, add, edit. Saving re-arms the alarm right away. */
@Composable
fun SchedulesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { BreakStore.get(context) }
    val version by store.changes.collectAsState()
    val schedules = remember(version) { store.schedules }
    var editing by remember { mutableStateOf<Schedule?>(null) }

    fun save(list: List<Schedule>) {
        store.schedules = list
        BreakScheduler.syncSchedules(context)
    }

    val e = editing
    if (e != null) {
        val isNew = schedules.none { it.id == e.id }
        ScheduleEditContent(
            initial = e,
            isNew = isNew,
            onBack = { editing = null },
            onSave = { updated ->
                save(if (isNew) schedules + updated else schedules.map { if (it.id == updated.id) updated else it })
                editing = null
            },
            onDelete = {
                save(schedules.filter { it.id != e.id })
                editing = null
            },
        )
    } else {
        ScheduleListContent(
            schedules = schedules,
            onBack = onBack,
            onAdd = { editing = Schedule(System.currentTimeMillis(), 21 * 60, 23 * 60, Schedule.EVERY_DAY) },
            onEdit = { editing = it },
            onToggle = { s, on -> save(schedules.map { if (it.id == s.id) it.copy(enabled = on) else it }) },
        )
    }
}

@Composable
fun ScheduleListContent(
    schedules: List<Schedule>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Schedule) -> Unit,
    onToggle: (Schedule, Boolean) -> Unit,
) {
    PageFrame(onBack, "Premium", bottom = { PrimaryButton("Add schedule", onAdd) }) {
        ScreenTitle("Scheduled breaks", "Your chosen apps lock automatically at these times.")
        Spacer(Modifier.height(18.dp))
        if (schedules.isEmpty()) {
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    IconTile(Icons.Rounded.Schedule)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "No schedules yet. Try a phone-free evening, like every day from 9 to 11 pm.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Palette.Muted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            Column(Modifier.fillMaxWidth().hairline()) {
                schedules.forEachIndexed { i, s ->
                    if (i > 0) HorizontalDivider(color = Palette.Line)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(Palette.Card)
                            .clickable { onEdit(s) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${formatClock(s.startMinute)} – ${formatClock(s.endMinute)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (s.enabled) Palette.DeepPine else Palette.Muted,
                            )
                            Text(Schedule.describeDays(s.days), style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
                        }
                        BrandSwitch(s.enabled) { onToggle(s, it) }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "During a scheduled break, BreakTime works just like a normal break. Strict mode applies if it's on.",
            style = MaterialTheme.typography.bodySmall,
            color = Palette.Muted,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditContent(
    initial: Schedule,
    isNew: Boolean,
    onBack: () -> Unit,
    onSave: (Schedule) -> Unit,
    onDelete: () -> Unit,
) {
    var start by remember { mutableStateOf(initial.startMinute) }
    var end by remember { mutableStateOf(initial.endMinute) }
    var days by remember { mutableStateOf(initial.days) }
    var picking by remember { mutableStateOf<String?>(null) } // "start" or "end"
    val draft = initial.copy(startMinute = start, endMinute = end, days = days)

    PageFrame(
        onBack,
        if (isNew) "New schedule" else "Edit schedule",
        bottom = {
            PrimaryButton("Save schedule", { onSave(draft) }, enabled = days.isNotEmpty() && start != end)
            if (!isNew) {
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete schedule", style = MaterialTheme.typography.labelLarge, color = Palette.Danger)
                }
            }
        },
    ) {
        ScreenTitle(if (isNew) "Add a schedule" else "Edit schedule", "Pick when your apps lock and on which days.")
        Spacer(Modifier.height(18.dp))
        Column(Modifier.fillMaxWidth().hairline()) {
            TimeRow("Starts", start) { picking = "start" }
            HorizontalDivider(color = Palette.Line)
            TimeRow(if (end <= start) "Ends (next day)" else "Ends", end) { picking = "end" }
        }
        Spacer(Modifier.height(20.dp))
        Eyebrow("Repeat on")
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            (1..7).forEach { d ->
                val on = d in days
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (on) Palette.Forest else Palette.Card)
                        .border(1.dp, if (on) Palette.Forest else Palette.Line, CircleShape)
                        .clickable { days = if (on) days - d else days + d },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(Schedule.dayLetter(d), style = MaterialTheme.typography.labelLarge, color = if (on) Palette.Card else Palette.DeepPine)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("Every day", onClick = { days = Schedule.EVERY_DAY })
            Pill("Weekdays", onClick = { days = Schedule.WEEKDAYS })
            Pill("Weekends", onClick = { days = Schedule.WEEKENDS })
        }
        Spacer(Modifier.height(18.dp))
        Text(
            when {
                days.isEmpty() -> "Pick at least one day."
                start == end -> "Start and end can't be the same time."
                else -> "Apps lock for ${formatDuration(draft.durationMinutes)}: ${Schedule.describeDays(days)}."
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (days.isEmpty() || start == end) Palette.Danger else Palette.Muted,
        )
    }

    val p = picking
    if (p != null) {
        val current = if (p == "start") start else end
        val state = rememberTimePickerState(initialHour = current / 60, initialMinute = current % 60)
        AlertDialog(
            onDismissRequest = { picking = null },
            containerColor = Palette.Parchment,
            title = { Text(if (p == "start") "Start time" else "End time", color = Palette.DeepPine) },
            text = {
                TimePicker(
                    state = state,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = Palette.SoftSage,
                        selectorColor = Palette.Forest,
                        timeSelectorSelectedContainerColor = Palette.SoftSage,
                        timeSelectorSelectedContentColor = Palette.DeepPine,
                        periodSelectorSelectedContainerColor = Palette.SoftSage,
                        periodSelectorSelectedContentColor = Palette.DeepPine,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val m = state.hour * 60 + state.minute
                    if (p == "start") start = m else end = m
                    picking = null
                }) { Text("OK", color = Palette.Forest) }
            },
            dismissButton = {
                TextButton(onClick = { picking = null }) { Text("Cancel", color = Palette.Muted) }
            },
        )
    }
}

@Composable
private fun TimeRow(label: String, minute: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Palette.Card)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = Palette.DeepPine, modifier = Modifier.weight(1f))
        Text(
            formatClock(minute),
            style = MaterialTheme.typography.titleMedium,
            color = Palette.Forest,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Palette.SoftSage.copy(alpha = 0.7f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
fun BrandSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Palette.Card,
            checkedTrackColor = Palette.Forest,
            uncheckedThumbColor = Palette.SageDeep,
            uncheckedTrackColor = Palette.Card,
            uncheckedBorderColor = Palette.SageDeep,
        ),
    )
}

/** Full-screen page: back header, scrolling body and buttons pinned to the bottom. */
@Composable
private fun PageFrame(onBack: () -> Unit, label: String, bottom: @Composable () -> Unit, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Parchment)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Palette.DeepPine) }
            Spacer(Modifier.weight(1f))
            StatusLabel(label)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            content()
            Spacer(Modifier.height(16.dp))
        }
        bottom()
        Spacer(Modifier.height(12.dp))
    }
}
