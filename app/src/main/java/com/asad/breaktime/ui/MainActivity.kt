package com.asad.breaktime.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.asad.breaktime.data.Billing
import com.asad.breaktime.data.BreakStore
import com.asad.breaktime.service.BreakScheduler

private enum class Overlay { None, Setup, Premium }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Palette.Parchment.toArgb(), Palette.DeepPine.toArgb()),
            navigationBarStyle = SystemBarStyle.light(Palette.Parchment.toArgb(), Palette.DeepPine.toArgb()),
        )
        setContent {
            BreakTimeTheme {
                val store = remember { BreakStore.get(this) }
                var onboarded by remember { mutableStateOf(store.onboardingDone) }
                if (!onboarded) {
                    OnboardingScreen(onFinish = {
                        store.onboardingDone = true
                        onboarded = true
                    })
                } else {
                    MainApp(store)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        BreakScheduler.finishIfDue(this)
        Billing.refresh(this)
    }
}

@Composable
private fun MainApp(store: BreakStore) {
    val context = LocalContext.current
    val version by store.changes.collectAsState()
    var tab by rememberSaveable { mutableStateOf(Tab.Break) }
    var overlay by rememberSaveable { mutableStateOf(Overlay.None) }
    val celebrate = remember(version) { store.pendingCelebration }

    BackHandler(enabled = overlay != Overlay.None) { overlay = Overlay.None }

    when {
        celebrate > 0 -> {
            val streak = remember(version) { store.currentStreak() }
            BreakCompleteContent(
                minutes = celebrate,
                streak = streak,
                onAnother = {
                    store.clearCelebration()
                    tab = Tab.Break
                },
                onDone = { store.clearCelebration() },
            )
        }
        overlay == Overlay.Setup -> {
            val setup = rememberSetupState()
            SetupPage(
                lockOn = setup.lockOn,
                restricted = setup.restricted,
                onOpenAccessibility = setup.openAccessibility,
                onOpenAppInfo = setup.openAppInfo,
                onBack = { overlay = Overlay.None },
                onDone = { overlay = Overlay.None },
                onLater = { overlay = Overlay.None },
                inOnboarding = false,
                consented = setup.consented,
                onConsent = setup.consent,
            )
        }
        overlay == Overlay.Premium -> {
            val prices by Billing.prices.collectAsState()
            val premium = remember(version) { store.isPremium }
            PremiumContent(
                onClose = { overlay = Overlay.None },
                onStartTrial = { yearly ->
                    val activity = context.findActivity()
                    if (activity == null || !Billing.buy(activity, yearly)) {
                        Billing.refresh(context)
                        Toast.makeText(context, "Premium isn't available right now. Please try again in a moment.", Toast.LENGTH_SHORT).show()
                    }
                },
                onRestore = {
                    Billing.restore(context) { found ->
                        val msg = when (found) {
                            true -> "Premium restored. Welcome back!"
                            false -> "No Premium purchase found on this Google account."
                            null -> "Couldn't reach Google Play. Please try again."
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                },
                prices = prices,
                isPremium = premium,
            )
        }
        else -> Scaffold(
            containerColor = Palette.Parchment,
            bottomBar = { BottomNav(tab) { tab = it } },
        ) { padding ->
            val modifier = Modifier
                .fillMaxSize()
                .background(Palette.Parchment)
                .padding(padding)
            when (tab) {
                Tab.Break -> BreakScreen(
                    modifier,
                    onPickApps = { tab = Tab.Apps },
                    onSetup = { overlay = Overlay.Setup },
                    onPremium = { overlay = Overlay.Premium },
                )
                Tab.Apps -> AppsScreen(
                    modifier,
                    onDone = { tab = Tab.Break },
                    onPremium = { overlay = Overlay.Premium },
                )
                Tab.Progress -> HistoryScreen(modifier)
            }
        }
    }
}
