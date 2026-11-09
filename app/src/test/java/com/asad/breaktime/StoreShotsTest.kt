package com.asad.breaktime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.Snapshot
import app.cash.paparazzi.SnapshotHandler
import com.asad.breaktime.data.BreakRecord
import com.asad.breaktime.ui.BottomNav
import com.asad.breaktime.ui.BreakActiveContent
import com.asad.breaktime.ui.BreakCompleteContent
import com.asad.breaktime.ui.BreakTimeTheme
import com.asad.breaktime.ui.ChooseAppsContent
import com.asad.breaktime.ui.DayBar
import com.asad.breaktime.ui.HomeContent
import com.asad.breaktime.ui.InstalledApp
import com.asad.breaktime.ui.LockedAppContent
import com.asad.breaktime.ui.Palette
import com.asad.breaktime.ui.ProgressContent
import com.asad.breaktime.ui.Tab
import com.asad.breaktime.ui.WelcomePage
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.lang.reflect.Proxy

/**
 * Writes full-resolution PNGs (Paparazzi's normal output is scaled down).
 * Uses reflection because java.awt isn't on the Android unit-test compile classpath.
 */
private class FullResHandler(private val dir: File) : SnapshotHandler {
    override fun newFrameHandler(snapshot: Snapshot, frameCount: Int, fps: Int): SnapshotHandler.FrameHandler {
        val out = File(dir, "${snapshot.name}.png")
        return Proxy.newProxyInstance(
            javaClass.classLoader,
            arrayOf(SnapshotHandler.FrameHandler::class.java),
        ) { _, method, args ->
            if (method.name == "handle") {
                dir.mkdirs()
                Class.forName("javax.imageio.ImageIO")
                    .getMethod("write", Class.forName("java.awt.image.RenderedImage"), String::class.java, File::class.java)
                    .invoke(null, args[0], "png", out)
            }
            null
        } as SnapshotHandler.FrameHandler
    }
    override fun close() = Unit
}

/**
 * Play Store screenshots with realistic, brand-free sample data.
 * Output: PNGs in app/build/store-shots at 1080 x 2340.
 */
class StoreShotsTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        showSystemUi = false,
        snapshotHandler = FullResHandler(File("build/store-shots")),
    )

    private val now = 1_762_700_000_000L
    private val endsAt = now + 24 * 60_000L + 18_000L

    private fun shot(name: String, content: @Composable () -> Unit) = paparazzi.snapshot(name) {
        BreakTimeTheme { Box(Modifier.fillMaxSize().background(Palette.Parchment)) { content() } }
    }

    @Composable
    private fun Tabs(tab: Tab, content: @Composable (Modifier) -> Unit) {
        Scaffold(containerColor = Palette.Parchment, bottomBar = { BottomNav(tab) {} }) { p -> content(Modifier.padding(p)) }
    }

    @Test fun home() = shot("store_1_home") {
        Tabs(Tab.Break) { m -> HomeContent("Good afternoon", 5, 45, {}, {}, 3, true, {}, {}, {}, {}, m) }
    }

    @Test fun locked() = shot("store_2_locked") {
        LockedAppContent("Short videos", null, endsAt, 0.55f, endsAt - now, "Go for a 10-minute walk. No phone needed.", {})
    }

    @Test fun active() = shot("store_3_active") {
        Tabs(Tab.Break) { m ->
            BreakActiveContent(endsAt, 0.55f, endsAt - now, "Step outside and find three things you can hear.", {}, m)
        }
    }

    @Test fun apps() = shot("store_4_apps") {
        val apps = listOf("Photo feed", "Short videos", "AI chat", "Video app", "News feed", "Messages", "Games", "Shopping")
            .map { InstalledApp("pkg.$it", it, null) }
        Tabs(Tab.Apps) { m ->
            ChooseAppsContent(apps, setOf("pkg.Photo feed", "pkg.Short videos", "pkg.AI chat"), "", {}, null, {}, {}, {}, {}, m)
        }
    }

    @Test fun progress() = shot("store_5_progress") {
        val week = listOf("T" to 30, "W" to 45, "T" to 20, "F" to 60, "S" to 90, "S" to 45, "M" to 75)
            .mapIndexed { i, (d, m) -> DayBar(d, m, i == 6) }
        val day = 86_400_000L
        val hist = listOf(
            BreakRecord(now, 45, true),
            BreakRecord(now - day, 30, true),
            BreakRecord(now - 2 * day, 60, true),
            BreakRecord(now - 3 * day, 15, false),
            BreakRecord(now - 3 * day - 7_200_000, 30, true),
        )
        Tabs(Tab.Progress) { m -> ProgressContent(5, 18, 9.5, week, "Nov 3–9", hist, m) }
    }

    @Test fun complete() = shot("store_6_complete") { BreakCompleteContent(45, 5, {}, {}) }
    @Test fun welcome() = shot("store_7_welcome") { WelcomePage({}, {}) }
}
