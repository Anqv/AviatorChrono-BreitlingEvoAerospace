package com.aviatorchrono.app

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.cos
import kotlin.math.sin

// ---- Design tokens ----
val DialBackground = Color(0xFF0A1433)
val DialOutline    = Color(0xFF2A4070)
val Cream          = Color(0xFFD0D8E4)   // hour/minute hands
val Orange         = Color(0xFFFF6B1A)   // second hand + chrono sweep
val MinorTick      = Color(0xFF4A5A70)   // kept for compatibility
val Teal           = Color(0xFF4A9B94)   // kept for compatibility

// LCD panel tokens (referenced from MainActivity)
val LcdBackground  = Color(0xFF050D05)
val LcdBorder      = Color(0xFF1E2E1E)
val LcdLabel       = Color(0xFF607060)

/** Draws only the analog hands and hub — background comes from the PNG image. */
@Composable
fun AviatorDial(
    hourAngleDeg: Float,
    minuteAngleDeg: Float,
    secondAngleDeg: Float?,
    chronoAngleDeg: Float?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 4f

        // Chrono sweep (under main hands)
        chronoAngleDeg?.let { drawHand(center, radius * 0.60f, it, Orange.copy(alpha = 0.9f), 2.5f) }

        // Second hand (orange, thin)
        secondAngleDeg?.let { drawHand(center, radius * 0.82f, it, Orange, 2f) }

        // Minute hand
        drawHand(center, radius * 0.70f, minuteAngleDeg, Cream, 6f)

        // Hour hand
        drawHand(center, radius * 0.48f, hourAngleDeg, Cream, 9f)

        // Hub
        drawCircle(color = Cream, radius = 7f, center = center)
        drawCircle(color = Color(0xFF1A1A1A), radius = 2.5f, center = center)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHand(
    center: Offset,
    length: Float,
    angleDeg: Float,
    color: Color,
    strokeWidth: Float
) {
    val rad = Math.toRadians((angleDeg - 90.0))
    drawLine(
        color = color,
        start = center,
        end = Offset(center.x + length * cos(rad).toFloat(), center.y + length * sin(rad).toFloat()),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}
