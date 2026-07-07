# Aviator Chrono — Technical Specification

**Version:** 1.1  
**Platform:** Wear OS 3+ (minSdk 30)  
**Package:** `com.aviatorchrono.app`  
**Repository:** https://github.com/Anqv/AviatorChronoApp

---

## 1. Overview

Aviator Chrono is a standalone Wear OS foreground application that functions as a precision aviation chronograph. It is modelled visually on the Breitling EVO Aerospace (Chronometre Aerospace) and targets pilots and navigators who need a reliable, screen-awake navigation timer on a Galaxy Watch 4 or later device.

### 1.1 Why an app, not a watch face

Wear OS's Watch Face Format (WFF) is declarative XML and cannot support:

- A stateful start / stop / reset chronograph
- `FLAG_KEEP_SCREEN_ON` (screen-awake lock)

A foreground app can do both, using the same mechanism workout and timer apps rely on. The app appears in the watch's app launcher and is marked `standalone` (no paired phone required).

---

## 2. Platform & Build Requirements

| Property | Value |
|---|---|
| `minSdk` | 30 (Wear OS 3) |
| `targetSdk` / `compileSdk` | 34 |
| `applicationId` | `com.aviatorchrono.app` |
| `versionCode` / `versionName` | 1 / "1.0" |
| Kotlin compiler extension | 1.5.14 |
| JVM target | 1.8 |
| Build tool | Gradle 9.3, JDK 17+ (Android Studio bundled JBR) |

### 2.1 Dependencies

| Library | Version | Purpose |
|---|---|---|
| `androidx.core:core-ktx` | 1.13.1 | Android KTX extensions |
| `androidx.activity:activity-compose` | 1.9.0 | `setContent {}` entry point |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.8.2 | Coroutine lifecycle scope |
| `androidx.wear.compose:compose-material` | 1.3.1 | Wear OS Compose widgets |
| `androidx.wear.compose:compose-foundation` | 1.3.1 | Wear OS layout primitives |
| `androidx.wear:wear` | 1.3.0 | Wear OS core APIs |
| `androidx.wear:wear-tooling-preview` | 1.0.0 | Emulator preview support |

---

## 3. Architecture

The app uses a minimal three-layer architecture with no ViewModel, no Room, and no dependency injection framework.

```
┌─────────────────────────────────────────┐
│              MainActivity               │  Activity — lifecycle, screen lock,
│         AviatorChronoScreen             │  Compose entry point, UI composition,
│                                         │  tap routing, sweep animation
├─────────────────────────────────────────┤
│              ChronoState                │  State holder — Compose mutableStateOf,
│                                         │  all business logic, enums
├─────────────────────────────────────────┤
│  AviatorDial   SevenSegmentDisplay      │  Rendering — Canvas DrawScope extensions,
│                                         │  no state, pure drawing functions
└─────────────────────────────────────────┘
```

### 3.1 Refresh loop

A `LaunchedEffect` coroutine polls `System.currentTimeMillis()` at **10 Hz** (100 ms interval). This drives:

- Analog hand angle calculations
- 7-segment second and hundredths display
- Chrono sweep hand position

CPU and battery impact is negligible compared to the display staying on.

### 3.2 State observation

`ChronoState` properties are `mutableStateOf` values. Compose's snapshot system automatically triggers recomposition in any composable that reads them, without explicit `StateFlow` or `LiveData` wiring.

### 3.3 Screen lock

`MainActivity` passes an `onNavigationActiveChanged: (Boolean) -> Unit` lambda to `AviatorChronoScreen`. A `LaunchedEffect(chrono.isNavigationActive)` calls this lambda whenever the navigation-active state changes. The activity then adds or clears `FLAG_KEEP_SCREEN_ON` on its window. `onPause()` always clears the flag as a safety net.

---

## 4. Source Files

### 4.1 `MainActivity.kt`

**Responsibilities:**
- Activity lifecycle (`onCreate`, `onPause`)
- `setContent {}` with `AviatorChronoScreen`
- Window flag management (`FLAG_KEEP_SCREEN_ON`)
- `AviatorChronoScreen` composable — full UI layout and tap routing
- Chrono sweep-back animation (`Animatable`, `LaunchedEffect`)
- `formatChrono()` — elapsed time formatting
- `dayAbbrev()` / `monthAbbrev()` — locale-aware date strings

**Composable layer stack (bottom to top):**

