package com.asad.breaktime.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asad.breaktime.data.BreakStore
import com.asad.breaktime.data.Tips
import com.asad.breaktime.service.BreakScheduler
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

/**
 * Shown on top of a locked app during a break. There's no way back into the
 * locked app from here: the button and the back gesture both go to the home screen.
 * No ads here, ever (Google Play policy).
 */
class BlockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goHome()
        })

        val store = BreakStore.get(this)
        val appName = intent.getStringExtra(EXTRA_PACKAGE)?.let { pkg ->
            try {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
            } catch (e: Exception) {
                null
            }
        }

        setContent {
            BreakTimeTheme {
                val tip = remember { Tips.random() }
                var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
                LaunchedEffect(Unit) {
                    while (true) {
                        now = System.currentTimeMillis()
                        if (!store.isBreakActive(now)) {
                            BreakScheduler.finishIfDue(this@BlockActivity)
                            finish() // Break is over: let them back into the app.
                            break
                        }
                        delay(1000)
                    }
                }

                val endsAt = store.breakEndsAt
                val total = (endsAt - store.breakStartedAt).coerceAtLeast(1L)
                val remaining = endsAt - now
                val endTime = remember(endsAt) {
                    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(endsAt))
                }
                val onBg = MaterialTheme.colorScheme.onPrimary

                Column(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 28.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "BREAKTIME",
                        color = onBg.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 3.sp,
                    )
                    Spacer(Modifier.weight(1f))

                    Text(
                        if (appName != null) "$appName is paused" else "This app is paused",
                        color = onBg,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "You're taking a break. It unlocks at $endTime.",
                        color = onBg.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(32.dp))
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { (remaining.toFloat() / total).coerceIn(0f, 1f) },
                            modifier = Modifier.size(220.dp),
                            color = onBg,
                            trackColor = onBg.copy(alpha = 0.2f),
                            strokeWidth = 10.dp,
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(formatRemaining(remaining), color = onBg, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                            Text("left", color = onBg.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(32.dp))

                    Surface(
                        color = onBg.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Instead, try this", color = onBg.copy(alpha = 0.7f), style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(4.dp))
                            Text(tip, color = onBg, style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = { goHome() },
                        colors = ButtonDefaults.buttonColors(containerColor = onBg, contentColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    ) {
                        Text("Back to my break", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate() // A different locked app was opened; show its name.
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE = "package"
    }
}
