# Aviator Chrono — User Guide

A pilot's guide to using the watch. For build/install instructions see the [README](../README.md); for engineering details see [TECHNICAL_SPEC.md](TECHNICAL_SPEC.md).

---

## 1. The dial at a glance

```
              ┌─────────────────────┐
              │   UPPER LCD PANEL   │   ← date, or mode label (CHR / CD)
              └─────────────────────┘
                     analog dial
                  (hour / minute / second
                    / chrono sweep hands)
              ┌─────────────────────┐
              │   LOWER LCD PANEL   │   ← time, chrono, or countdown readout
              │  STOP·RESET │ START │   ← left half | right half
              └─────────────────────┘
```

- **Upper LCD** — tap it to cycle through the four modes.
- **Lower LCD** — the main numeric readout. Its left half and right half are separate tap zones (a thin vertical line marks the boundary).
- **Centre hub** — where the hands pivot; also a tap zone.
- **Everything else on the dial** — tapping anywhere else on the watch face (outside the two LCDs and the hub) changes the LCD colour.

---

## 2. The four modes

Tap the **upper LCD** to cycle: `NORMAL → CHR → CHR 1/100 → CD → NORMAL`.

| Upper LCD | Lower LCD shows | What it's for |
|---|---|---|
| `MON 07 JUL` (date) | Current UTC time, `HH:MM:SS` | Everyday timekeeping |
| `CHR` | Chronograph elapsed time, `HH:MM:SS` | Stopwatch, second precision |
| `CHR 1-100` | Chronograph elapsed time, `MM:SS.cc` | Stopwatch, hundredths precision |
| `CD` | Countdown timer | Timing an approach, hold, or any fixed interval |

---

## 3. Using the chronograph (`CHR` / `CHR 1/100`)

1. Switch to `CHR` or `CHR 1/100` by tapping the upper LCD.
2. **Tap the right half** of the lower LCD to start timing.
3. **Tap the left half** to stop. The elapsed time stays on screen.
4. **Tap the left half again** to reset to `00:00:00`.

**Taking a split (lap) time**, while the chronograph is running:

1. **Tap the right half** — the lower LCD freezes on the current time (label switches to `LAP`), but the chronograph keeps running underneath.
2. **Tap the right half again** — the display jumps back to the live, running time.

The analog chrono-sweep hand (with the small red aircraft at its tip) always mirrors the true running time, even while a lap is frozen on the LCD. When you stop and reset, it sweeps back to 12 o'clock.

**Screen stays on:** while the chronograph is running in `CHR` mode (seconds precision), the screen is held awake automatically so you don't lose the readout mid-approach. This does not apply in `CHR 1/100` mode, and it always releases the instant you background the app.

---

## 4. Using the countdown timer (`CD` mode)

This is for timing down to a specific moment — an approach fix, a hold, a departure slot — with an alarm so you don't have to keep watching it.

### Setting the time

1. Switch to `CD` mode (tap the upper LCD until it shows `CD`).
2. **Turn the bezel** (or rotating crown) to dial in a duration, shown on the lower LCD as `HH:MM:SS`:
   - **Clockwise** increases the time, **counter-clockwise** decreases it.
   - Turning faster changes it faster — a quick spin moves minutes, a slow nudge fine-tunes seconds.
   - The lower LCD's dim corner label reads `SET` while you're dialing.

### Starting it

3. **Tap the right half** of the lower LCD. The countdown begins — label switches to `CD`.

> **Why the number might jump slightly when you start:** the watch remembers the moment you first touched the bezel. Whatever time you spent dialing in the number gets subtracted automatically, so the countdown starts already "caught up" rather than adding your fumbling-with-the-bezel time on top of the interval you actually wanted.

### While it's running

- **Single beep** at 10 seconds remaining.
- **Double beep** at 5 seconds remaining.
- At **00:00:00**, a looping alarm sounds continuously until you dismiss it — tap either half of the lower LCD, or just start turning the bezel again.
- If you don't dismiss it, the display keeps counting **upward past zero** as overtime, shown as `-:MM:SS` (the hour digits are blanked out and replaced with the `-` sign, so the figures stay the same size — nothing jumps around on you).

### Changing your mind mid-countdown

You can retime it at any point, running or not:

- **Turn the bezel** — this immediately pauses the running countdown (freezing it at its current value) and lets you dial a new number from there, exactly like setting it fresh.
- **Tap the right half** to commit and restart — again, minus however long you spent adjusting it.
- **Tap the left half** to pause without changing the time; tap left half again (while paused, at a non-zero value) to reset to `00:00:00`.

---

## 5. Other controls

| Action | Effect |
|---|---|
| **Tap the centre hub** | Parks the hour hand at 3 o'clock and the minute hand at 9 o'clock, and hides the second hand — a quick way to get the hands out of the way of the LCDs or your view of the dial. Tap again to unpark. |
| **Tap anywhere else on the dial** | Cycles the LCD colour: Amber → Green → Red → Blue → Yellow. Purely cosmetic — pick whatever's most legible in your lighting. |

---

## 6. Tips & troubleshooting

- **Bezel/crown does nothing:** it only responds while you're in `CD` mode — turning it in `NORMAL`/`CHR`/`CHR 1/100` mode has no effect by design. If you're testing in the Wear OS emulator rather than a real watch, note that the on-screen crown graphic on the watch face image does **not** send rotary input — use the emulator's **⋯ (Extended controls) → Rotary input** panel instead.
- **Alarm won't stop:** tap either half of the lower LCD, or just start turning the bezel — both silence it immediately.
- **Screen turns off mid-approach:** the screen-awake lock only engages in `CHR` (not `CHR 1/100` or `CD`) — see §3. If you need the countdown timer to keep the screen on too, that's currently not automatic; keep the watch active or tap the screen periodically.
- **Lost your countdown value:** switching away from `CD` mode and back does not clear it — your dialed/running value is preserved. Only an explicit reset (left tap while paused) clears it to zero.
