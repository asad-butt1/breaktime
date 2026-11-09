package com.asad.breaktime.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asad.breaktime.service.BlockerService
import kotlinx.coroutines.delay

private const val PAGES = 3

/** First-run welcome: what the app does, how it works, then setting up app lock. */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = page > 0) { page-- }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
    ) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            when (page) {
                0 -> Intro(
                    emoji = "🌿",
                    title = "Welcome to BreakTime",
                    body = "Social media and AI apps are built to keep you scrolling. " +
                        "BreakTime helps you put the phone down and get some real time back.",
                )
                1 -> HowItWorks()
                else -> SetupPage()
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(PAGES) { i ->
                Box(
                    Modifier
                        .padding(4.dp)
                        .size(if (i == page) 10.dp else 8.dp)
                        .background(
                            if (i == page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            CircleShape,
                        )
                )
            }
        }

        Button(
            onClick = { if (page < PAGES - 1) page++ else onFinish() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text(if (page < PAGES - 1) "Next" else "Get started", fontSize = 18.sp)
        }
        if (page == PAGES - 1) {
            TextButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("I'll do this later") }
        } else {
            TextButton(onClick = { page = PAGES - 1 }, modifier = Modifier.fillMaxWidth()) { Text("Skip") }
        }
    }
}

@Composable
private fun Intro(emoji: String, title: String, body: String) {
    Text(emoji, fontSize = 72.sp)
    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    Text(body, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
}

@Composable
private fun HowItWorks() {
    Text("How it works", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    HowRow("📱", "Pick your distracting apps", "Instagram, TikTok, YouTube, ChatGPT, whatever pulls you in.")
    HowRow("⏱️", "Set a break timer", "Pick a quick preset or set your own time.")
    HowRow("🔒", "Those apps stay locked", "If you open one, BreakTime gently reminds you you're on a break.")
    HowRow("🚶", "Go outside, breathe, reset", "You'll get ideas for what to do instead.")
    HowRow("🔓", "Apps unlock when time's up", "We'll send you a notification. Build a daily streak.")
}

@Composable
private fun HowRow(emoji: String, title: String, body: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(emoji, fontSize = 28.sp)
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SetupPage() {
    val context = LocalContext.current
    var lockOn by remember { mutableStateOf(BlockerService.isEnabled(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            lockOn = BlockerService.isEnabled(context)
            delay(1000)
        }
    }
    Text("One last step", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
    SetupGuide(lockOn = lockOn)
}
