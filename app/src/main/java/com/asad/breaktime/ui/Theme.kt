package com.asad.breaktime.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.asad.breaktime.R

/** BreakTime palette (from the design). */
object Palette {
    val Parchment = Color(0xFFFAFBF6)
    val DeepPine = Color(0xFF173D34)
    val Forest = Color(0xFF245D4C)
    val SoftSage = Color(0xFFE2EFE4)
    val MorningSky = Color(0xFFDCEEF0)
    val Sunlight = Color(0xFFF9DFA0)

    val Card = Color(0xFFFFFFFF)
    val Muted = Color(0xFF66776F)
    val Line = Color(0xFFE4E9E2)
    val SageDeep = Color(0xFF9CC3AA)
    val Danger = Color(0xFFB4483C)
    val Flame = Color(0xFFE2702F)
}

@OptIn(ExperimentalTextApi::class)
private fun jakarta(weight: Int) = Font(
    R.font.plus_jakarta_sans,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Jakarta = FontFamily(jakarta(400), jakarta(500), jakarta(600), jakarta(700), jakarta(800))

private fun style(size: Int, weight: Int, line: Int, spacing: Double = 0.0) = TextStyle(
    fontFamily = Jakarta,
    fontSize = size.sp,
    fontWeight = FontWeight(weight),
    lineHeight = line.sp,
    letterSpacing = spacing.em,
)

private val AppTypography = Typography(
    displayLarge = style(56, 700, 60, -0.03),
    displayMedium = style(44, 700, 48, -0.03),
    headlineLarge = style(32, 700, 38, -0.02),
    headlineMedium = style(28, 700, 34, -0.02),
    headlineSmall = style(22, 700, 28, -0.01),
    titleLarge = style(20, 700, 26, -0.01),
    titleMedium = style(16, 700, 22),
    titleSmall = style(14, 700, 20),
    bodyLarge = style(16, 400, 24),
    bodyMedium = style(14, 400, 20),
    bodySmall = style(12, 400, 17),
    labelLarge = style(15, 700, 20),
    labelMedium = style(12, 600, 16),
    labelSmall = style(10, 700, 14, 0.12),
)

private val Scheme = lightColorScheme(
    primary = Palette.Forest,
    onPrimary = Color.White,
    primaryContainer = Palette.SoftSage,
    onPrimaryContainer = Palette.DeepPine,
    secondaryContainer = Palette.SoftSage,
    onSecondaryContainer = Palette.DeepPine,
    background = Palette.Parchment,
    onBackground = Palette.DeepPine,
    surface = Palette.Parchment,
    onSurface = Palette.DeepPine,
    surfaceVariant = Palette.SoftSage,
    onSurfaceVariant = Palette.Muted,
    surfaceContainerLow = Palette.Card,
    surfaceContainer = Palette.Card,
    surfaceContainerHigh = Palette.Card,
    outline = Palette.Line,
    outlineVariant = Palette.Line,
    error = Palette.Danger,
)

@Composable
fun BreakTimeTheme(content: @Composable () -> Unit) {
    // The design is light-only for now.
    MaterialTheme(colorScheme = Scheme, typography = AppTypography, content = content)
}

/** 125 seconds -> "2:05", 3725 seconds -> "1:02:05". */
fun formatRemaining(millis: Long): String {
    val total = (millis.coerceAtLeast(0) + 999) / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** 45 -> "45 min", 60 -> "1 hr", 90 -> "1 hr 30 min". */
fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "$m min"
        m == 0 -> "$h hr"
        else -> "$h hr $m min"
    }
}
