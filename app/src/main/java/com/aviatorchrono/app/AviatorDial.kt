package com.aviatorchrono.app

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform

// ---- Design tokens ----
val DialBackground = Color(0xFF0A1433)
val DialOutline    = Color(0xFF2A4070)
val Cream          = Color(0xFFD0D8E4)   // lume stripe colour
val Orange         = Color(0xFFFF6B1A)   // second hand + chrono sweep
val MinorTick      = Color(0xFF4A5A70)   // kept for compatibility
val Teal           = Color(0xFF4A9B94)   // kept for compatibility

// Hand colours
private val HandSilver = Color(0xFF7A8898)  // outer sword body (dark silver/gunmetal)
private val HandLume   = Color(0xFFE8E4D0)  // inner lume stripe (warm cream)

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

        // Chrono sweep — thin orange line, drawn under all main hands
        chronoAngleDeg?.let {
            drawSimpleLine(center, radius * 0.60f, it, Orange.copy(alpha = 0.85f), 2f)
        }

        // Minute hand — sword shape, longer and slightly narrower than hour
        drawSwordHand(
            center    = center,
            length    = radius * 0.72f,
            angleDeg  = minuteAngleDeg,
            maxWidth  = radius * 0.042f,
            outerColor = HandSilver,
            lumeColor  = HandLume
        )

        // Hour hand — sword shape, shorter and wider
        drawSwordHand(
            center    = center,
            length    = radius * 0.50f,
            angleDeg  = hourAngleDeg,
            maxWidth  = radius * 0.060f,
            outerColor = HandSilver,
            lumeColor  = HandLume
        )

        // Second hand — thin needle with orange counterweight tail
        secondAngleDeg?.let {
            drawSecondHand(
                center     = center,
                length     = radius * 0.84f,
                tailLength = radius * 0.22f,
                angleDeg   = it,
                color      = Orange
            )
        }

        // Hub — layered circles for depth
        drawCircle(color = HandSilver,           radius = radius * 0.040f, center = center)
        drawCircle(color = Color(0xFF1A1E28),    radius = radius * 0.024f, center = center)
        drawCircle(color = HandSilver,           radius = radius * 0.010f, center = center)
    }
}

/**
 * Breitling-style sword hand.
 *
 * Shape (local coords, pointing up, pivot at origin):
 *   - Narrow at pivot base (28% of maxWidth)
 *   - Widens over the first 15% of length to maxWidth
 *   - Stays parallel (full width) from 15% to 70% of length
 *   - Tapers sharply to a point at the tip
 *
 * A cream lume stripe runs down the centre at 40% of maxWidth,
 * stopping just short of the tip.
 */
private fun DrawScope.drawSwordHand(
    center: Offset,
    length: Float,
    angleDeg: Float,
    maxWidth: Float,
    outerColor: Color,
    lumeColor: Color?
) {
    withTransform({
        translate(center.x, center.y)
        rotate(angleDeg)
    }) {
        val baseW      = maxWidth * 0.28f    // width at pivot
        val wideStartY = -length * 0.15f    // y where hand reaches full width
        val taperStartY = -length * 0.70f   // y where taper to tip begins

        // Outer sword body
        val outer = Path().apply {
            moveTo(0f, 0f)
            lineTo(-baseW / 2f, 0f)
            lineTo(-maxWidth / 2f, wideStartY)
            lineTo(-maxWidth / 2f, taperStartY)
            lineTo(0f, -length)
            lineTo(maxWidth / 2f, taperStartY)
            lineTo(maxWidth / 2f, wideStartY)
            lineTo(baseW / 2f, 0f)
            close()
        }
        drawPath(outer, color = outerColor)

        // Lume stripe — same sword shape, narrower, stops 8% short of tip
        if (lumeColor != null) {
            val lw = maxWidth * 0.38f
            val lbw = baseW * 0.5f
            val lume = Path().apply {
                moveTo(0f, 0f)
                lineTo(-lbw / 2f, 0f)
                lineTo(-lw / 2f, wideStartY)
                lineTo(-lw / 2f, taperStartY)
                lineTo(0f, -length * 0.92f)
                lineTo(lw / 2f, taperStartY)
                lineTo(lw / 2f, wideStartY)
                lineTo(lbw / 2f, 0f)
                close()
            }
            drawPath(lume, color = lumeColor)
        }
    }
}

/**
 * Second hand — thin orange needle with counterweight tail behind pivot.
 * Tail is slightly thicker than the main needle (standard Swiss chrono style).
 */
private fun DrawScope.drawSecondHand(
    center: Offset,
    length: Float,
    tailLength: Float,
    angleDeg: Float,
    color: Color
) {
    withTransform({
        translate(center.x, center.y)
        rotate(angleDeg)
    }) {
        val sw = 2.2f

        // Tail (counterweight, thicker)
        drawLine(color, Offset(0f, 0f), Offset(0f, tailLength), sw * 1.8f, StrokeCap.Round)

        // Main needle to tip
        drawLine(color, Offset(0f, 0f), Offset(0f, -length), sw, StrokeCap.Round)

        // Small orange circle at pivot
        drawCircle(color, sw * 2.2f, Offset(0f, 0f))
    }
}

/** Simple line hand — used for the chrono sweep indicator. */
private fun DrawScope.drawSimpleLine(
    center: Offset,
    length: Float,
    angleDeg: Float,
    color: Color,
    strokeWidth: Float
) {
    withTransform({
        translate(center.x, center.y)
        rotate(angleDeg)
    }) {
        drawLine(color, Offset(0f, 0f), Offset(0f, -length), strokeWidth, StrokeCap.Round)
    }
}
