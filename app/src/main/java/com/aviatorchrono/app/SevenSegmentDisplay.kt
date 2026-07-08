package com.aviatorchrono.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope

// Segment bitmask: bit6=a(top) 5=b(topRight) 4=c(botRight) 3=d(bot) 2=e(botLeft) 1=f(topLeft) 0=g(mid)
private val DIGIT_SEGMENTS = mapOf(
    '0' to 0b1111110,
    '1' to 0b0110000,
    '2' to 0b1101101,
    '3' to 0b1111001,
    '4' to 0b0110011,
    '5' to 0b1011011,
    '6' to 0b1011111,
    '7' to 0b1110000,
    '8' to 0b1111111,
    '9' to 0b1111011,
    '-' to 0b0000001,
    ' ' to 0b0000000,
    // Letters used for mode labels
    'C' to 0b1001110,   // a d e f  — top, bottom, bottom-left, top-left
    'D' to 0b0111101,   // b c d e g — lowercase-d shape, used by the "CD" countdown label
    'H' to 0b0110111,   // b c e f g — all verticals + middle
    'R' to 0b0000101,   // e g — lowercase-r style; avoids P confusion on small screens
    'h' to 0b0010111,   // c e f g  — lowercase h shape
    'r' to 0b0000101,   // e g      — same as R
)

// Gap between digit cells as a fraction of digitWidth.
// 0.35 = 35% of one digit width — enough to clearly separate e.g. "1" from "6".
private const val DIGIT_GAP_FRACTION = 0.35f

/**
 * Draws 7-segment style text at [topLeft].
 * Supported characters: '0'-'9', ':', '.', '-', ' '.
 * Returns total pixel width consumed.
 *
 * [strokeWidth] defaults to ~12% of digitWidth.
 * Inter-digit gap is DIGIT_GAP_FRACTION × digitWidth (in addition to stroke width).
 */
fun DrawScope.drawSevenSegText(
    text: String,
    topLeft: Offset,
    digitWidth: Float,
    digitHeight: Float,
    onColor: Color,
    offColor: Color,
    strokeWidth: Float = digitWidth * 0.12f
): Float {
    val sw  = if (strokeWidth <= 0f) digitWidth * 0.12f else strokeWidth
    val pad = sw * 0.8f
    val gap = digitWidth * DIGIT_GAP_FRACTION   // explicit inter-digit gap
    val colonW = digitWidth * 0.45f
    val dotW   = digitWidth * 0.32f
    var x = topLeft.x
    val y = topLeft.y
    val w = digitWidth
    val h = digitHeight

    for (ch in text) {
        when (ch) {
            ':' -> {
                val cx = x + colonW / 2f
                val r = sw * 0.6f
                drawCircle(onColor, r, Offset(cx, y + h * 0.28f))
                drawCircle(onColor, r, Offset(cx, y + h * 0.72f))
                x += colonW
            }
            '.' -> {
                val cx = x + dotW / 2f
                val r = sw * 0.6f
                drawCircle(onColor, r, Offset(cx, y + h - r))
                x += dotW
            }
            else -> {
                val mask = DIGIT_SEGMENTS[ch] ?: 0
                fun active(bit: Int) = (mask shr bit) and 1 == 1
                fun seg(bit: Int, start: Offset, end: Offset) = drawLine(
                    color = if (active(bit)) onColor else offColor,
                    start = start, end = end,
                    strokeWidth = sw, cap = StrokeCap.Round
                )
                seg(6, Offset(x + pad, y),            Offset(x + w - pad, y))           // a top
                seg(5, Offset(x + w, y + pad),         Offset(x + w, y + h/2 - pad))    // b top-right
                seg(4, Offset(x + w, y + h/2 + pad),   Offset(x + w, y + h - pad))      // c bot-right
                seg(3, Offset(x + pad, y + h),         Offset(x + w - pad, y + h))      // d bottom
                seg(2, Offset(x, y + h/2 + pad),       Offset(x, y + h - pad))          // e bot-left
                seg(1, Offset(x, y + pad),              Offset(x, y + h/2 - pad))       // f top-left
                seg(0, Offset(x + pad, y + h/2),       Offset(x + w - pad, y + h/2))   // g middle
                x += w + gap   // advance by digit width + explicit gap
            }
        }
    }
    return x - topLeft.x
}

/** Measures total pixel width that [drawSevenSegText] would consume for [text]. */
fun measureSevenSegWidth(
    text: String,
    digitWidth: Float,
    strokeWidth: Float = digitWidth * 0.12f
): Float {
    val gap = digitWidth * DIGIT_GAP_FRACTION
    var total = 0f
    var lastWasDigit = false
    for (ch in text) {
        total += when (ch) {
            ':' -> { lastWasDigit = false; digitWidth * 0.45f }
            '.' -> { lastWasDigit = false; digitWidth * 0.32f }
            else -> { lastWasDigit = true; digitWidth + gap }
        }
    }
    // Remove trailing gap after the last digit (no gap needed after the final character)
    if (lastWasDigit) total -= gap
    return total
}

/**
 * Computes the largest [digitWidth] / [digitHeight] pair (at fixed aspect ratio [widthToHeight])
 * such that [text] fits within [availWidth] x [availHeight].
 */
fun sevenSegFitSize(
    text: String,
    availWidth: Float,
    availHeight: Float,
    widthToHeight: Float = 0.58f
): Pair<Float, Float> {
    val gapFrac = DIGIT_GAP_FRACTION
    var widthUnits = 0f
    var lastWasDigit = false
    for (ch in text) {
        widthUnits += when (ch) {
            ':' -> { lastWasDigit = false; 0.45f }
            '.' -> { lastWasDigit = false; 0.32f }
            else -> { lastWasDigit = true; 1f + gapFrac }
        }
    }
    if (lastWasDigit) widthUnits -= gapFrac  // no trailing gap on last digit

    val dwFromWidth = if (widthUnits > 0f) availWidth / widthUnits else availWidth
    val dhFromWidth = dwFromWidth / widthToHeight

    val dhFromHeight = availHeight
    val dwFromHeight = dhFromHeight * widthToHeight

    return if (dhFromWidth <= dhFromHeight) {
        Pair(dwFromWidth, dhFromWidth)
    } else {
        Pair(dwFromHeight, dhFromHeight)
    }
}