```
BoxWithConstraints (fillMaxSize)
├── Box (fillMaxSize, pointerInput) — single tap dispatcher
├── Canvas — PNG background (nativeCanvas.drawBitmap, scaled to fill)
├── Box (upper LCD position) — Canvas with 7-seg content
├── Box (lower LCD position) — Canvas with 7-seg content
└── AviatorDial — analog hands Canvas (top layer, no touch handling)
```

All touch events are handled by a single `detectTapGestures` dispatcher that routes by fractional screen coordinates (see §10).

### 4.2 `ChronoState.kt`

**Responsibilities:** All application logic and user-facing state.

**Enums:**

| Enum | Values |
|---|---|
| `WatchMode` | `NORMAL`, `CHR`, `CHR_HUNDREDTHS` |
| `LockPreference` | `AUTO`, `OFF` |
| `LcdColor` | `AMBER`, `GREEN`, `RED`, `BLUE`, `YELLOW` |

Each `LcdColor` entry carries its own `onColor` (active segment) and `offColor` (ghost/inactive segment) as Compose `Color` values — see §7 for the full colour table.

**State properties:**

| Property | Type | Default | Visibility |
|---|---|---|---|
| `running` | `Boolean` | `false` | public read, private set |
| `elapsedMs` | `Long` | `0L` | public read, private set |
| `watchMode` | `WatchMode` | `NORMAL` | public read, private set |
| `lockPreference` | `LockPreference` | `AUTO` | public read, private set |
| `lcdColor` | `LcdColor` | `AMBER` | public read, private set |
| `parkedMode` | `Boolean` | `false` | public read, private set |
| `lapMode` | `Boolean` | `false` | public read, private set |
| `lapElapsedMs` | `Long` | `0L` | public read, private set |
| `resetCount` | `Int` | `0` | public read, private set |

**Private fields:**

| Field | Purpose |
|---|---|
| `startedAtMs` | Timestamp of the most recent start call |

**Computed properties:**

```kotlin
isNavigationActive = lockPreference == AUTO && running && watchMode == CHR
currentElapsedMs(nowMs) = if (running) elapsedMs + (nowMs - startedAtMs) else elapsedMs
```

### 4.3 `AviatorDial.kt`

Draws only the analog hands and center hub onto a `Canvas(modifier)`. The PNG background is rendered by `MainActivity` before this composable in the layer stack.

**Hands drawn (bottom to top):**

