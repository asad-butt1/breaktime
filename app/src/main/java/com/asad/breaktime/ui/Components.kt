package com.asad.breaktime.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.asad.breaktime.R
import kotlin.math.cos
import kotlin.math.sin

/** Logo + "BreakTime" on the left, optional pill on the right. */
@Composable
fun BrandHeader(modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painterResource(R.drawable.ic_logo),
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(7.dp)),
        )
        Text(
            "BreakTime",
            style = MaterialTheme.typography.titleSmall,
            color = Palette.DeepPine,
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f),
        )
        trailing()
    }
}

/** Small rounded label like "Premium", "Break active", "01 / 03". */
@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    container: Color = Palette.SoftSage,
    content: Color = Palette.Forest,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier
            .clip(CircleShape)
            .background(container)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = content, modifier = Modifier.size(13.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = content)
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showArrow: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Palette.Forest,
            contentColor = Color.White,
            disabledContainerColor = Palette.Line,
            disabledContentColor = Palette.Muted,
        ),
        contentPadding = PaddingValues(horizontal = 24.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
        if (showArrow) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward, null,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(16.dp),
            )
        }
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Palette.SoftSage, contentColor = Palette.Forest),
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

/** Small outlined pill button, e.g. "End break early". */
@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        shape = CircleShape,
        border = BorderStroke(1.dp, Palette.Line),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Palette.Card, contentColor = Palette.DeepPine),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
        modifier = modifier,
    ) { Text(text, style = MaterialTheme.typography.labelMedium) }
}

/** White rounded card with a hairline border. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    container: Color = Palette.Card,
    bordered: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = container,
        border = if (bordered) BorderStroke(1.dp, Palette.Line) else null,
    ) {
        Column(content = content)
    }
}

/** Rounded square holding an icon, used in lists. */
@Composable
fun IconTile(icon: ImageVector, container: Color = Palette.SoftSage, tint: Color = Palette.Forest, size: Dp = 40.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(container),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.5f)) }
}

/** Onboarding progress: the current page is a short dash, others are dots. */
@Composable
fun PageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            Box(
                Modifier
                    .height(5.dp)
                    .width(if (i == current) 18.dp else 5.dp)
                    .clip(CircleShape)
                    .background(if (i == current) Palette.Forest else Palette.SageDeep.copy(alpha = 0.5f))
            )
        }
    }
}

/** Uppercase small label, e.g. "TRY THIS", "BREAK LENGTH". */
@Composable
fun Eyebrow(text: String, color: Color = Palette.Muted, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color, modifier = modifier)
}

@Composable
fun ScreenTitle(title: String, subtitle: String? = null, centered: Boolean = false, modifier: Modifier = Modifier) {
    val align = if (centered) TextAlign.Center else TextAlign.Start
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = Palette.DeepPine, textAlign = align)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Palette.Muted, textAlign = align)
        }
    }
}

// ---------------------------------------------------------------- Landscape art

/**
 * The calm hills-and-sun illustration used across the app, drawn in code so it stays
 * crisp at any size. [sunX]/[sunY] position the sun as a fraction of the box.
 */
@Composable
fun Landscape(
    modifier: Modifier = Modifier,
    sunX: Float = 0.62f,
    sunY: Float = 0.36f,
    showPath: Boolean = true,
    showBirds: Boolean = true,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) { drawLandscape(sunX, sunY, showPath, showBirds) }
        overlay()
    }
}

private fun DrawScope.drawLandscape(sunX: Float, sunY: Float, showPath: Boolean, showBirds: Boolean) {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(Color(0xFFD5ECEE), Color(0xFFF4F1D8), Color(0xFFF7EFCF))))

    val sun = Offset(w * sunX, h * sunY)
    val r = minOf(w, h) * 0.2f
    drawCircle(Color(0xFFFBEFC6).copy(alpha = 0.7f), radius = r * 1.55f, center = sun)
    drawCircle(Palette.Sunlight, radius = r, center = sun)

    if (showBirds) {
        val bird = { x: Float, y: Float, s: Float ->
            val p = Path().apply {
                moveTo(x - s, y)
                quadraticTo(x - s / 2, y - s * 0.6f, x, y)
                quadraticTo(x + s / 2, y - s * 0.6f, x + s, y)
            }
            drawPath(p, Color(0xFF6F8F84), style = Stroke(width = 1.2f * density, cap = StrokeCap.Round))
        }
        bird(w * 0.2f, h * 0.22f, w * 0.03f)
        bird(w * 0.27f, h * 0.18f, w * 0.022f)
    }

    fun hill(color: Color, startY: Float, c1x: Float, c1y: Float, c2x: Float, c2y: Float, endY: Float) {
        val p = Path().apply {
            moveTo(0f, h * startY)
            cubicTo(w * c1x, h * c1y, w * c2x, h * c2y, w, h * endY)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(p, color)
    }
    hill(Color(0xFFC4DFC9), 0.50f, 0.25f, 0.40f, 0.55f, 0.58f, 0.46f)
    hill(Color(0xFFA2CBAF), 0.60f, 0.30f, 0.50f, 0.70f, 0.70f, 0.56f)
    hill(Color(0xFF79AE8E), 0.70f, 0.35f, 0.62f, 0.65f, 0.80f, 0.66f)
    hill(Color(0xFF4F8C6F), 0.84f, 0.30f, 0.74f, 0.70f, 0.92f, 0.76f)

    if (showPath) {
        val p = Path().apply {
            moveTo(w * 0.62f, h * 0.64f)
            cubicTo(w * 0.80f, h * 0.70f, w * 0.40f, h * 0.78f, w * 0.58f, h * 0.86f)
            cubicTo(w * 0.72f, h * 0.92f, w * 0.52f, h * 0.98f, w * 0.48f, h * 1.02f)
        }
        drawPath(p, Color(0xFFEFE3BC), style = Stroke(width = w * 0.035f, cap = StrokeCap.Round))
    }
    // Grass tufts
    val tuft = Color(0xFF8DBB9C)
    listOf(0.84f to 0.86f, 0.9f to 0.88f).forEach { (x, y) ->
        drawLine(tuft, Offset(w * x, h * y), Offset(w * x - w * 0.015f, h * y - h * 0.05f), 1.5f * density, StrokeCap.Round)
        drawLine(tuft, Offset(w * x, h * y), Offset(w * x + w * 0.02f, h * y - h * 0.06f), 1.5f * density, StrokeCap.Round)
    }
}

