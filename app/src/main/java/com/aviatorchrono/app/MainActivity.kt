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

// ─── Tap zones (fractional screen coordinates) ────────────────────────────────
//
//  Upper LCD   x 20.9–79.3%  y 17.5–30.0%  → cycle mode NORMAL/CHR/CHR 1/100
//  Lower LCD left half   x 20.9–50.1%  y 64.9–82.5%  → stop / reset chrono
//  Lower LCD right half  x 50.1–79.3%  y 64.9–82.5%  → start / continue chrono
//  Centre hub  x 42.5–57.5%  y 42.5–57.5%  → park / unpark hands
//  Dial (rest)                                → cycle LCD colour

private const val LCD_X1 = 0.2088f;  private const val LCD_X2 = 0.7928f
private const val LCD_XM = 0.5008f   // midpoint of lower LCD
private const val ULCD_Y1 = 0.1746f; private const val ULCD_Y2 = 0.3002f
private const val LLCD_Y1 = 0.6493f; private const val LLCD_Y2 = 0.8254f

@Composable
fun AviatorChronoScreen(
    chrono: ChronoState,
    onNavigationActiveChanged: (Boolean) -> Unit
) {
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) { nowMs = System.currentTimeMillis(); delay(100) }
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
    val minute  = calendar.get(Calendar.MINUTE)
    val second  = calendar.get(Calendar.SECOND)
    val millis  = calendar.get(Calendar.MILLISECOND)

    val utcCal = remember(nowMs) {
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = nowMs }
    }

    val hourAngle   = if (chrono.parkedMode) 90f  else hour12 * 30f + minute * 0.5f
    val minuteAngle = if (chrono.parkedMode) 270f else minute * 6f + second * 0.1f
    val secondAngle: Float? = if (chrono.parkedMode) null else second * 6f + millis * 0.006f

    val chronoElapsed = chrono.currentElapsedMs(nowMs)
    val chronoAngle   = ((chronoElapsed / 1000.0) % 60.0 * 6.0).toFloat()

    val lcdOn  = chrono.lcdColor.onColor
    val lcdOff = chrono.lcdColor.offColor

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(DialBackground)) {
        val swDp = maxWidth;  val shDp = maxHeight
        val density = LocalDensity.current
        val swPx = with(density) { swDp.toPx() }
        val shPx = with(density) { shDp.toPx() }

        // ── Single tap dispatcher ─────────────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxSize().pointerInput(swPx, shPx) {
                detectTapGestures { offset ->
                    val fx = offset.x / swPx
                    val fy = offset.y / shPx
                    when {
                        // Upper LCD → cycle mode
                        fx in LCD_X1..LCD_X2 && fy in ULCD_Y1..ULCD_Y2 ->
                            chrono.cycleMode()
                        // Lower LCD left → stop / reset
                        fx in LCD_X1..LCD_XM && fy in LLCD_Y1..LLCD_Y2 ->
                            chrono.stopOrReset(System.currentTimeMillis())
                        // Lower LCD right → start / continue
                        fx in LCD_XM..LCD_X2 && fy in LLCD_Y1..LLCD_Y2 ->
                            chrono.startContinue(System.currentTimeMillis())
                        // Centre hub → park / unpark
                        fx in 0.425f..0.575f && fy in 0.425f..0.575f ->
                            chrono.toggleParkedMode()
                        // Rest of dial → cycle LCD colour
                        else -> chrono.cycleLcdColor()
                    }
                }
            }
        )

        // ── Layer 1 – PNG background ──────────────────────────────────────────
        if (bgBitmap != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawBitmap(
                        bgBitmap, null,
                        RectF(0f, 0f, size.width, size.height), null
                    )
                }
            }
        }

        // ── Layer 2 – Upper LCD ───────────────────────────────────────────────
        Box(
            modifier = Modifier
                .absoluteOffset(x = swDp * LCD_X1, y = shDp * ULCD_Y1)
                .width(swDp * (LCD_X2 - LCD_X1))
                .height(shDp * (ULCD_Y2 - ULCD_Y1))
                .background(LcdBackground)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val panelW = size.width;  val panelH = size.height
                val vPad = panelH * 0.07f;  val hPad = panelW * 0.03f
                val innerH = panelH - vPad * 2f;  val innerW = panelW - hPad * 2f

                when (chrono.watchMode) {
                    WatchMode.NORMAL -> {
                        // Date: "MON  07  JUL"
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
                        val textW   = textPaint.measureText("MON")
                        val spacing = dw * 0.5f
                        val rowW    = textW + spacing + segW + spacing + textW

                        var cx    = hPad + (innerW - rowW) / 2f
                        val segY  = vPad + (innerH - dh) / 2f
                        val textY = segY + dh * 0.72f

                        drawIntoCanvas { c -> c.nativeCanvas.drawText(dayAbbr, cx, textY, textPaint) }
                        cx += textW + spacing
                        drawSevenSegText(dayNum, Offset(cx, segY), dw, dh, lcdOn, lcdOff)
                        cx += segW + spacing
                        drawIntoCanvas { c -> c.nativeCanvas.drawText(monthAbbr, cx, textY, textPaint) }
                    }
                    WatchMode.CHR -> {
                        val (dw, dh) = sevenSegFitSize("CHR", innerW, innerH)
                        val tw = measureSevenSegWidth("CHR", dw)
                        drawSevenSegText("CHR", Offset(hPad + (innerW - tw) / 2f, vPad + (innerH - dh) / 2f), dw, dh, lcdOn, lcdOff)
                    }
                    WatchMode.CHR_HUNDREDTHS -> {
                        val label = "CHR 1-100"
                        val (dw, dh) = sevenSegFitSize(label, innerW, innerH)
                        val tw = measureSevenSegWidth(label, dw)
                        drawSevenSegText(label, Offset(hPad + (innerW - tw) / 2f, vPad + (innerH - dh) / 2f), dw, dh, lcdOn, lcdOff)
                    }
                }
            }
        }

        // ── Layer 3 – Lower LCD ───────────────────────────────────────────────
        Box(
            modifier = Modifier
                .absoluteOffset(x = swDp * LCD_X1, y = shDp * LLCD_Y1)
                .width(swDp * (LCD_X2 - LCD_X1))
                .height(shDp * (LLCD_Y2 - LLCD_Y1))
                .background(LcdBackground)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val panelW = size.width;  val panelH = size.height
                val vPad = panelH * 0.08f;  val hPad = panelW * 0.04f
                val innerH = panelH - vPad * 2f;  val innerW = panelW - hPad * 2f

                val displayText = when (chrono.watchMode) {
                    WatchMode.NORMAL ->
                        "%02d:%02d:%02d".format(
                            utcCal.get(Calendar.HOUR_OF_DAY),
                            utcCal.get(Calendar.MINUTE),
                            utcCal.get(Calendar.SECOND)
                        )
                    WatchMode.CHR ->
                        formatChrono(chronoElapsed, hundredths = false)
                    WatchMode.CHR_HUNDREDTHS ->
                        formatChrono(chronoElapsed, hundredths = true)
                }

                val (dw, dh) = sevenSegFitSize(displayText, innerW, innerH)
                val totalW   = measureSevenSegWidth(displayText, dw)
                drawSevenSegText(
                    displayText,
                    Offset(hPad + (innerW - totalW) / 2f, vPad + (innerH - dh) / 2f),
                    dw, dh, lcdOn, lcdOff
                )

                // Dim context label bottom-right
                val label = when (chrono.watchMode) {
                    WatchMode.NORMAL -> "UTC"
                    WatchMode.CHR    -> "CHR"
                    WatchMode.CHR_HUNDREDTHS -> "1/100"
                }
                val dimPaint = Paint().apply {
                    isAntiAlias = true
                    color = lcdOff.copy(alpha = 0.7f).toArgb()
                    typeface = android.graphics.Typeface.MONOSPACE
                    textSize  = panelH * 0.18f
                    textAlign = Paint.Align.RIGHT
                }
                drawIntoCanvas { c ->
                    c.nativeCanvas.drawText(label, panelW - hPad, panelH - vPad * 0.4f, dimPaint)
                }

                // Divider line splitting STOP|RESET (left) and START (right) halves
                val midX = panelW / 2f
                drawLine(
                    color = lcdOff,
                    start = Offset(midX, vPad * 1.5f),
                    end   = Offset(midX, panelH - vPad * 1.5f),
                    strokeWidth = 1.5f
                )
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

private fun formatChrono(elapsedMs: Long, hundredths: Boolean): String {
    val totalSec = elapsedMs / 1000
    val hh = totalSec / 3600
    val mm = (totalSec / 60) % 60
    val ss = totalSec % 60
    val cs = (elapsedMs / 10) % 100
    return if (hundredths) "%02d:%02d.%02d".format(mm, ss, cs)
    else                   "%02d:%02d:%02d".format(hh, mm, ss)
}

private fun dayAbbrev(cal: Calendar): String =
    SimpleDateFormat("EEE", Locale.US).format(cal.time).uppercase(Locale.US)

private fun monthAbbrev(cal: Calendar): String =
    SimpleDateFormat("MMM", Locale.US).format(cal.time).uppercase(Locale.US)
