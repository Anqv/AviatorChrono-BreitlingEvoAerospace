package com.aviatorchrono.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// Cycles on every upper-LCD tap: NORMAL → CHR → CHR_HUNDREDTHS → NORMAL → …
enum class WatchMode { NORMAL, CHR, CHR_HUNDREDTHS }

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
            WatchMode.CHR_HUNDREDTHS  -> WatchMode.NORMAL
        }
    }

    // Lower LCD LEFT tap — stop if running, reset if paused with elapsed time
    fun stopOrReset(nowMs: Long) {
        if (running) {
            elapsedMs += nowMs - startedAtMs
            running = false
        } else if (elapsedMs > 0L) {
            elapsedMs = 0L
        }
    }

    // Lower LCD RIGHT tap — start or continue
    fun startContinue(nowMs: Long) {
        if (!running) {
            startedAtMs = nowMs
            running = true
        }
    }

    fun cycleLcdColor() {
        lcdColor = lcdColor.next()
    }

    fun toggleParkedMode() {
        parkedMode = !parkedMode
    }
}
