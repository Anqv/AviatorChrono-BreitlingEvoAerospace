package com.aviatorchrono.app

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.cos
import kotlin.math.sin

// ---- Design tokens ----
val DialBackground = Color(0xFF0A1433)
val DialOutline    = Color(0xFF2A4070)
val Cream          = Color(0xFFD0D8E4)
val Orange         = Color(0xFFFF6B1A)
val MinorTick      = Color(0xFF4A5A70)
val Teal           = Color(0xFF4A9B94)

// Hand colours
private val HandSilver = Color(0xFF7A8898)   // outer sword body
private val HandLume   = Color(0xFFE8E4D0)   // inner lume stripe

// LCD panel tokens (referenced from MainActivity)
val LcdBackground  = Color(0xFF050D05)
val LcdBorder      = Color(0xFF1E2E1E)
val LcdLabel       = Color(0xFF607060)

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

        // Chrono sweep with airplane tip — drawn under main hands
        chronoAngleDeg?.let {
            drawChronoSweep(center, radius * 0.62f, it, Orange.copy(alpha = 0.9f))
        }

        // Minute hand — longer, slightly narrower sword
        drawSwordHand(
            center     = center,
            length     = radius * 0.72f,
            angleDeg   = minuteAngleDeg,
            maxWidth   = radius * 0.042f,
            outerColor = HandSilver,
            lumeColor  = HandLume
        )

        // Hour hand — shorter, wider sword
        drawSwordHand(
            center     = center,
            length     = radius * 0.50f,
            angleDeg   = hourAngleDeg,
            maxWidth   = radius * 0.060f,
            outerColor = HandSilver,
            lumeColor  = HandLume
        )

        // Second hand — thin needle with counterweight tail
        secondAngleDeg?.let {
            drawSecondHand(center, radius * 0.84f, radius * 0.22f, it, Orange)
        }

        // Hub — three concentric circles for depth
        drawCircle(HandSilver,         radius * 0.040f, center)
        drawCircle(Color(0xFF1A1E28),  radius * 0.024f, center)
        drawCircle(HandSilver,         radius * 0.010f, center)
    }
}

// ─── Coordinate helper ───────────────────────────────────────────────────────
//
// All hand-drawing functions use the same angle convention as the original:
//   rad = toRadians(angleDeg - 90)
// so 0° = 12 o'clock, 90° = 3 o'clock, etc.
//
// pt(along, perp) converts a hand-local coordinate to a screen Offset:
//   along  – distance from pivot toward the tip (positive)
//   perp   – lateral offset, positive = to the RIGHT when facing the tip
//
private fun handPt(center: Offset, cosA: Float, sinA: Float, along: Float, perp: Float) =
    Offset(
        center.x + along * cosA - perp * sinA,
        center.y + along * sinA + perp * cosA
    )

// ─── Sword / Breitling hand ──────────────────────────────────────────────────
//
// Shape (pivot at 0, tip at `length`):
//   • narrow base (28% of maxWidth)
//   • widens to maxWidth over the first 15% of length
//   • parallel section from 15% to 70%
//   • tapers to a sharp point at the tip
//
// Cream lume stripe runs down the centre at 38% of maxWidth.
//
private fun DrawScope.drawSwordHand(
    center: Offset,
    length: Float,
    angleDeg: Float,
    maxWidth: Float,
    outerColor: Color,
    lumeColor: Color?
) {
    val rad  = Math.toRadians((angleDeg - 90.0))
    val cosA = cos(rad).toFloat()
    val sinA = sin(rad).toFloat()
    fun pt(along: Float, perp: Float) = handPt(center, cosA, sinA, along, perp)

    val bw = maxWidth * 0.28f   // base half-width (at pivot)
    val ws = length * 0.15f     // along-position where full width is reached
    val ts = length * 0.70f     // along-position where taper to tip begins

    // Outer sword body — start at left base, trace outline, close across base
    val outer = Path().apply {
        pt(0f, -bw / 2f).let         { moveTo(it.x, it.y) }  // base left
        pt(ws, -maxWidth / 2f).let   { lineTo(it.x, it.y) }  // wide left
        pt(ts, -maxWidth / 2f).let   { lineTo(it.x, it.y) }  // taper left
        pt(length, 0f).let           { lineTo(it.x, it.y) }  // tip
        pt(ts, maxWidth / 2f).let    { lineTo(it.x, it.y) }  // taper right
        pt(ws, maxWidth / 2f).let    { lineTo(it.x, it.y) }  // wide right
        pt(0f, bw / 2f).let          { lineTo(it.x, it.y) }  // base right
        close()                                                // flat base edge
    }
    drawPath(outer, outerColor)

    // Lume stripe — same sword shape, 38% of maxWidth, stops at 92% of length
    if (lumeColor != null) {
        val lw  = maxWidth * 0.38f
        val lbw = bw * 0.50f
        val lume = Path().apply {
            pt(0f, -lbw / 2f).let      { moveTo(it.x, it.y) }
            pt(ws, -lw / 2f).let       { lineTo(it.x, it.y) }
            pt(ts, -lw / 2f).let       { lineTo(it.x, it.y) }
            pt(length * 0.92f, 0f).let { lineTo(it.x, it.y) }
            pt(ts, lw / 2f).let        { lineTo(it.x, it.y) }
            pt(ws, lw / 2f).let        { lineTo(it.x, it.y) }
            pt(0f, lbw / 2f).let       { lineTo(it.x, it.y) }
            close()
        }
        drawPath(lume, lumeColor)
    }
}

