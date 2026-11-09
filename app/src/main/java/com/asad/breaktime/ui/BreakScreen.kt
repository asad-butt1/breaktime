package com.asad.breaktime.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.asad.breaktime.data.Ads
import com.asad.breaktime.data.BreakStore
import com.asad.breaktime.data.Tips
import com.asad.breaktime.service.BlockerService
import com.asad.breaktime.service.BreakScheduler
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.time.LocalTime
import java.util.Date

private val PRESETS = listOf(15, 30, 45, 60, 120)
private const val MAX_MINUTES = 12 * 60

/** Break tab: pick a length and start, or watch the current break. */
@Composable
fun BreakScreen(
    modifier: Modifier = Modifier,
    onPickApps: () -> Unit,
    onSetup: () -> Unit,
    onPremium: () -> Unit,
) {
    val context = LocalContext.current
    val store = remember { BreakStore.get(context) }
    val version by store.changes.collectAsState()

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            BreakScheduler.finishIfDue(context)
            delay(1000)
        }
    }
    NotificationPermissionRequest()

    val lockOn = remember(now) { BlockerService.isEnabled(context) }
    val active = remember(version, now) { store.isBreakActive(now) }
    val lockedCount = remember(version) { store.lockedApps.size }

    if (active) {
        var confirm by remember { mutableStateOf(false) }
        val tip = remember(store.breakStartedAt) { Tips.random() }
        BreakActiveContent(
            endsAt = store.breakEndsAt,
            fraction = ((store.breakEndsAt - now).toFloat() / (store.breakEndsAt - store.breakStartedAt).coerceAtLeast(1L)),
            remaining = store.breakEndsAt - now,
            tip = tip,
            onEndEarly = { confirm = true },
            modifier = modifier,
        )
        if (confirm) {
            EndEarlyDialog(
                onEnd = {
                    confirm = false
                    BreakScheduler.endEarly(context)
                },
                onKeep = { confirm = false },
            )
        }
    } else {
        var minutes by rememberSaveable { mutableIntStateOf(30) }
        var showCustom by remember { mutableStateOf(false) }
        HomeContent(
            greeting = greeting(),
            minutes = minutes,
            onMinutesChange = { minutes = it },
            onCustom = { showCustom = true },
            lockedCount = lockedCount,
            lockOn = lockOn,
            onSetup = onSetup,
            onPickApps = onPickApps,
            onPremium = onPremium,
            onStart = {
                val start = { BreakScheduler.startBreak(context, minutes) }
                val activity = context.findActivity()
                if (activity != null) Ads.showBreakStartAd(activity) { start() } else start()
            },
            modifier = modifier,
        )
        if (showCustom) {
            CustomTimeSheet(
                initialMinutes = minutes,
                onDismiss = { showCustom = false },
                onSet = {
                    minutes = it
                    showCustom = false
                },
            )
        }
    }
}

private fun greeting(): String {
    val h = LocalTime.now().hour
    return when {
        h < 5 -> "Good evening"
        h < 12 -> "Good morning"
        h < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}

// ---------------------------------------------------------------- Start a break

@Composable
fun HomeContent(
    greeting: String,
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    onCustom: () -> Unit,
    lockedCount: Int,
    lockOn: Boolean,
    onSetup: () -> Unit,
    onPickApps: () -> Unit,
    onPremium: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        BrandHeader { Pill("Premium", icon = Icons.Rounded.AutoAwesome, onClick = onPremium) }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))
            ScreenTitle(greeting, "Take a little time for yourself.")
            Spacer(Modifier.height(16.dp))

            if (!lockOn) {
                Notice(
                    text = "Turn on app lock so BreakTime can pause your apps.",
                    action = "Set up",
                    onClick = onSetup,
                )
                Spacer(Modifier.height(12.dp))
            } else if (lockedCount == 0) {
                Notice(
                    text = "Choose which apps to pause during a break.",
                    action = "Choose apps",
                    onClick = onPickApps,
                )
                Spacer(Modifier.height(12.dp))
            }

            DialTimePicker(
                minutes = minutes,
                onMinutesChange = onMinutesChange,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(20.dp))

            val chips = PRESETS.map { it to formatDuration(it) }
            val rows = (chips + (-1 to "Custom")).chunked(3)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { (value, label) ->
                            val isCustom = value == -1
                            val selected = if (isCustom) minutes !in PRESETS else minutes == value
                            TimeChip(
                                label = if (isCustom && selected) formatDuration(minutes) else label,
                                selected = selected,
                                leadingPlus = isCustom && !selected,
                                onClick = { if (isCustom) onCustom() else onMinutesChange(value) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Lock, null, tint = Palette.Muted, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                when (lockedCount) {
                    0 -> "No apps chosen yet"
                    1 -> "1 app will be locked"
                    else -> "$lockedCount apps will be locked"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Palette.Muted,
            )
        }
        PrimaryButton(
            "Start ${formatDuration(minutes)} break",
            onStart,
            enabled = lockOn && lockedCount > 0,
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun TimeChip(label: String, selected: Boolean, leadingPlus: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Row(
        modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(if (selected) Palette.SoftSage else Palette.Card)
            .border(1.dp, if (selected) Palette.SageDeep else Palette.Line, CircleShape)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingPlus) {
            Icon(Icons.Rounded.Add, null, tint = Palette.DeepPine, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = Palette.DeepPine, maxLines = 1)
    }
}

@Composable
private fun Notice(text: String, action: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.Sunlight.copy(alpha = 0.45f))
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = Palette.DeepPine, modifier = Modifier.weight(1f))
        Pill(action, container = Palette.Card, onClick = onClick)
    }
}

