package com.aviatorchrono.app

import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
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
    val planeBitmap = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.jas_plane)
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
// jas_plane.png has nose at the top (row 0) of the image.
//
// Matrix operations (each postXxx is appended, so they're applied to points
// in REVERSE order — last op runs first on any given bitmap pixel):
//   1. postScale(scale)           — resize bitmap
//   2. postTranslate(-w/2, 0)     — put nose centre at local origin
//   3. postRotate(+angleDeg)      — CW rotation so nose points away from centre
//   4. postTranslate(tip.x, tip.y)— move origin to sweep-arm tip on screen
//
// Why +angleDeg (CW): Android canvas uses Y-down. CW rotation by θ transforms
// (x,y) → (x·cosθ − y·sinθ,  x·sinθ + y·cosθ). For the tail vector (0, h)
// this gives (−h·sinθ, h·cosθ), which points toward the dial centre at every
// clock position — exactly what we need.
//
private fun DrawScope.drawChronoSweep(
    center: Offset,
    length: Float,
    angleDeg: Float,
    color: Color,
    planeBitmap: android.graphics.Bitmap
) {
    val rad  = Math.toRadians((angleDeg - 90.0))
    val cosA = cos(rad).toFloat()
    val sinA = sin(rad).toFloat()
    fun pt(along: Float, perp: Float = 0f) = handPt(center, cosA, sinA, along, perp)

    val scaledH = length * 0.38f
    val scale   = scaledH / planeBitmap.height.toFloat()
    val scaledW = planeBitmap.width * scale
    val lineEnd = length - scaledH

    drawLine(color, center, pt(lineEnd), 2f, StrokeCap.Round)

    val tip = pt(length, 0f)
    val matrix = Matrix().apply {
        postScale(scale, scale)
        postTranslate(-scaledW / 2f, 0f)
        postRotate(angleDeg)
        postTranslate(tip.x, tip.y)
    }
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    drawIntoCanvas { it.nativeCanvas.drawBitmap(planeBitmap, matrix, paint) }
}
