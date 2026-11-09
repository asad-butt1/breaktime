package com.asad.breaktime.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
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
    "com.openai.chatgpt",
    "com.snapchat.android",
    "com.twitter.android", // X
    "com.reddit.frontpage",
    "com.facebook.katana",
    "com.anthropic.claude",
    "com.google.android.apps.bard", // Gemini
    "com.whatsapp",
    "com.pinterest",
    "com.netflix.mediaclient",
)

@Composable
fun AppsScreen(modifier: Modifier = Modifier, onDone: () -> Unit, onPremium: () -> Unit) {
    val context = LocalContext.current
    val store = remember { BreakStore.get(context) }
    val version by store.changes.collectAsState()
    val locked = remember(version) { store.lockedApps }
    val breakActive = remember(version) { store.isBreakActive() }
    val limit = remember(version) { store.appLimit }
    // Order by what was chosen when the screen opened, so rows don't jump while ticking.
    val lockedAtOpen = remember { store.lockedApps }
    var query by remember { mutableStateOf("") }
    var limitHit by remember { mutableStateOf(false) }

    val apps by produceState<List<InstalledApp>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { loadApps(context) }
    }
    val shown = remember(apps, query) {
        apps
            ?.filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }
            ?.sortedWith(
                compareBy<InstalledApp>(
                    { it.packageName !in lockedAtOpen },
                    { SUGGESTED.indexOf(it.packageName).let { i -> if (i < 0) Int.MAX_VALUE else i } },
                    { it.label.lowercase() },
                )
            )
    }

    ChooseAppsContent(
        apps = shown,
        locked = locked,
        query = query,
        onQuery = { query = it },
        note = when {
            breakActive -> "You can add apps during a break, but not remove them."
            limitHit -> "The free plan locks up to ${BreakStore.FREE_APP_LIMIT} apps."
            else -> null
        },
        onToggle = { pkg ->
            if (pkg in locked) {
                if (!breakActive) {
                    store.lockedApps = locked - pkg
                    limitHit = false
                }
            } else if (locked.size >= limit) {
                limitHit = true
            } else {
                store.lockedApps = locked + pkg
            }
        },
        onBack = onDone,
        onDone = onDone,
        onPremium = onPremium,
        modifier = modifier,
        isPremium = limit == Int.MAX_VALUE,
    )
}

@Composable
fun ChooseAppsContent(
    apps: List<InstalledApp>?,
    locked: Set<String>,
    query: String,
    onQuery: (String) -> Unit,
    note: String?,
    onToggle: (String) -> Unit,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onPremium: () -> Unit,
    modifier: Modifier = Modifier,
    isPremium: Boolean = false,
) {
    val limit = BreakStore.FREE_APP_LIMIT
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Palette.DeepPine)
            }
            Spacer(Modifier.weight(1f))
            StatusLabel(if (isPremium) "Premium" else "Free plan")
        }
        ScreenTitle("Choose apps to lock", "Pick the apps you'd like to pause.")
        Spacer(Modifier.height(14.dp))

        // Search
        Row(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(CircleShape)
                .background(Palette.SoftSage.copy(alpha = 0.6f))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, null, tint = Palette.Muted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text("Search installed apps", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQuery,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Palette.DeepPine),
                    cursorBrush = SolidColor(Palette.Forest),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                note ?: if (isPremium) "${locked.size} chosen" else "${locked.size} of $limit chosen on the free plan",
                style = MaterialTheme.typography.labelMedium,
                color = if (note != null) Palette.Danger else Palette.DeepPine,
                modifier = Modifier.weight(1f),
            )
            if (!isPremium) Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(limit) { i ->
                    Box(
                        Modifier
                            .width(10.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(if (i < locked.size) Palette.Forest else Palette.Line)
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .hairline(),
        ) {
            if (apps == null) {
                CircularProgressIndicator(color = Palette.Forest, modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn {
                    itemsIndexed(apps, key = { _, a -> a.packageName }) { i, app ->
                        val checked = app.packageName in locked
                        if (i > 0) HorizontalDivider(color = Palette.Line)
                        AppRow(app, checked) { onToggle(app.packageName) }
                    }
                }
            }
        }

        if (isPremium) Spacer(Modifier.height(12.dp)) else Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onPremium)
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = Palette.Forest, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("Unlock unlimited with Premium", style = MaterialTheme.typography.labelMedium, color = Palette.Forest)
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Palette.Forest, modifier = Modifier.size(16.dp))
        }
        PrimaryButton(
            if (locked.isEmpty()) "Pick at least one app" else "Done (${locked.size} selected)",
            onDone,
            enabled = locked.isNotEmpty(),
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun AppRow(app: InstalledApp, checked: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (checked) Palette.SoftSage.copy(alpha = 0.55f) else Palette.Card)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (app.icon != null) {
            Image(app.icon, null, Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)))
        } else {
            // Fallback when the app's icon can't be loaded: a coloured tile with its first letter.
            val tints = listOf(Palette.Forest, Color(0xFF3F7F9A), Color(0xFFC0814A), Color(0xFF7A6BB0), Color(0xFFB45C6E), Color(0xFF4F8C6F))
            Box(
                Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(tints[Math.floorMod(app.label.hashCode(), tints.size)]),
                contentAlignment = Alignment.Center,
            ) {
                Text(app.label.take(1).uppercase(), style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
        }
        Text(app.label, style = MaterialTheme.typography.titleSmall, color = Palette.DeepPine, modifier = Modifier.weight(1f))
        CheckBox(checked)
    }
}

@Composable
private fun CheckBox(checked: Boolean) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        Modifier
            .size(20.dp)
            .clip(shape)
            .background(if (checked) Palette.Forest else Palette.Card)
            .border(1.5.dp, if (checked) Palette.Forest else Palette.SageDeep, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(Icons.Rounded.Check, null, tint = Palette.Card, modifier = Modifier.size(14.dp))
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
