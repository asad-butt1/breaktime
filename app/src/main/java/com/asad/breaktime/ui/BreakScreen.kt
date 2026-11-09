package com.asad.breaktime.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.asad.breaktime.data.Ads
import com.asad.breaktime.data.BreakStore
import com.asad.breaktime.data.Tips
import com.asad.breaktime.service.BlockerService
import com.asad.breaktime.service.BreakScheduler
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val PRESETS = listOf(15, 30, 45, 60, 120)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BreakScreen(modifier: Modifier = Modifier, onPickApps: () -> Unit) {
    val context = LocalContext.current
    val store = remember { BreakStore.get(context) }
    val version by store.changes.collectAsState()

    // Tick once a second so the countdown and permission checks stay fresh.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            BreakScheduler.finishIfDue(context)
            delay(1000)
        }
    }

    val lockOn = remember(now) { BlockerService.isEnabled(context) }
    val active = remember(version, now) { store.isBreakActive(now) }
    val lockedCount = remember(version) { store.lockedApps.size }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("BreakTime", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)

        if (!lockOn) LockPermissionCard()
        NotificationPermissionRequest()

        if (active) {
            ActiveBreak(remaining = store.breakEndsAt - now)
        } else {
            StartBreak(
                lockedCount = lockedCount,
                lockOn = lockOn,
                onPickApps = onPickApps,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StartBreak(lockedCount: Int, lockOn: Boolean, onPickApps: () -> Unit) {
    val context = LocalContext.current
    var minutes by rememberSaveable { mutableIntStateOf(30) }

    Text(
        "Take a break from your phone. The apps you pick stay locked until the timer ends.",
        style = MaterialTheme.typography.bodyLarge,
    )

    if (lockedCount == 0) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pick the apps to lock first", fontWeight = FontWeight.SemiBold)
                Text("For example Instagram, TikTok, YouTube or ChatGPT.")
                Button(onClick = onPickApps) { Text("Choose apps") }
            }
        }
        return
    }

    Text("How long?", style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PRESETS.forEach { p ->
            FilterChip(
                selected = minutes == p,
                onClick = { minutes = p },
                label = { Text(if (p < 60) "$p min" else "${p / 60} hr") },
            )
        }
    }
    Text("Or choose exactly: $minutes minutes", style = MaterialTheme.typography.bodyMedium)
    Slider(
        value = minutes.toFloat(),
        onValueChange = { minutes = (it / 5).roundToInt() * 5 },
        valueRange = 5f..240f,
    )

    Button(
        onClick = {
            val activity = context.findActivity()
            val start = { BreakScheduler.startBreak(context, minutes) }
            if (activity != null) Ads.showBreakStartAd(activity) { start() } else start()
        },
        enabled = lockOn,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Text("Start break", fontSize = 18.sp)
    }
    Text(
        if (lockOn) "$lockedCount app${if (lockedCount == 1) "" else "s"} will be locked."
        else "Turn on app lock above to start a break.",
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun ActiveBreak(remaining: Long) {
    val context = LocalContext.current
    val tip = remember { Tips.random() }
    var confirm by remember { mutableStateOf(false) }

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("You're on a break", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(formatRemaining(remaining), fontSize = 56.sp, fontWeight = FontWeight.Bold)
            Text("left", style = MaterialTheme.typography.bodyMedium)
        }
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Try this", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(tip, style = MaterialTheme.typography.bodyLarge)
        }
    }
    Text(
        "Put your phone down. We'll let you know when your apps are unlocked.",
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedButton(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth()) {
        Text("End break early")
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("End your break early?") },
            text = { Text("Your apps will unlock now and this break won't count toward your streak.") },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    BreakScheduler.endEarly(context)
                }) { Text("End break") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Keep going") } },
        )
    }
}

@Composable
private fun LockPermissionCard() {
    val context = LocalContext.current
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Turn on app lock", fontWeight = FontWeight.SemiBold)
            Text(
                "To lock apps, BreakTime needs Accessibility access. It only checks which app is open " +
                    "and never reads your screen. Tap below, find BreakTime, and switch it on."
            )
            Button(onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }) { Text("Open settings") }
        }
    }
}

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

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
