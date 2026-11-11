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
import com.android.resources.Density
import com.asad.breaktime.data.PremiumPrices
import com.asad.breaktime.ui.BottomNav
import com.asad.breaktime.ui.BreakTimeTheme
import com.asad.breaktime.ui.HomeContent
import com.asad.breaktime.ui.Palette
import com.asad.breaktime.ui.PremiumContent
import com.asad.breaktime.ui.Tab
import org.junit.Rule
import org.junit.Test

/** Galaxy S24-sized screen (360 x 780 dp) with slightly larger text, where layouts get tight. */
class NarrowPhoneTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(
            screenWidth = 1080,
            screenHeight = 2340,
            xdpi = 480,
            ydpi = 480,
            density = Density.XXHIGH,
            fontScale = 1.15f,
        ),
        showSystemUi = false,
    )

    private fun shot(name: String, content: @Composable () -> Unit) {
        paparazzi.snapshot(name) {
            BreakTimeTheme { Box(Modifier.fillMaxSize().background(Palette.Parchment)) { content() } }
        }
    }

    @Test fun home() = shot("s24_home") {
        Scaffold(containerColor = Palette.Parchment, bottomBar = { BottomNav(Tab.Break) {} }) { p ->
            HomeContent("Good afternoon", 3, 30, {}, {}, 3, true, {}, {}, {}, {}, Modifier.padding(p))
        }
    }

    @Test fun premium() = shot("s24_premium") {
        PremiumContent({}, {}, {}, prices = PremiumPrices("Rs 850.00", "Rs 5,600.00", 0, 0))
    }
}
