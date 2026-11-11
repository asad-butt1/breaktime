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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.asad.breaktime.data.PremiumPrices

// ---------------------------------------------------------------- Break complete

@Composable
fun BreakCompleteContent(minutes: Int, streak: Int, onAnother: () -> Unit, onDone: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Parchment)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        BrandHeader { StatusLabel("Break finished", icon = Icons.Rounded.Check) }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))
            Landscape(
                Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(24.dp)),
                sunX = 0.6f,
                sunY = 0.32f,
            ) {
                Text(
                    "${formatDuration(minutes).uppercase()} FOR YOU",
                    style = MaterialTheme.typography.labelSmall,
                    color = Palette.DeepPine,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.85f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Break complete.\nNice work!",
                style = MaterialTheme.typography.headlineLarge,
                color = Palette.DeepPine,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text("Your apps are unlocked.", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
            Spacer(Modifier.height(20.dp))
            if (streak > 0) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Palette.Sunlight.copy(alpha = 0.5f))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IconTile(Icons.Rounded.WbSunny, container = Palette.Card, size = 34.dp)
                    Column {
                        Text(
                            if (streak == 1) "1-day streak" else "$streak-day streak",
                            style = MaterialTheme.typography.titleSmall,
                            color = Palette.DeepPine,
                        )
                        Text(
                            if (streak == 1) "Your streak has started. Come back tomorrow." else "Your streak grew by one day.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Palette.Muted,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Start another break", onAnother)
        Spacer(Modifier.height(10.dp))
        SecondaryButton("Done", onDone)
        Spacer(Modifier.height(12.dp))
    }
}

// ---------------------------------------------------------------- Premium

private val PREMIUM_BENEFITS = listOf(
    "Unlimited locked apps",
    "Scheduled breaks, e.g. every evening",
    "Strict mode: breaks can't be ended early",
    "No ads",
    "Support a small independent app",
)

@Composable
fun PremiumContent(
    onClose: () -> Unit,
    onStartTrial: (yearly: Boolean) -> Unit,
    onRestore: () -> Unit,
    prices: PremiumPrices? = null,
    isPremium: Boolean = false,
    onPrivacyChoices: (() -> Unit)? = null,
    onRedeemCode: ((String) -> Boolean)? = null,
) {
    var showCode by remember { mutableStateOf(false) }
    var yearly by remember { mutableStateOf(true) }
    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Parchment)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Close", tint = Palette.DeepPine) }
            Spacer(Modifier.weight(1f))
            StatusLabel("BreakTime Premium", icon = Icons.Rounded.AutoAwesome)
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))
            Landscape(
                Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(22.dp)),
                sunX = 0.75f,
                sunY = 0.35f,
                showPath = false,
            )
            Spacer(Modifier.height(20.dp))
            Text("Make more time\nfor yourself", style = MaterialTheme.typography.headlineLarge, color = Palette.DeepPine)
            Spacer(Modifier.height(6.dp))
            Text("Build a break routine that fits your day.", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PREMIUM_BENEFITS.forEach { b ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Palette.SoftSage),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.Check, null, tint = Palette.Forest, modifier = Modifier.size(13.dp)) }
                        Text(b, style = MaterialTheme.typography.titleSmall, color = Palette.DeepPine)
                    }
                }
            }
            if (isPremium) {
                Spacer(Modifier.height(20.dp))
                Pill("You're on Premium. Thank you!", icon = Icons.Rounded.Check, container = Palette.SoftSage, content = Palette.Forest)
            }
            Spacer(Modifier.height(16.dp))
        }
        // Plans sit outside the scrolling area so both are always visible next to the button.
        if (!isPremium) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PlanTile("Yearly", prices?.yearly ?: "$19.99", "/ year", bestValue = true, selected = yearly, modifier = Modifier.weight(1f)) { yearly = true }
                PlanTile("Monthly", prices?.monthly ?: "$2.99", "/ month", bestValue = false, selected = !yearly, modifier = Modifier.weight(1f)) { yearly = false }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Renews automatically. Cancel anytime in Google Play.",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
        }
        val trialDays = if (yearly) prices?.yearlyTrialDays ?: 0 else prices?.monthlyTrialDays ?: 0
        if (isPremium) {
            PrimaryButton("Done", onClose)
        } else {
            PrimaryButton(if (trialDays > 0) "Start $trialDays-day free trial" else "Get Premium", { onStartTrial(yearly) })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = onRestore) {
                Text("Restore purchase", style = MaterialTheme.typography.labelLarge, color = Palette.DeepPine)
            }
            if (onRedeemCode != null && !isPremium) {
                TextButton(onClick = { showCode = true }) {
                    Text("Have a code?", style = MaterialTheme.typography.labelMedium, color = Palette.Muted)
                }
            }
        }
        if (showCode && onRedeemCode != null) {
            RedeemCodeDialog(onRedeem = onRedeemCode, onDismiss = { showCode = false })
        }
        if (onPrivacyChoices != null) {
            TextButton(onClick = onPrivacyChoices, modifier = Modifier.fillMaxWidth()) {
                Text("Ad privacy choices", style = MaterialTheme.typography.labelMedium, color = Palette.Muted)
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun PlanTile(
    name: String,
    price: String,
    period: String,
    bestValue: Boolean,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .clip(shape)
            .background(if (selected) Palette.SoftSage.copy(alpha = 0.5f) else Palette.Card)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) Palette.Forest else Palette.Line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, if (selected) Palette.Forest else Palette.SageDeep, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Box(Modifier.size(9.dp).clip(CircleShape).background(Palette.Forest))
            }
            Spacer(Modifier.width(8.dp))
            Text(name, style = MaterialTheme.typography.titleSmall, color = Palette.DeepPine)
        }
        Text(price, style = MaterialTheme.typography.titleLarge, color = Palette.DeepPine)
        Text(period, style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
        if (bestValue) {
            Pill("BEST VALUE", container = Palette.Sunlight, content = Palette.DeepPine)
        } else {
            // Keeps both tiles the same height.
            Pill(" ", container = Color.Transparent, content = Color.Transparent)
        }
    }
}

@Composable
private fun RedeemCodeDialog(onRedeem: (String) -> Boolean, onDismiss: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Parchment,
        title = { Text("Enter code", color = Palette.DeepPine) },
        text = {
            Column {
                androidx.compose.material3.OutlinedTextField(
                    value = code,
                    onValueChange = { code = it; error = false },
                    singleLine = true,
                    placeholder = { Text("Code") },
                    isError = error,
                )
                if (error) {
                    Spacer(Modifier.height(6.dp))
                    Text("That code isn't valid.", style = MaterialTheme.typography.bodySmall, color = Palette.Danger)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (onRedeem(code)) onDismiss() else error = true }) {
                Text("Unlock", color = Palette.Forest)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Palette.Muted) } },
    )
}
