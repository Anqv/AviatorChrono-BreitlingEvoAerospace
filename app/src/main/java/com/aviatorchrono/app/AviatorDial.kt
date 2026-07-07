package com.aviatorchrono.app

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
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
private val HandSilver = Color(0xFF7A8898)
private val HandLume   = Color(0xFFE8E4D0)

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
    val context = LocalContext.current
    val planeBitmap: ImageBitmap = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.jas_plane).asImageBitmap()
    }

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 4f

        // Chrono sweep with JAS airplane tip — drawn under main hands
        chronoAngleDeg?.let {
            drawChronoSweep(center, radius * 0.62f, it, Orange.copy(alpha = 0.9f), planeBitmap)
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
// All hand-drawing functions use:
//   rad = toRadians(angleDeg - 90)   →  0° = 12 o'clock, 90° = 3 o'clock
//
// pt(along, perp):
//   along – distance from pivot toward tip (positive)
//   perp  – lateral offset; positive = RIGHT when facing the tip
//
private fun handPt(center: Offset, cosA: Float, sinA: Float, along: Float, perp: Float) =
    Offset(
        center.x + along * cosA - perp * sinA,
        center.y + along * sinA + perp * cosA
    )

// ─── Sword / Breitling hand ──────────────────────────────────────────────────
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

    val bw = maxWidth * 0.28f
    val ws = length * 0.15f
    val ts = length * 0.70f

    val outer = Path().apply {
        pt(0f, -bw / 2f).let         { moveTo(it.x, it.y) }
        pt(ws, -maxWidth / 2f).let   { lineTo(it.x, it.y) }
        pt(ts, -maxWidth / 2f).let   { lineTo(it.x, it.y) }
        pt(length, 0f).let           { lineTo(it.x, it.y) }
        pt(ts, maxWidth / 2f).let    { lineTo(it.x, it.y) }
        pt(ws, maxWidth / 2f).let    { lineTo(it.x, it.y) }
        pt(0f, bw / 2f).let          { lineTo(it.x, it.y) }
        close()
    }
    drawPath(outer, outerColor)

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

    drawLine(color, center, pt(-tailLength), 4f,   StrokeCap.Round)
    drawLine(color, center, pt(length),      2.2f, StrokeCap.Round)
    drawCircle(color, 4f, center)
}

// ─── Chrono sweep with JAS airplane bitmap at tip ────────────────────────────
//
// jas_plane.png has its nose at the top (row 0). To orient nose in the sweep
// direction we rotate by -angleDeg (CCW by angleDeg degrees):
//   bitmap body (+Y) must map to (-cosA, -sinA) — toward center.
//   Solving CW rotation θ: sinθ = -cosA, cosθ = -sinA  →  θ = -angleDeg.
//
// withTransform sequence (each op post-concatenated to CTM, so rightmost applied first):
//   1. translate(tip)           — origin at tip in screen coords
//   2. rotate(-angleDeg, 0,0)  — rotate around tip
//   3. translate(-scaledW/2,0) — shift so top-center of bitmap lands at origin
// Drawing at (0,0) with dstSize then places the bitmap correctly.
//
private fun DrawScope.drawChronoSweep(
    center: Offset,
    length: Float,
    angleDeg: Float,
    color: Color,
    planeBitmap: ImageBitmap
) {
    val rad  = Math.toRadians((angleDeg - 90.0))
    val cosA = cos(rad).toFloat()
    val sinA = sin(rad).toFloat()
    fun pt(along: Float, perp: Float = 0f) = handPt(center, cosA, sinA, along, perp)

    val scaledH  = length * 0.38f
    val scaledW  = scaledH * (planeBitmap.width.toFloat() / planeBitmap.height.toFloat())
    val lineEnd  = length - scaledH

    drawLine(color, center, pt(lineEnd), 2f, StrokeCap.Round)

    val tip = pt(length, 0f)
    withTransform({
        translate(tip.x, tip.y)
        rotate(-angleDeg, pivot = Offset.Zero)
        translate(-scaledW / 2f, 0f)
    }) {
        drawImage(
            image   = planeBitmap,
            dstSize = IntSize(scaledW.toInt(), scaledH.toInt())
        )
    }
}
