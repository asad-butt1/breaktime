package com.asad.breaktime.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
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
        val pkg = intent.getStringExtra(EXTRA_PACKAGE)
        val info = pkg?.let { runCatching { packageManager.getApplicationInfo(it, 0) }.getOrNull() }
        val appName = info?.let { packageManager.getApplicationLabel(it).toString() }
        val appIcon = info?.let { runCatching { packageManager.getApplicationIcon(it).toBitmap(128, 128).asImageBitmap() }.getOrNull() }

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
                LockedAppContent(
                    appName = appName,
                    appIcon = appIcon,
                    endsAt = endsAt,
                    fraction = (endsAt - now).toFloat() / (endsAt - store.breakStartedAt).coerceAtLeast(1L),
                    remaining = (endsAt - now).coerceIn(0L, endsAt - store.breakStartedAt),
                    tip = tip,
                    onBack = { goHome() },
                )
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

@Composable
fun LockedAppContent(
    appName: String?,
    appIcon: ImageBitmap?,
    endsAt: Long,
    fraction: Float,
    remaining: Long,
    tip: String,
    onBack: () -> Unit,
) {
    val endTime = remember(endsAt) { DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(endsAt)) }
    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Parchment)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        BrandHeader { StatusLabel("App paused", icon = Icons.Rounded.Lock) }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))
            if (appIcon != null) {
                Image(
                    appIcon, null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
                Spacer(Modifier.height(14.dp))
            }
            ScreenTitle(
                if (appName != null) "$appName is paused" else "This app is paused",
                "It unlocks at $endTime.",
                centered = true,
            )
            Spacer(Modifier.height(20.dp))
            CountdownRing(fraction, remaining)
            Spacer(Modifier.height(24.dp))
            TipCard("Instead, try this", tip)
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Back to my break", onBack)
        Spacer(Modifier.height(12.dp))
    }
}