// ---------------------------------------------------------------- Rings

/** Draws the ring used by the dial and the countdown: faint outer ticks, sage track, forest arc. */
fun DrawScope.drawTimerRing(fraction: Float, strokeDp: Float = 10f, withTicks: Boolean = true): Pair<Offset, Float> {
    val stroke = strokeDp * density
    val tickSpace = if (withTicks) 14f * density else 0f
    val radius = size.minDimension / 2f - stroke / 2f - tickSpace
    val center = Offset(size.width / 2f, size.height / 2f)

    if (withTicks) {
        for (i in 0 until 60) {
            val ang = Math.toRadians((i * 6 - 90).toDouble())
            val major = i % 5 == 0
            val outer = radius + stroke / 2f + tickSpace - 2f * density
            val len = if (major) 7f * density else 4f * density
            val c = cos(ang).toFloat()
            val s = sin(ang).toFloat()
            drawLine(
                Palette.SageDeep.copy(alpha = if (major) 0.9f else 0.5f),
                Offset(center.x + c * outer, center.y + s * outer),
                Offset(center.x + c * (outer - len), center.y + s * (outer - len)),
                strokeWidth = 1.2f * density,
                cap = StrokeCap.Round,
            )
        }
    }
    // soft inner disc
    drawCircle(Palette.SoftSage.copy(alpha = 0.45f), radius = radius - stroke / 2f, center = center)
    drawCircle(Palette.SoftSage, radius = radius, center = center, style = Stroke(stroke))
    val sweep = 360f * fraction.coerceIn(0f, 1f)
    drawArc(
        Palette.Forest,
        startAngle = -90f,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(stroke, cap = StrokeCap.Round),
    )
    val endAng = Math.toRadians((sweep - 90f).toDouble())
    return Offset(center.x + cos(endAng).toFloat() * radius, center.y + sin(endAng).toFloat() * radius) to radius
}

/** Countdown: "TIME LEFT / 18:42 / remaining". */
@Composable
fun CountdownRing(fraction: Float, remainingMillis: Long, modifier: Modifier = Modifier, diameter: Dp = 230.dp) {
    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) { drawTimerRing(fraction) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Eyebrow("Time left")
            Text(formatRemaining(remainingMillis), style = MaterialTheme.typography.displayMedium, color = Palette.DeepPine)
            Text("remaining", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
        }
    }
}

// ---------------------------------------------------------------- Bottom navigation

enum class Tab(val label: String, val icon: ImageVector) {
    Break("Break", Icons.Rounded.AccessTime),
    Apps("Apps", Icons.Rounded.GridView),
    Progress("Progress", Icons.AutoMirrored.Rounded.ShowChart),
}

@Composable
fun BottomNav(current: Tab, onSelect: (Tab) -> Unit) {
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Palette.Line)
        )
        NavigationBar(containerColor = Palette.Parchment, tonalElevation = 0.dp) {
            Tab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = tab == current,
                    onClick = { onSelect(tab) },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Palette.Forest,
                        selectedTextColor = Palette.DeepPine,
                        indicatorColor = Palette.SoftSage,
                        unselectedIconColor = Palette.Muted,
                        unselectedTextColor = Palette.Muted,
                    ),
                )
            }
        }
    }
}

/** Thin bordered container for list rows. */
fun Modifier.hairline(shape: RoundedCornerShape = RoundedCornerShape(20.dp)) =
    this
        .clip(shape)
        .border(1.dp, Palette.Line, shape)
