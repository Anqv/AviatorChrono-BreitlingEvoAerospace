package com.aviatorchrono.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// Cycles on every upper-LCD tap: NORMAL → CHR → CHR_HUNDREDTHS → COUNTDOWN → NORMAL → …
enum class WatchMode { NORMAL, CHR, CHR_HUNDREDTHS, COUNTDOWN }

// Longest countdown the bezel can dial in: 99:59:59
private const val COUNTDOWN_MAX_MS = (99L * 3600 + 59 * 60 + 59) * 1000

enum class LockPreference { AUTO, OFF }

enum class LcdColor(val onColor: Color, val offColor: Color) {
    AMBER (Color(0xFFD4A843), Color(0xFF2A1F0A)),
    GREEN (Color(0xFF39FF14), Color(0xFF061A04)),
    RED   (Color(0xFFFF3030), Color(0xFF1A0505)),
    BLUE  (Color(0xFF30A0FF), Color(0xFF051020)),
    YELLOW(Color(0xFFFFE600), Color(0xFF1A1800));

    fun next(): LcdColor = entries[(ordinal + 1) % entries.size]
}

class ChronoState {
    var running by mutableStateOf(false)
        private set
    var elapsedMs by mutableStateOf(0L)
        private set
    private var startedAtMs: Long = 0L

    var watchMode by mutableStateOf(WatchMode.NORMAL)
        private set

    var lockPreference by mutableStateOf(LockPreference.AUTO)
        private set

    var lcdColor by mutableStateOf(LcdColor.AMBER)
        private set

    var parkedMode by mutableStateOf(false)
        private set

    // Increments each time the counter is actually reset to zero (animation trigger)
    var resetCount by mutableStateOf(0)
        private set

    // Split/lap: display frozen at lapElapsedMs while chrono keeps counting
    var lapMode by mutableStateOf(false)
        private set
    var lapElapsedMs by mutableStateOf(0L)
        private set

    // Countdown timer (COUNTDOWN mode) — dialed via the bezel, counts down to
    // 00:00:00 then continues into negative (overtime) until stopped/reset.
    var countdownSetMs by mutableStateOf(0L)     // frozen/dialed value while not running
        private set
    var countdownRunning by mutableStateOf(false)
        private set
    var countdownAlarmActive by mutableStateOf(false)
        private set
    private var countdownBaseMs: Long = 0L        // remaining ms at the moment running started
    private var countdownStartedAtMs: Long = 0L
    private var bezelFirstTurnAtMs: Long? = null

    fun currentCountdownRemainingMs(nowMs: Long): Long =
        if (countdownRunning) countdownBaseMs - (nowMs - countdownStartedAtMs) else countdownSetMs

    // Bezel rotation while in COUNTDOWN mode. Interrupts a running countdown back
    // into "dialing", dismisses the alarm, and nudges the dialed value by deltaMs.
    fun adjustCountdown(nowMs: Long, deltaMs: Long) {
        if (countdownRunning) {
            countdownSetMs = currentCountdownRemainingMs(nowMs)
            countdownRunning = false
        }
        countdownAlarmActive = false
        if (bezelFirstTurnAtMs == null) bezelFirstTurnAtMs = nowMs
        countdownSetMs = (countdownSetMs + deltaMs).coerceIn(0L, COUNTDOWN_MAX_MS)
    }

    // Lower LCD RIGHT tap in COUNTDOWN mode — commit the dialed value (minus the
    // time spent dialing it in) and start counting down.
    fun countdownStart(nowMs: Long) {
        countdownAlarmActive = false
        if (!countdownRunning) {
            val dialElapsed = bezelFirstTurnAtMs?.let { nowMs - it } ?: 0L
            countdownBaseMs = countdownSetMs - dialElapsed
            countdownRunning = true
            countdownStartedAtMs = nowMs
            bezelFirstTurnAtMs = null
        }
    }

    // Lower LCD LEFT tap in COUNTDOWN mode — pause if running, else reset to zero.
    fun countdownStopOrReset(nowMs: Long) {
        countdownAlarmActive = false
        if (countdownRunning) {
            countdownSetMs = currentCountdownRemainingMs(nowMs)
            countdownRunning = false
            bezelFirstTurnAtMs = null
        } else if (countdownSetMs != 0L) {
            countdownSetMs = 0L
            bezelFirstTurnAtMs = null
        }
    }

    fun armCountdownAlarm() {
        countdownAlarmActive = true
    }

    // Screen-awake lock: only in CHR (seconds) mode while running
    val isNavigationActive: Boolean
        get() = lockPreference == LockPreference.AUTO &&
                running &&
                watchMode == WatchMode.CHR

    fun currentElapsedMs(nowMs: Long): Long =
        if (running) elapsedMs + (nowMs - startedAtMs) else elapsedMs

    // Upper LCD tap — cycle display mode
    fun cycleMode() {
        watchMode = when (watchMode) {
            WatchMode.NORMAL          -> WatchMode.CHR
            WatchMode.CHR             -> WatchMode.CHR_HUNDREDTHS
            WatchMode.CHR_HUNDREDTHS  -> WatchMode.COUNTDOWN
            WatchMode.COUNTDOWN       -> WatchMode.NORMAL
        }
    }

    // Lower LCD LEFT tap — stop if running, reset if paused with elapsed time
    fun stopOrReset(nowMs: Long) {
        if (running) {
            elapsedMs += nowMs - startedAtMs
            running = false
            lapMode = false
        } else if (elapsedMs > 0L) {
            elapsedMs = 0L
            lapMode = false
            resetCount++
        }
    }

    // Lower LCD RIGHT tap:
    //   not running          → start
    //   running, no lap      → freeze display (lap/split)
    //   running, lap active  → unfreeze, show live time again
    fun startContinue(nowMs: Long) {
        when {
            !running  -> { startedAtMs = nowMs; running = true; lapMode = false }
            lapMode   -> { lapMode = false }
            else      -> { lapElapsedMs = currentElapsedMs(nowMs); lapMode = true }
        }
    }

    fun cycleLcdColor() {
        lcdColor = lcdColor.next()
    }

    fun toggleParkedMode() {
        parkedMode = !parkedMode
    }
}
