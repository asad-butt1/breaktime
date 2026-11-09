package com.asad.breaktime.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.asad.breaktime.data.BreakStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledApp(val packageName: String, val label: String, val icon: ImageBitmap?)

/** Common distracting apps, shown at the top of the list when installed. */
private val SUGGESTED = listOf(
    "com.instagram.android",
    "com.zhiliaoapp.musically", // TikTok
    "com.ss.android.ugc.trill", // TikTok (some regions)
    "com.google.android.youtube",
    "com.facebook.katana",
    "com.twitter.android", // X
    "com.snapchat.android",
    "com.reddit.frontpage",
    "com.openai.chatgpt",
    "com.anthropic.claude",
    "com.google.android.apps.bard", // Gemini
    "com.whatsapp",
    "com.pinterest",
    "com.netflix.mediaclient",
)

@Composable
fun AppsScreen(modifier: Modifier = Modifier, onDone: () -> Unit) {
    val context = LocalContext.current
    val store = remember { BreakStore.get(context) }
    val version by store.changes.collectAsState()
    val locked = remember(version) { store.lockedApps }
    val breakActive = remember(version) { store.isBreakActive() }
    // Order by what was chosen when the screen opened, so rows don't jump while ticking.
    val lockedAtOpen = remember { store.lockedApps }
    var query by remember { mutableStateOf("") }
    var limitHit by remember { mutableStateOf(false) }

    val apps by produceState<List<InstalledApp>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { loadApps(context) }
    }

    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Text(
            "Apps to lock",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
        )
        Text(
            when {
                breakActive -> "You can add apps during a break, but not remove them."
                limitHit -> "The free plan locks up to ${BreakStore.FREE_APP_LIMIT} apps. Premium (coming soon) removes the limit."
                else -> "${locked.size} of ${BreakStore.FREE_APP_LIMIT} chosen on the free plan."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (limitHit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search apps") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        )

        val list = apps
        if (list == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }

        val shown = list
            .filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }
            .sortedWith(
                compareBy<InstalledApp>(
                    { it.packageName !in lockedAtOpen },
                    { SUGGESTED.indexOf(it.packageName).let { i -> if (i < 0) Int.MAX_VALUE else i } },
                    { it.label.lowercase() },
                )
            )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(shown, key = { it.packageName }) { app ->
                val checked = app.packageName in locked
                val toggle: () -> Unit = {
                    if (checked) {
                        if (!breakActive) {
                            store.lockedApps = locked - app.packageName
                            limitHit = false
                        }
                    } else if (locked.size >= BreakStore.FREE_APP_LIMIT) {
                        limitHit = true
                    } else {
                        store.lockedApps = locked + app.packageName
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { toggle() }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val icon = app.icon
                    if (icon != null) {
                        Image(icon, contentDescription = null, modifier = Modifier.size(40.dp))
                    } else {
                        Box(Modifier.size(40.dp))
                    }
                    Text(app.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Checkbox(checked = checked, onCheckedChange = { toggle() })
                }
            }
        }
        Button(
            onClick = onDone,
            enabled = locked.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .height(52.dp),
        ) {
            Text(if (locked.isEmpty()) "Pick at least one app" else "Done (${locked.size} selected)")
        }
    }
}

private fun loadApps(context: Context): List<InstalledApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    @Suppress("DEPRECATION")
    return pm.queryIntentActivities(intent, 0)
        .map { it.activityInfo.packageName to it }
        .distinctBy { it.first }
        .filter { it.first != context.packageName }
        .map { (pkg, info) ->
            val icon = try {
                info.loadIcon(pm).toBitmap(96, 96).asImageBitmap()
            } catch (e: Exception) {
                null
            }
            InstalledApp(pkg, info.loadLabel(pm).toString(), icon)
        }
}
