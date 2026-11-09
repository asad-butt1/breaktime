package com.asad.breaktime.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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

/**
 * Shown on top of a locked app during a break. There's no way back into the
 * locked app from here: the button and the back gesture both go to the home screen.
 * No ads here, ever (Google Play policy).
 */
class BlockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

                Column(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("🌿", fontSize = 64.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (appName != null) "$appName is locked" else "This app is locked",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("You're on a break. Time left:", textAlign = TextAlign.Center)
                    Text(
                        formatRemaining(store.breakEndsAt - now),
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(tip, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(32.dp))
                    Button(onClick = { goHome() }, modifier = Modifier.fillMaxWidth()) {
                        Text("OK, I'll take a break")
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
