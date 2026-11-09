package com.asad.breaktime.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val STEP = 5          // minutes per tick
private const val MIN_MINUTES = 5
private const val MAX_MINUTES = 12 * 60

/**
 * A round timer dial. Drag your finger around the ring: one full lap is 60 minutes,
 * and every extra lap adds an hour. The phone gives a light tick every 5 minutes.
 */
@Composable
fun DialTimePicker(
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    diameter: Dp = 260.dp,
) {
    val haptic = LocalHapticFeedback.current
    val current by rememberUpdatedState(minutes)
    val onChange by rememberUpdatedState(onMinutesChange)
    var lastAngle by remember { mutableFloatStateOf(0f) }
    var exact by remember { mutableFloatStateOf(minutes.toFloat()) }

    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.primaryContainer
    val tick = MaterialTheme.colorScheme.outline
    val knobFill = MaterialTheme.colorScheme.surface

    Box(
        modifier
            .size(diameter)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { start ->
                        lastAngle = angleOf(start, size)
                        exact = current.toFloat()
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val a = angleOf(change.position, size)
                        var delta = a - lastAngle
                        if (delta > 180f) delta -= 360f
                        if (delta < -180f) delta += 360f
                        lastAngle = a
                        exact = (exact + delta / 6f).coerceIn(MIN_MINUTES.toFloat(), MAX_MINUTES.toFloat())
                        val snapped = ((exact / STEP).roundToInt() * STEP).coerceIn(MIN_MINUTES, MAX_MINUTES)
                        if (snapped != current) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onChange(snapped)
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                // Tapping the ring jumps to that spot within the current hour.
                detectTapGestures { pos ->
                    val hours = current / 60
                    val inHour = ((angleOf(pos, size) / 6f) / STEP).roundToInt() * STEP
                    val target = (hours * 60 + inHour).coerceIn(MIN_MINUTES, MAX_MINUTES)
                    if (target != current) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onChange(target)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 22.dp.toPx()
            val radius = size.minDimension / 2f - stroke
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2, radius * 2)

            // Background ring
            drawCircle(track, radius = radius, style = Stroke(stroke))

            // Tick marks every 5 minutes (bigger every 15)
            for (i in 0 until 12) {
                val ang = Math.toRadians((i * 30 - 90).toDouble())
                val outer = radius - stroke / 2f - 6.dp.toPx()
                val len = if (i % 3 == 0) 12.dp.toPx() else 6.dp.toPx()
                val c = cos(ang).toFloat()
                val s = sin(ang).toFloat()
                drawLine(
                    tick,
                    start = Offset(center.x + c * outer, center.y + s * outer),
                    end = Offset(center.x + c * (outer - len), center.y + s * (outer - len)),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            // Completed hours show as a full softer ring underneath
            val hours = minutes / 60
            val rest = minutes % 60
            if (hours > 0) {
                drawCircle(primary.copy(alpha = 0.35f), radius = radius, style = Stroke(stroke))
            }
            val sweep = if (rest == 0 && hours > 0) 360f else rest * 6f
            drawArc(
                primary,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )

            // Knob at the end of the arc
            val knobAng = Math.toRadians((sweep - 90f).toDouble())
            val knob = Offset(
                center.x + cos(knobAng).toFloat() * radius,
                center.y + sin(knobAng).toFloat() * radius,
            )
            drawCircle(primary, radius = stroke * 0.85f, center = knob)
            drawCircle(knobFill, radius = stroke * 0.45f, center = knob)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val h = minutes / 60
            val m = minutes % 60
            Text(
                if (h > 0) "%d:%02d".format(h, m) else "$m",
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                when {
                    h == 0 -> "minutes"
                    h == 1 -> "hour : min"
                    else -> "hours : min"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Angle in degrees, 0 at the top, increasing clockwise. */
private fun angleOf(p: Offset, size: IntSize): Float {
    val dx = p.x - size.width / 2f
    val dy = p.y - size.height / 2f
    val deg = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat() + 90f
    return (deg + 360f) % 360f
}