| Hand | Length (% radius) | Color | Notes |
|---|---|---|---|
| Hour (sword) | 50% | Silver + lume inlay | Parked at 90° (3 o'clock) when `parkedMode` |
| Minute (sword) | 72% | Silver + lume inlay | Parked at 270° (9 o'clock) when `parkedMode` |
| Second | 84% (tip), 22% (tail) | Aviation red | Hidden (`null`) when `parkedMode` |
| Chrono sweep | 62% | Aviation red | JAS aircraft bitmap at tip; `null` in NORMAL mode |
| Hub circles | — | Silver / dark / silver | Three concentric circles, topmost layer |

Also defines the shared design token `Color` constants used across files (see §7).

**Chrono sweep bitmap transform** (`drawChronoSweep`):

The `jas_plane.png` bitmap has its nose at pixel row 0. The `android.graphics.Matrix` chain applied (operations execute in reverse declaration order on pixels):

1. `postScale(scale, scale)` — resize to target height
2. `postTranslate(-scaledW/2, 0)` — centre nose on local origin
3. `postRotate(+angleDeg)` — CW rotation aligns the tail (positive-Y axis) toward the dial centre in Android's Y-down coordinate system
4. `postTranslate(tip.x, tip.y)` — place nose at sweep-arm tip

### 4.4 `SevenSegmentDisplay.kt`

Pure `DrawScope` extension functions — no composable, no state.

**Public API:**

```kotlin
// Draw text and return total pixel width consumed
fun DrawScope.drawSevenSegText(
    text: String,
    topLeft: Offset,
    digitWidth: Float,
    digitHeight: Float,
    onColor: Color,
    offColor: Color,
    strokeWidth: Float = digitWidth * 0.12f
): Float

// Measure width without drawing
fun measureSevenSegWidth(text: String, digitWidth: Float, ...): Float

// Find the largest digit size that fits within a bounding box
fun sevenSegFitSize(
    text: String,
    availWidth: Float,
    availHeight: Float,
    widthToHeight: Float = 0.58f    // digit aspect ratio
): Pair<Float, Float>               // (digitWidth, digitHeight)
```

---

## 5. Chronograph State Machine

```
         ┌──────────────────────────────┐
         │           NORMAL             │
         │  running=false, elapsed=0    │◄──── lower-LCD left tap (reset)
         └────────────┬─────────────────┘      when stopped & elapsed > 0
                      │ lower-LCD right tap (start)
                      ▼
         ┌──────────────────────────────┐
         │           RUNNING            │
         │  running=true                │
         │  elapsed accumulates live    │
         └──────┬──────────┬────────────┘
                │ right tap │ left tap
                ▼           ▼
         ┌──────────┐  ┌────────────────┐
         │ LAP MODE │  │    STOPPED     │
         │ display  │  │ running=false  │
         │ frozen   │  │ elapsed frozen │
         └──────────┘  └────────────────┘
          right tap →
          resume live
```

**Elapsed time accumulation:** `elapsedMs` stores the sum of all completed intervals. The live value is `currentElapsedMs(nowMs) = elapsedMs + (nowMs - startedAtMs)` while running. On stop, `(nowMs - startedAtMs)` is added to `elapsedMs` before clearing `running`.

**Lap/split:** Tapping the right half of the lower LCD while running stores `currentElapsedMs(nowMs)` in `lapElapsedMs` and sets `lapMode = true`. The lower LCD then displays the frozen `lapElapsedMs`. A second right-tap clears `lapMode` and resumes the live display. Stopping the chrono also clears `lapMode`.

**Reset:** Tapping the left half of the lower LCD while stopped (and `elapsedMs > 0`) resets `elapsedMs = 0` and increments `resetCount`. `resetCount` is used as a `LaunchedEffect` key in `MainActivity` to trigger the sweep-back animation.

---

## 6. Display Logic

### 6.1 Upper LCD panel

The upper LCD acts as a **mode indicator**, cycled by tapping it.

| `WatchMode` | Content |
|---|---|
| `NORMAL` | `MON 07 JUL` — day abbrev + 7-seg day number + month abbrev |
| `CHR` | `CHR` (7-seg letters) |
| `CHR_HUNDREDTHS` | `CHR 1-100` (7-seg letters and digits) |

Date rendering: day-of-week abbreviation (`MON`) and month abbreviation (`JUL`) are drawn as native monospace text via `nativeCanvas.drawText`; the day number (`07`) is drawn as 7-segment digits. All three are vertically aligned and horizontally centred as a group.

### 6.2 Lower LCD panel

The lower LCD shows the **primary numeric readout** — digits are auto-sized to fill the panel.

| State | Content | Format |
|---|---|---|
| `NORMAL` mode | UTC time | `HH:MM:SS` |
| `CHR` mode | Chrono elapsed (or lap snapshot) | `HH:MM:SS` |
| `CHR_HUNDREDTHS` mode | Chrono elapsed (or lap snapshot) | `MM:SS.cc` |

A small dim label at the bottom-right corner shows `UTC`, `CHR`, `1/100`, or `LAP` for context.

A vertical divider line at the horizontal midpoint of the panel marks the STOP|RESET (left) / START (right) tap boundary.

### 6.3 Chrono time formatting (`formatChrono`)

```
CHR mode:           HH:MM:SS   (hours : minutes : seconds)
CHR_HUNDREDTHS mode: MM:SS.cc  (minutes : seconds . centiseconds)
```

### 6.4 Analog hands

Hand angles are calculated from `System.currentTimeMillis()` on every 10 Hz tick. When `parkedMode` is true the time hands are overridden to fixed positions and the second hand is hidden.

```
hourAngle   = hour12 × 30° + minute × 0.5°   (or 90° when parked)
minuteAngle = minute × 6° + second × 0.1°    (or 270° when parked)
secondAngle = second × 6° + millis × 0.006°  (or null when parked)
chronoAngle = (elapsedMs / 1000 mod 60) × 6° (driven by displayedChronoAngle in NORMAL/reset)
```

### 6.5 Chrono sweep animation

`displayedChronoAngle` is an `Animatable<Float>` that:

- **Snaps** to the live `chronoAngle` each tick while `chrono.running == true` in CHR/CHR_HUNDREDTHS mode
- **Animates CCW to 0** at 90°/s (`tween(ms, LinearEasing)`) when either:
  - `watchMode` switches to `NORMAL`
  - `resetCount` increments (reset while in CHR mode)

`AviatorDial` receives `displayedChronoAngle.value` (not the raw `chronoAngle`), so the visible hand follows the animation.

**Race-condition guard:** the snap-to-live-angle `LaunchedEffect` only fires when `chrono.running == true`. This prevents the effect from racing with the sweep-back animation on a reset (when `chronoAngle` drops to 0 in the same recomposition that increments `resetCount`).

---

## 7. Colour System

### 7.1 Design tokens (defined in `AviatorDial.kt`)

| Token | Hex | Used for |
|---|---|---|
| `DialBackground` | `#0A1433` | Fallback background if PNG fails to load |
| `DialOutline` | `#2A4070` | (unused after PNG background; kept for compat) |
| `Cream` | `#D0D8E4` | Hour and minute hand outer colour |
| `Orange` | `#DC0A1E` | Aviation red — both second hands and chrono sweep |
| `MinorTick` | `#4A5A70` | (unused after PNG background; kept for compat) |
| `Teal` | `#4A9B94` | (unused; kept for compat) |
| `LcdBackground` | `#050D05` | LCD panel fill |
| `LcdBorder` | `#1E2E1E` | LCD panel border |
| `LcdLabel` | `#607060` | Reserved |

### 7.2 LCD segment colours

| `LcdColor` | On (active) | Off (ghost) |
|---|---|---|
| `AMBER` | `#D4A843` | `#2A1F0A` |
| `GREEN` | `#39FF14` | `#061A04` |
| `RED` | `#FF3030` | `#1A0505` |
| `BLUE` | `#30A0FF` | `#051020` |
| `YELLOW` | `#FFE600` | `#1A1800` |

Ghost (off) segments are always drawn — they give the authentic LCD look of inactive segments being faintly visible.

---

## 8. LCD Panel Geometry

Panel positions were measured pixel-by-pixel from `watch_face_clean_blue.png` (613 × 613 px) using Pillow. Fractional coordinates are applied to the live screen size via `BoxWithConstraints`.

| Panel | Top (Y1) | Bottom (Y2) | Left (X1) | Right (X2) | Mid-X |
|---|---|---|---|---|---|
| Upper | 0.1746 | 0.3002 | 0.2088 | 0.7928 | — |
| Lower | 0.6493 | 0.8254 | 0.2088 | 0.7928 | 0.5008 |

Both panels share the same horizontal bounds. The lower LCD mid-X (`LCD_XM = 0.5008`) divides the STOP|RESET tap zone (left) from the START tap zone (right).

---

## 9. 7-Segment Renderer

### 9.1 Segment layout

```
 ─── a ───
|         |
f         b
|         |
 ─── g ───
|         |
e         c
|         |
 ─── d ───
```

Each segment is a `drawLine` call with `StrokeCap.Round`. Endpoints are inset by `pad = strokeWidth × 0.8` to avoid segment overlap at corners.

### 9.2 Bitmask table

| Char | Bitmask (abcdefg) | Active segments |
|---|---|---|
| `0` | `1111110` | a b c d e f |
| `1` | `0110000` | b c |
| `2` | `1101101` | a b d e g |
| `3` | `1111001` | a b c d g |
| `4` | `0110011` | b c f g |
| `5` | `1011011` | a c d f g |
| `6` | `1011111` | a c d e f g |
| `7` | `1110000` | a b c |
| `8` | `1111111` | a b c d e f g |
| `9` | `1111011` | a b c d f g |
| `-` | `0000001` | g |
| ` ` | `0000000` | (none) |
| `C` | `1001110` | a d e f |
| `H` | `0110111` | b c e f g |
| `R` | `0000101` | e g (lowercase-r style; avoids P confusion) |
| `h` | `0010111` | c e f g |
| `r` | `0000101` | e g |

### 9.3 Spacing constants

| Constant | Value | Purpose |
|---|---|---|
| `strokeWidth` | `digitWidth × 0.12` | Segment line thickness |
| `pad` | `strokeWidth × 0.8` | Endpoint inset to avoid corner bleed |
| `DIGIT_GAP_FRACTION` | `0.35` | Gap between digit cells as fraction of digitWidth |
| `colonWidth` | `digitWidth × 0.45` | Width allocated for `:` character |
| `dotWidth` | `digitWidth × 0.32` | Width allocated for `.` character |
| `widthToHeight` | `0.58` | Digit aspect ratio used by `sevenSegFitSize` |

### 9.4 Auto-fit algorithm (`sevenSegFitSize`)

Computes the maximum digit size that fits a given text string in a bounding box:

1. Sum `widthUnits` for the string: each digit = `1 + DIGIT_GAP_FRACTION`, each `:` = `0.45`, each `.` = `0.32`. Subtract one trailing gap.
2. `dwFromWidth = availWidth / widthUnits` → derive `dhFromWidth = dwFromWidth / widthToHeight`
3. `dhFromHeight = availHeight` → derive `dwFromHeight = dhFromHeight × widthToHeight`
4. Return whichever pair results in the smaller digit height (i.e. the binding constraint).

---

## 10. Interaction Model

All touch is handled by a single `detectTapGestures` dispatcher attached to a full-screen `Box`. Fractional coordinates determine the action.

| Zone | Bounds (fractional) | Action |
|---|---|---|
| Upper LCD | x: 20.9–79.3%, y: 17.5–30.0% | `chrono.cycleMode()` → NORMAL→CHR→CHR_HUNDREDTHS→NORMAL |
| Lower LCD left | x: 20.9–50.1%, y: 64.9–82.5% | `chrono.stopOrReset(nowMs)` |
| Lower LCD right | x: 50.1–79.3%, y: 64.9–82.5% | `chrono.startContinue(nowMs)` |
| Centre hub | x: 42.5–57.5%, y: 42.5–57.5% | `chrono.toggleParkedMode()` |
| Dial (everything else) | — | `chrono.cycleLcdColor()` |

---

## 11. Screen-Awake Lock

The lock engages automatically when all three conditions hold simultaneously:

```kotlin
isNavigationActive = (lockPreference == AUTO) && running && (watchMode == CHR)
```

The rationale: hundredths mode implies a short-duration stopwatch context (not navigation), so the lock is released to save battery. In NORMAL mode the chrono is not running, so the lock is never engaged there either.

`onPause()` always clears `FLAG_KEEP_SCREEN_ON` — the lock can never persist after the app is backgrounded.

---

## 12. Asset Pipeline

### 12.1 Dial background

`watch_face_clean_blue.png` (613 × 613 px, RGBA) is stored at:
- Project root — source / reference
- `app/src/main/res/drawable/` — runtime resource

At runtime it is decoded with `BitmapFactory.decodeResource` (held in `remember {}`) and drawn on the main Canvas via `nativeCanvas.drawBitmap(..., RectF(0,0,w,h), null)` to fill the screen exactly.

### 12.2 JAS aircraft bitmap

`jas_plane.png` is a transparency-keyed PNG derived from `JAS.bmp`:
- Dark pixels → aviation red (RGBA `#DC0A1E`, full opacity)
- Light pixels → fully transparent

The bitmap has the aircraft nose at pixel row 0. It is drawn at the tip of the chrono sweep hand via a `Matrix` that scales, centres the nose at the origin, rotates CW by the sweep angle, and translates to the tip position on screen.

### 12.3 App icon

| Resource | Format | Purpose |
|---|---|---|
| `mipmap-mdpi/ic_launcher.png` | 48 × 48 px PNG | Legacy launcher |
| `mipmap-hdpi/ic_launcher.png` | 72 × 72 px PNG | Legacy launcher |
| `mipmap-xhdpi/ic_launcher.png` | 96 × 96 px PNG | Legacy launcher |
| `mipmap-xxhdpi/ic_launcher.png` | 144 × 144 px PNG | Legacy launcher |
| `mipmap-xxxhdpi/ic_launcher.png` | 192 × 192 px PNG | Legacy launcher |
| `mipmap-anydpi-v26/ic_launcher.xml` | Adaptive icon | API 26+ launcher |
| `drawable/ic_launcher_foreground.xml` | Vector 108 dp | Adaptive foreground |
| `drawable/ic_launcher_background.xml` | Shape (solid) | Adaptive background (`#0A1433`) |

---

## 13. Future Extension Points

| Feature | Notes |
|---|---|
| Additional modes (alarm, timer) | Add a new `WatchMode` enum value; `cycleMode()` and upper LCD already route on mode |
| Wear OS Complications | Expose chrono running state as a `SHORT_TEXT` complication provider in a separate module |
| Ambient mode | Pass `secondAngleDeg = null` and `chronoAngleDeg = null` to `AviatorDial`; reduce LCD to static UTC display |
| Persistent chrono | Save `elapsedMs` + `startedAtMs` + `running` to `DataStore` on stop/pause to survive process death |
| Additional LCD colours | Add entries to the `LcdColor` enum; `next()` cycles automatically |