// ---------------------------------------------------------------- Custom time

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTimeSheet(initialMinutes: Int, onDismiss: () -> Unit, onSet: (Int) -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Palette.Parchment,
    ) {
        CustomTimeContent(initialMinutes, onDismiss, onSet)
    }
}

@Composable
fun CustomTimeContent(initialMinutes: Int, onCancel: () -> Unit, onSet: (Int) -> Unit) {
    var hours by remember { mutableStateOf((initialMinutes / 60).toString()) }
    var mins by remember { mutableStateOf((initialMinutes % 60).toString()) }
    val total = (hours.toIntOrNull() ?: 0) * 60 + (mins.toIntOrNull() ?: 0)
    val valid = total in 1..MAX_MINUTES

    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
        Text("Custom time", style = MaterialTheme.typography.headlineSmall, color = Palette.DeepPine)
        Text("How long would you like to take a break?", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            TimeField("Hours", hours, { v -> hours = v.filter { it.isDigit() }.take(2) }, Modifier.weight(1f))
            Text(":", style = MaterialTheme.typography.headlineSmall, color = Palette.DeepPine, modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp))
            TimeField("Minutes", mins, { v -> mins = v.filter { it.isDigit() }.take(3) }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (valid) "Break length: ${formatDuration(total)}" else "Choose between 1 minute and 12 hours.",
            style = MaterialTheme.typography.bodySmall,
            color = if (valid) Palette.Muted else Palette.Danger,
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton("Cancel", onCancel, Modifier.weight(1f))
            PrimaryButton("Set", { onSet(total) }, Modifier.weight(1f), enabled = valid)
        }
    }
}

@Composable
private fun TimeField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Palette.DeepPine)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineSmall.copy(color = Palette.DeepPine),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Palette.Forest,
                unfocusedBorderColor = Palette.Line,
                focusedContainerColor = Palette.Card,
                unfocusedContainerColor = Palette.Card,
                cursorColor = Palette.Forest,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ---------------------------------------------------------------- Break in progress

@Composable
fun BreakActiveContent(
    endsAt: Long,
    fraction: Float,
    remaining: Long,
    tip: String,
    onEndEarly: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val endTime = remember(endsAt) { DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(endsAt)) }
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        BrandHeader { Pill("Break active", icon = Icons.Rounded.Pause) }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))
            ScreenTitle("You're on a break", "Your apps are paused until $endTime.", centered = true)
            Spacer(Modifier.height(24.dp))
            CountdownRing(fraction, remaining)
            Spacer(Modifier.height(28.dp))
            TipCard("Try this", tip)
        }
        GhostButton("End break early", onEndEarly, Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(12.dp))
    }
}

/** Sage card with a suggestion on the left and a small landscape on the right. */
@Composable
fun TipCard(label: String, tip: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.SoftSage),
    ) {
        Column(
            Modifier
                .weight(1f)
                .padding(14.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Eyebrow(label, color = Palette.Forest)
            Spacer(Modifier.height(4.dp))
            Text(tip, style = MaterialTheme.typography.titleSmall, color = Palette.DeepPine, maxLines = 3)
        }
        Landscape(
            Modifier
                .width(96.dp)
                .fillMaxHeight(),
            sunX = 0.55f,
            sunY = 0.3f,
            showBirds = false,
        )
    }
}

// ---------------------------------------------------------------- End early

@Composable
fun EndEarlyDialog(onEnd: () -> Unit, onKeep: () -> Unit) {
    Dialog(onDismissRequest = onKeep) { EndEarlyCard(onEnd, onKeep) }
}

@Composable
fun EndEarlyCard(onEnd: () -> Unit, onKeep: () -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = Palette.Card) {
        Column(Modifier.padding(20.dp)) {
            IconTile(Icons.Rounded.Pause, container = Palette.Sunlight.copy(alpha = 0.6f), tint = Palette.DeepPine, size = 36.dp)
            Spacer(Modifier.height(14.dp))
            Text("End your break early?", style = MaterialTheme.typography.headlineSmall, color = Palette.DeepPine)
            Spacer(Modifier.height(6.dp))
            Text(
                "Your apps will unlock now, and this break won't count toward your streak.",
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.Muted,
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                androidx.compose.material3.OutlinedButton(
                    onClick = onEnd,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Palette.Line),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                ) { Text("End break", style = MaterialTheme.typography.labelLarge, color = Palette.DeepPine) }
                PrimaryButton("Keep going", onKeep, Modifier.weight(1f).height(48.dp))
            }
        }
    }
}

// ---------------------------------------------------------------- Helpers

@Composable
private fun NotificationPermissionRequest() {
    if (Build.VERSION.SDK_INT < 33) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
