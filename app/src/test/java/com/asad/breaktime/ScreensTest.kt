package com.asad.breaktime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.asad.breaktime.data.BreakRecord
import com.asad.breaktime.ui.BottomNav
import com.asad.breaktime.ui.BreakActiveContent
import com.asad.breaktime.ui.BreakCompleteContent
import com.asad.breaktime.ui.BreakTimeTheme
import com.asad.breaktime.ui.ChooseAppsContent
import com.asad.breaktime.ui.CustomTimeContent
import com.asad.breaktime.ui.DayBar
import com.asad.breaktime.ui.EndEarlyCard
import com.asad.breaktime.ui.HomeContent
import com.asad.breaktime.ui.HowItWorksPage
import com.asad.breaktime.ui.InstalledApp
import com.asad.breaktime.ui.LockedAppContent
import com.asad.breaktime.ui.Palette
import com.asad.breaktime.ui.PremiumContent
import com.asad.breaktime.ui.ProgressContent
import com.asad.breaktime.ui.SetupPage
import com.asad.breaktime.ui.Tab
import com.asad.breaktime.ui.WelcomePage
import org.junit.Rule
import org.junit.Test

/**
 * Renders every screen to a PNG so the design can be checked without a phone.
 * Run: ./gradlew :app:recordPaparazziDebug  → images in app/src/test/snapshots/images
 */
class ScreensTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5, showSystemUi = false)

    private val now = 1_762_700_000_000L
    private val endsAt = now + 18 * 60_000L + 42_000L

    private fun shot(name: String, content: @Composable () -> Unit) {
        paparazzi.snapshot(name) {
            BreakTimeTheme {
                Box(Modifier.fillMaxSize().background(Palette.Parchment)) { content() }
            }
        }
    }

    @Composable
    private fun Tabs(tab: Tab, content: @Composable (Modifier) -> Unit) {
        Scaffold(containerColor = Palette.Parchment, bottomBar = { BottomNav(tab) {} }) { p ->
            content(Modifier.padding(p))
        }
    }

    @Test fun s01_welcome() = shot("01_welcome") { WelcomePage({}, {}) }
    @Test fun s02_how() = shot("02_how") { HowItWorksPage({}, {}) }
    @Test fun s03_setup() = shot("03_setup") { SetupPage(false, true, {}, {}, {}, {}, {}) }
    @Test fun s03a_disclosure() = shot("03a_disclosure") { SetupPage(false, true, {}, {}, {}, {}, {}, consented = false) }
    @Test fun s03b_setup_on() = shot("03b_setup_on") { SetupPage(true, true, {}, {}, {}, {}, {}) }

    @Test fun s04_home() = shot("04_home") {
        Tabs(Tab.Break) { m ->
            HomeContent("Good afternoon", 3, 30, {}, {}, 3, true, {}, {}, {}, {}, m)
        }
    }

    @Test fun s04b_home_setup() = shot("04b_home_needs_setup") {
        Tabs(Tab.Break) { m ->
            HomeContent("Good morning", 0, 10, {}, {}, 0, false, {}, {}, {}, {}, m)
        }
    }

    @Test fun s05_custom() = shot("05_custom") {
        Box(Modifier.fillMaxSize().background(Color(0x66000000))) {
            Column(Modifier.align(Alignment.BottomCenter).background(Palette.Parchment).padding(top = 24.dp)) {
                CustomTimeContent(75, {}, {})
            }
        }
    }

    @Test fun s06_active() = shot("06_active") {
        Tabs(Tab.Break) { m ->
            BreakActiveContent(endsAt, 0.62f, endsAt - now, "Go for a 10-minute walk. No phone needed.", {}, m)
        }
    }

    @Test fun s07_end_early() = shot("07_end_early") {
        Box(Modifier.fillMaxSize().background(Color(0x66000000)).padding(24.dp), contentAlignment = Alignment.Center) {
            EndEarlyCard({}, {})
        }
    }

    @Test fun s08_locked() = shot("08_locked") {
        LockedAppContent("Instagram", null, endsAt, 0.62f, endsAt - now, "Take three slow breaths. Relax your shoulders as you breathe out.", {})
    }

    @Test fun s09_apps() = shot("09_apps") {
        val apps = listOf("Instagram", "TikTok", "YouTube", "ChatGPT", "Snapchat", "X", "Reddit", "WhatsApp")
            .map { InstalledApp("pkg.$it", it, null) }
        Tabs(Tab.Apps) { m ->
            ChooseAppsContent(apps, setOf("pkg.Instagram", "pkg.ChatGPT"), "", {}, null, {}, {}, {}, {}, m)
        }
    }

    @Test fun s10_progress() = shot("10_progress") {
        val week = listOf("T" to 20, "W" to 45, "T" to 15, "F" to 30, "S" to 60, "S" to 40, "M" to 75)
            .mapIndexed { i, (d, m) -> DayBar(d, m, i == 6) }
        val day = 86_400_000L
        val hist = listOf(
            BreakRecord(now, 30, true),
            BreakRecord(now - day, 45, true),
            BreakRecord(now - 2 * day, 15, false),
            BreakRecord(now - 2 * day - 3_600_000, 60, true),
        )
        Tabs(Tab.Progress) { m -> ProgressContent(3, 12, 6.5, week, "Nov 3–9", hist, m) }
    }

    @Test fun s11_complete() = shot("11_complete") { BreakCompleteContent(30, 3, {}, {}) }
    @Test fun s12_premium() = shot("12_premium") { PremiumContent({}, {}, {}) }
}