// ─── Second hand with counterweight tail ─────────────────────────────────────
private fun DrawScope.drawSecondHand(
    center: Offset,
    length: Float,
    tailLength: Float,
    angleDeg: Float,
    color: Color
) {
    val rad  = Math.toRadians((angleDeg - 90.0))
    val cosA = cos(rad).toFloat()
    val sinA = sin(rad).toFloat()
    fun pt(along: Float) = handPt(center, cosA, sinA, along, 0f)

    drawLine(color, center, pt(-tailLength), 4f,   StrokeCap.Round)  // counterweight
    drawLine(color, center, pt(length),      2.2f, StrokeCap.Round)  // needle
    drawCircle(color, 4f, center)                                     // pivot dot
}

// ─── Chrono sweep with airplane at tip ───────────────────────────────────────
private fun DrawScope.drawChronoSweep(
    center: Offset,
    length: Float,
    angleDeg: Float,
    color: Color
) {
    val rad  = Math.toRadians((angleDeg - 90.0))
    val cosA = cos(rad).toFloat()
    val sinA = sin(rad).toFloat()
    fun pt(along: Float, perp: Float = 0f) = handPt(center, cosA, sinA, along, perp)

    val ps      = length * 0.11f        // airplane scale unit
    val lineEnd = length - ps * 2.0f    // line stops before the airplane body

    // Thin stem
    drawLine(color, center, pt(lineEnd), 2f, StrokeCap.Round)

    // ── Top-down airplane silhouette ──────────────────────────────────────
    // Coordinates in (along, perp): nose at `length`, tail toward pivot.
    val noseY    = length
    val fLen     = ps * 1.8f
    val fW       = ps * 0.30f           // fuselage half-width
    val wingFront = noseY - fLen * 0.30f
    val wingBack  = noseY - fLen * 0.62f
    val span      = ps * 1.20f          // half-wingspan
    val tailY     = noseY - fLen

    // Fuselage
    val fuselage = Path().apply {
        pt(noseY, 0f).let                          { moveTo(it.x, it.y) }  // nose
        pt(wingFront, -fW).let                     { lineTo(it.x, it.y) }  // left shoulder
        pt(tailY + ps * 0.18f, -fW * 0.50f).let  { lineTo(it.x, it.y) }  // left tail root
        pt(tailY, 0f).let                          { lineTo(it.x, it.y) }  // tail tip
        pt(tailY + ps * 0.18f,  fW * 0.50f).let  { lineTo(it.x, it.y) }  // right tail root
        pt(wingFront,  fW).let                     { lineTo(it.x, it.y) }  // right shoulder
        close()
    }
    drawPath(fuselage, color)

    // Left wing
    val lWing = Path().apply {
        pt(wingFront, -fW).let   { moveTo(it.x, it.y) }   // root leading
        pt(wingBack, -span).let  { lineTo(it.x, it.y) }   // wingtip
        pt(wingBack, -fW).let    { lineTo(it.x, it.y) }   // root trailing
        close()
    }
    drawPath(lWing, color)

    // Right wing
    val rWing = Path().apply {
        pt(wingFront, fW).let   { moveTo(it.x, it.y) }
        pt(wingBack, span).let  { lineTo(it.x, it.y) }
        pt(wingBack, fW).let    { lineTo(it.x, it.y) }
        close()
    }
    drawPath(rWing, color)
}
