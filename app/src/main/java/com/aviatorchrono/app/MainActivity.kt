package com.aviatorchrono.app

import android.graphics.BitmapFactory
import android.graphics.Paint
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import android.graphics.RectF
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private val chrono = ChronoState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AviatorChronoScreen(
                chrono = chrono,
                onNavigationActiveChanged = { active ->
                    if (active) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            )
        }
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

// ─── Tap-zone fractions (all relative to screen width/height) ────────────────
//
//  Upper LCD  │ x: 20.9–79.3%  y: 17.5–30.0%  │ tap → toggle precision
//  Lower LCD  │ x: 20.9–79.3%  y: 64.9–82.5%  │ tap → cycle LCD colour
//  Hub        │ x: 42.5–57.5%  y: 42.5–57.5%  │ tap → park / unpark hands
//  Rest dial  │ everything else                 │ tap → chrono start/stop/reset
//

@Composable
fun AviatorChronoScreen(
    chrono: ChronoState,
    onNavigationActiveChanged: (Boolean) -> Unit
) {
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(100)
        }
    }

    LaunchedEffect(chrono.isNavigationActive) {
        onNavigationActiveChanged(chrono.isNavigationActive)
    }

    val context = LocalContext.current
    val bgBitmap = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.watch_face_clean_blue)
    }

    val calendar = remember(nowMs) { Calendar.getInstance().apply { timeInMillis = nowMs } }
    val hour12 = calendar.get(Calendar.HOUR)
    val minute = calendar.get(Calendar.MINUTE)
    val second = calendar.get(Calendar.SECOND)
    val millis = calendar.get(Calendar.MILLISECOND)

    val utcCal = remember(nowMs) {
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = nowMs }
    }

    // When parked: hour at 3 o'clock (90°), minute at 9 o'clock (270°), second hidden
    val hourAngle    = if (chrono.parkedMode) 90f  else hour12 * 30f + minute * 0.5f
    val minuteAngle  = if (chrono.parkedMode) 270f else minute * 6f + second * 0.1f
    val secondAngle: Float? = if (chrono.parkedMode) null else second * 6f + millis * 0.006f
    val chronoElapsed = chrono.currentElapsedMs(nowMs)
    val chronoAngle  = ((chronoElapsed / 1000.0) % 60.0 * 6.0).toFloat()

    val lcdOn  = chrono.lcdColor.onColor
    val lcdOff = chrono.lcdColor.offColor

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(DialBackground)) {
        val swDp = maxWidth
        val shDp = maxHeight
        val density = LocalDensity.current
        val swPx = with(density) { swDp.toPx() }
        val shPx = with(density) { shDp.toPx() }

        // ── Single tap dispatcher (lowest Z — no visual content) ──────────────
        // All other layers carry NO clickable/pointerInput, so all taps fall here.
        // We dispatch by fractional coordinate so zones never overlap or conflict.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(swPx, shPx) {
                    detectTapGestures { offset ->
                        val fx = offset.x / swPx
                        val fy = offset.y / shPx
                        when {
                            // Centre hub → park / unpark
                            fx in 0.425f..0.575f && fy in 0.425f..0.575f ->
                                chrono.toggleParkedMode()
                            // Upper LCD → toggle SEC / 1/100 precision
                            fx in 0.2088f..0.7928f && fy in 0.1746f..0.3002f ->
                                chrono.togglePrecision()
                            // Lower LCD → cycle LCD colour
                            fx in 0.2088f..0.7928f && fy in 0.6493f..0.8254f ->
                                chrono.cycleLcdColor()
                            // Rest of dial → chrono start / stop / reset
                            else ->
                                chrono.toggleStartStop(System.currentTimeMillis())
                        }
                    }
                }
        )

        // ── Layer 1 – PNG background ──────────────────────────────────────────
        if (bgBitmap != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawBitmap(
                        bgBitmap,
                        null,
                        RectF(0f, 0f, size.width, size.height),
                        null
                    )
                }
            }
        }

        // ── Layer 2 – Upper LCD ───────────────────────────────────────────────
        // slot y=107-184 → frac 0.1746-0.3002, x=128-486 → frac 0.2088-0.7928
        Box(
            modifier = Modifier
                .absoluteOffset(x = swDp * 0.2088f, y = shDp * 0.1746f)
                .width(swDp * 0.5840f)
                .height(shDp * 0.1256f)
                .background(LcdBackground)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val panelW = size.width
                val panelH = size.height
                val vPad = panelH * 0.07f
                val hPad = panelW * 0.03f
                val innerH = panelH - vPad * 2f
                val innerW = panelW - hPad * 2f

                if (chrono.isChronoActive) {
                    // CHR or CHR 1-100
                    val modeText = if (chrono.precision == ChronoPrecision.HUNDREDTHS) "CHR 1-100" else "CHR"
                    val (dw, dh) = sevenSegFitSize(modeText, innerW, innerH)
                    val totalW = measureSevenSegWidth(modeText, dw)
                    val sx = hPad + (innerW - totalW) / 2f
                    val sy = vPad + (innerH - dh) / 2f
                    drawSevenSegText(modeText, Offset(sx, sy), dw, dh, lcdOn, lcdOff)
                } else {
                    // Date: "MON  07  JUL" mixing native text + 7-seg digits
                    val dayAbbr   = dayAbbrev(calendar)
                    val monthAbbr = monthAbbrev(calendar)
                    val dayNum    = "%02d".format(calendar.get(Calendar.DAY_OF_MONTH))

                    val (dw, dh) = sevenSegFitSize(dayNum, innerW * 0.30f, innerH)
                    val segW = measureSevenSegWidth(dayNum, dw)

                    val textPaint = Paint().apply {
                        isAntiAlias = true
                        color = lcdOn.toArgb()
                        typeface = android.graphics.Typeface.MONOSPACE
                        textSize = dh * 0.52f
                        textAlign = Paint.Align.LEFT
                    }
                    val textW = textPaint.measureText("MON")
                    val spacing = dw * 0.5f
                    val rowW = textW + spacing + segW + spacing + textW

                    var cx = hPad + (innerW - rowW) / 2f
                    val segY  = vPad + (innerH - dh) / 2f
                    val textY = segY + dh * 0.72f

                    drawIntoCanvas { c ->
                        c.nativeCanvas.drawText(dayAbbr, cx, textY, textPaint)
                    }
                    cx += textW + spacing
                    drawSevenSegText(dayNum, Offset(cx, segY), dw, dh, lcdOn, lcdOff)
                    cx += segW + spacing
                    drawIntoCanvas { c ->
                        c.nativeCanvas.drawText(monthAbbr, cx, textY, textPaint)
                    }
                }
            }
        }

        // ── Layer 3 – Lower LCD ───────────────────────────────────────────────
        // slot y=398-506 → frac 0.6493-0.8254, x=128-486 → frac 0.2088-0.7928
        Box(
            modifier = Modifier
                .absoluteOffset(x = swDp * 0.2088f, y = shDp * 0.6493f)
                .width(swDp * 0.5840f)
                .height(shDp * 0.1761f)
                .background(LcdBackground)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val panelW = size.width
                val panelH = size.height
                val vPad = panelH * 0.08f
                val hPad = panelW * 0.04f
                val innerH = panelH - vPad * 2f
                val innerW = panelW - hPad * 2f

                val displayText = if (chrono.isChronoActive) {
                    formatChrono(chronoElapsed, chrono.precision)
                } else {
                    "%02d:%02d:%02d".format(
                        utcCal.get(Calendar.HOUR_OF_DAY),
                        utcCal.get(Calendar.MINUTE),
                        utcCal.get(Calendar.SECOND)
                    )
                }

                val (dw, dh) = sevenSegFitSize(displayText, innerW, innerH)
                val totalW = measureSevenSegWidth(displayText, dw)
                val sx = hPad + (innerW - totalW) / 2f
                val sy = vPad + (innerH - dh) / 2f
                drawSevenSegText(displayText, Offset(sx, sy), dw, dh, lcdOn, lcdOff)

                val labelText = if (chrono.isChronoActive) "CHR" else "UTC"
                val dimPaint = Paint().apply {
                    isAntiAlias = true
                    color = lcdOff.copy(alpha = 0.7f).toArgb()
                    typeface = android.graphics.Typeface.MONOSPACE
                    textSize = panelH * 0.18f
                    textAlign = Paint.Align.RIGHT
                }
                drawIntoCanvas { c ->
                    c.nativeCanvas.drawText(labelText, panelW - hPad, panelH - vPad * 0.4f, dimPaint)
                }
            }
        }

        // ── Layer 4 – Analog hands ────────────────────────────────────────────
        AviatorDial(
            hourAngleDeg   = hourAngle,
            minuteAngleDeg = minuteAngle,
            secondAngleDeg = secondAngle,
            chronoAngleDeg = chronoAngle,
            modifier       = Modifier.fillMaxSize()
        )
    }
}

private fun formatChrono(elapsedMs: Long, precision: ChronoPrecision): String {
    val totalSec = elapsedMs / 1000
    val hh = totalSec / 3600
    val mm = (totalSec / 60) % 60
    val ss = totalSec % 60
    val cs = (elapsedMs / 10) % 100
    return if (precision == ChronoPrecision.HUNDREDTHS)
        "%02d:%02d.%02d".format(mm, ss, cs)
    else
        "%02d:%02d:%02d".format(hh, mm, ss)
}

private fun dayAbbrev(cal: Calendar): String =
    SimpleDateFormat("EEE", Locale.US).format(cal.time).uppercase(Locale.US)

private fun monthAbbrev(cal: Calendar): String =
    SimpleDateFormat("MMM", Locale.US).format(cal.time).uppercase(Locale.US)
