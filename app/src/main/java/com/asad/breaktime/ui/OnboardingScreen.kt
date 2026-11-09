package com.asad.breaktime.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.asad.breaktime.service.BlockerService
import kotlinx.coroutines.delay

private const val PAGES = 3

/** First run: Welcome → How it works → Set up app lock. */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = page > 0) { page-- }
    val setup = rememberSetupState()

    when (page) {
        0 -> WelcomePage(onNext = { page = 1 }, onSkip = { page = 2 })
        1 -> HowItWorksPage(onBack = { page = 0 }, onNext = { page = 2 })
        else -> SetupPage(
            lockOn = setup.lockOn,
            restricted = setup.restricted,
            onOpenAccessibility = setup.openAccessibility,
            onOpenAppInfo = setup.openAppInfo,
            onBack = { page = 1 },
            onDone = onFinish,
            onLater = onFinish,
        )
    }
}

@Composable
private fun OnboardingFrame(
    pageIndex: Int?,
    onBack: (() -> Unit)?,
    bottom: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Parchment)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        val pill: @Composable () -> Unit = { if (pageIndex != null) Pill("0${pageIndex + 1} / 0$PAGES") }
        if (onBack == null) {
            BrandHeader { pill() }
        } else {
            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.padding(start = 0.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Palette.DeepPine)
                }
                Spacer(Modifier.weight(1f))
                pill()
            }
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp),
            content = content,
        )
        if (pageIndex != null) {
            PageDots(PAGES, pageIndex, Modifier.align(Alignment.CenterHorizontally).padding(vertical = 16.dp))
        } else {
            Spacer(Modifier.height(16.dp))
        }
        bottom()
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun WelcomePage(onNext: () -> Unit, onSkip: () -> Unit) {
    OnboardingFrame(
        pageIndex = 0,
        onBack = null,
        bottom = {
            PrimaryButton("Next", onNext, showArrow = true)
            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text("Skip", style = MaterialTheme.typography.labelLarge, color = Palette.DeepPine)
            }
        },
    ) {
        Landscape(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp)),
        ) {
            Text(
                "A LITTLE ROOM TO BREATHE",
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
        Spacer(Modifier.height(28.dp))
        Text(
            "Welcome to\nBreakTime",
            style = MaterialTheme.typography.headlineLarge,
            color = Palette.DeepPine,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Get a little time back from endless scrolling.",
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private data class HowStep(val icon: ImageVector, val tile: Color, val title: String, val body: String)

private val HOW_STEPS = listOf(
    HowStep(Icons.Rounded.GridView, Palette.SoftSage, "Pick your distracting apps", "Choose the feeds and AI apps you want to pause."),
    HowStep(Icons.Rounded.AccessTime, Palette.MorningSky, "Set a break timer", "Decide how much time to take for yourself."),
    HowStep(Icons.Rounded.Lock, Palette.SoftSage, "Your chosen apps stay locked", "They're paused for the length of your break."),
    HowStep(Icons.Rounded.Spa, Palette.Sunlight, "Go outside, breathe or reset", "Take a walk, stretch or enjoy a quiet moment."),
    HowStep(Icons.Rounded.WbSunny, Palette.SoftSage, "Unlock and build your streak", "Your apps unlock when the timer ends."),
)

@Composable
fun HowItWorksPage(onBack: () -> Unit, onNext: () -> Unit) {
    OnboardingFrame(
        pageIndex = 1,
        onBack = onBack,
        bottom = { PrimaryButton("Next", onNext, showArrow = true) },
    ) {
        ScreenTitle("How it works", "Five small steps to a real break.")
        Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HOW_STEPS.forEachIndexed { i, step ->
                AppCard(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        IconTile(step.icon, container = step.tile)
                        Column(Modifier.weight(1f)) {
                            Text(step.title, style = MaterialTheme.typography.titleSmall, color = Palette.DeepPine)
                            Text(step.body, style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
                        }
                        Text("0${i + 1}", style = MaterialTheme.typography.labelMedium, color = Palette.Muted)
                    }
                }
            }
        }
    }
}

@Composable
fun SetupPage(
    lockOn: Boolean,
    restricted: Boolean,
    onOpenAccessibility: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onLater: () -> Unit,
    inOnboarding: Boolean = true,
) {
    OnboardingFrame(
        pageIndex = if (inOnboarding) 2 else null,
        onBack = onBack,
        bottom = {
            PrimaryButton(if (inOnboarding) "Get started" else "Done", onDone)
            if (!lockOn && inOnboarding) {
                TextButton(onClick = onLater, modifier = Modifier.fillMaxWidth()) {
                    Text("I'll do this later", style = MaterialTheme.typography.labelLarge, color = Palette.DeepPine)
                }
            }
        },
    ) {
        SetupContent(lockOn, restricted, onOpenAccessibility, onOpenAppInfo)
    }
}

/** The explanation + numbered steps for turning on app lock. */
@Composable
fun SetupContent(
    lockOn: Boolean,
    restricted: Boolean,
    onOpenAccessibility: () -> Unit,
    onOpenAppInfo: () -> Unit,
) {
    ScreenTitle("Set up app lock", "Allow Android Accessibility so BreakTime can tell which app you open.")
    Spacer(Modifier.height(16.dp))
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.MorningSky)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Rounded.VerifiedUser, null, tint = Palette.Forest, modifier = Modifier.size(18.dp))
        Text(
            "BreakTime never reads your screen, messages or passwords. Your app activity stays on this phone.",
            style = MaterialTheme.typography.bodySmall,
            color = Palette.DeepPine,
        )
    }
    Spacer(Modifier.height(12.dp))

    val steps = if (restricted) {
        listOf(
            Triple("Find BreakTime", "Open Accessibility → Installed apps → BreakTime app lock. If it says \"Restricted setting\", tap OK.", "Open Accessibility" to onOpenAccessibility),
            Triple("Allow access if Android blocks it", "In App info, tap ⋮ at the top, then Allow restricted settings.", "Open app info" to onOpenAppInfo),
            Triple("Turn on the app lock", "Go back to Accessibility and switch on BreakTime app lock.", "Open Accessibility" to onOpenAccessibility),
        )
    } else {
        listOf(
            Triple("Find BreakTime", "Open Accessibility → Installed apps → BreakTime app lock.", "Open Accessibility" to onOpenAccessibility),
            Triple("Turn on the app lock", "Switch it on, then tap Allow.", "Open Accessibility" to onOpenAccessibility),
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        steps.forEachIndexed { i, (title, body, action) ->
            AppCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Palette.SoftSage),
                        contentAlignment = Alignment.Center,
                    ) { Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = Palette.Forest) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(title, style = MaterialTheme.typography.titleSmall, color = Palette.DeepPine)
                        Text(body, style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
                        Spacer(Modifier.height(4.dp))
                        LinkPill(action.first, action.second)
                    }
                }
            }
        }
        StatusBar(lockOn)
    }
}

