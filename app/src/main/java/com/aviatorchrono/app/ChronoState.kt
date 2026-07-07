package com.aviatorchrono.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class ChronoPrecision { SEC, HUNDREDTHS }

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

    var precision by mutableStateOf(ChronoPrecision.SEC)
        private set

    var lockPreference by mutableStateOf(LockPreference.AUTO)
        private set

    var lcdColor by mutableStateOf(LcdColor.AMBER)
        private set

    var parkedMode by mutableStateOf(false)
        private set

    val isNavigationActive: Boolean
        get() = lockPreference == LockPreference.AUTO &&
                running &&
                precision == ChronoPrecision.SEC

    val isChronoActive: Boolean
        get() = running || elapsedMs > 0L

    fun currentElapsedMs(nowMs: Long): Long =
        if (running) elapsedMs + (nowMs - startedAtMs) else elapsedMs

    // 3-state cycle on every tap:  running → stopped → zero → running → …
    fun toggleStartStop(nowMs: Long) {
        when {
            running      -> { elapsedMs += nowMs - startedAtMs; running = false }
            elapsedMs > 0 -> { elapsedMs = 0L }
            else          -> { startedAtMs = nowMs; running = true }
        }
    }

    fun togglePrecision() {
        precision = if (precision == ChronoPrecision.SEC) ChronoPrecision.HUNDREDTHS else ChronoPrecision.SEC
    }

    fun toggleLockPreference() {
        lockPreference = if (lockPreference == LockPreference.AUTO) LockPreference.OFF else LockPreference.AUTO
    }

    fun cycleLcdColor() {
        lcdColor = lcdColor.next()
    }

    fun toggleParkedMode() {
        parkedMode = !parkedMode
    }
}
