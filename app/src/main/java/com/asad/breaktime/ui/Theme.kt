package com.asad.breaktime.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Forest = Color(0xFF2F6B4F)
private val Moss = Color(0xFFA8D5BA)

private val Light = lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6EEDF),
    onPrimaryContainer = Color(0xFF0F2E1F),
    background = Color(0xFFF6F8F3),
    surface = Color(0xFFF6F8F3),
)

private val Dark = darkColorScheme(
    primary = Moss,
    onPrimary = Color(0xFF0F2E1F),
    primaryContainer = Color(0xFF1E4A35),
    onPrimaryContainer = Color(0xFFD6EEDF),
    background = Color(0xFF111512),
    surface = Color(0xFF111512),
)

@Composable
fun BreakTimeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}

/** 125 seconds -> "2:05", 3725 seconds -> "1:02:05". */
fun formatRemaining(millis: Long): String {
    val total = (millis.coerceAtLeast(0) + 999) / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