@Composable
private fun LinkPill(text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(Palette.SoftSage)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = Palette.Forest)
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Palette.Forest, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun StatusBar(lockOn: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (lockOn) Palette.SoftSage else Palette.Sunlight.copy(alpha = 0.5f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            if (lockOn) Icons.Rounded.Check else Icons.Rounded.Lock, null,
            tint = Palette.Forest, modifier = Modifier.size(16.dp),
        )
        Text(
            if (lockOn) "App lock is on" else "App lock is off",
            style = MaterialTheme.typography.labelMedium,
            color = Palette.DeepPine,
        )
    }
}

// ---------------------------------------------------------------- Setup state helpers

class SetupState(
    val lockOn: Boolean,
    val restricted: Boolean,
    val openAccessibility: () -> Unit,
    val openAppInfo: () -> Unit,
)

/** Polls whether app lock is on, and works out whether Android will show the restricted-setting step. */
@Composable
fun rememberSetupState(): SetupState {
    val context = LocalContext.current
    var lockOn by remember { mutableStateOf(BlockerService.isEnabled(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            lockOn = BlockerService.isEnabled(context)
            delay(1000)
        }
    }
    val restricted = remember { Build.VERSION.SDK_INT >= 33 && !installedFromPlayStore(context) }
    return SetupState(
        lockOn = lockOn,
        restricted = restricted,
        openAccessibility = {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        },
        openAppInfo = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        },
    )
}

private fun installedFromPlayStore(context: Context): Boolean = try {
    val installer = if (Build.VERSION.SDK_INT >= 30) {
        context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getInstallerPackageName(context.packageName)
    }
    installer == "com.android.vending"
} catch (e: Exception) {
    false
}
