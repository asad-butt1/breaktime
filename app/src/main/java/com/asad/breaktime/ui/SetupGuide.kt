package com.asad.breaktime.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Explains why BreakTime needs Accessibility access and walks through turning it on,
 * including Android 13+'s "restricted setting" step for apps not installed from the Play Store.
 */
@Composable
fun SetupGuide(lockOn: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val openAccessibility = {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
    val openAppInfo = {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    if (lockOn) {
        Card(
            modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                Column {
                    Text("App lock is on", fontWeight = FontWeight.SemiBold)
                    Text("You're all set.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        return
    }

    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Turn on app lock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "To lock other apps, BreakTime needs Android's Accessibility permission. " +
                    "It's only used to notice which app you open, so BreakTime can cover it during a break. " +
                    "It never reads your messages, passwords or anything on your screen, and nothing leaves your phone.",
                style = MaterialTheme.typography.bodyMedium,
            )

            if (Build.VERSION.SDK_INT >= 33) {
                Text(
                    "Android shows extra warnings for apps installed outside the Play Store. That's normal while testing. Follow these steps:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Step(
                    1,
                    "Open Accessibility. Tap \"Installed apps\" (or \"Downloaded apps\"), then \"BreakTime app lock\". " +
                        "If it says \"Restricted setting\", tap OK and come back here.",
                    "Open Accessibility", openAccessibility,
                )
                Step(
                    2,
                    "Open BreakTime's app info. Tap ⋮ at the top right, then \"Allow restricted settings\", and confirm with your PIN or fingerprint.",
                    "Open app info", openAppInfo,
                )
                Step(
                    3,
                    "Go back to Accessibility → Installed apps → BreakTime app lock, switch it on, and tap Allow.",
                    "Open Accessibility", openAccessibility,
                )
            } else {
                Step(
                    1,
                    "Open Accessibility, find \"BreakTime app lock\" (sometimes under \"Installed apps\" or \"Downloaded apps\"), switch it on, and tap Allow.",
                    "Open Accessibility", openAccessibility,
                )
            }
            Text(
                "When it's on, this card turns green.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Step(number: Int, text: String, button: String, onClick: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("$number", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text, style = MaterialTheme.typography.bodyMedium)
            FilledTonalButton(onClick = onClick) { Text(button) }
        }
    }
}
