# Aviator Chrono — Technical Specification

**Version:** 1.0  
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
│         AviatorChronoScreen             │  Compose entry point, UI composition
├─────────────────────────────────────────┤
│              ChronoState                │  State holder — Compose mutableStateOf,
│                                         │  all business logic, enums
├─────────────────────────────────────────┤
│  AviatorDial   SevenSegmentDisplay      │  Rendering — Canvas DrawScope extensions,
│                                         │  no state, pure drawing functions
└─────────────────────────────────────────┘
```

### 3.1 Refresh loop

A `LaunchedEffect` coroutine polls `System.currentTimeMillis()` at **10 Hz** (100 ms interval). This is sufficient for:

- Smooth analog hand movement (16 ms / frame is handled by Compose recomposition; 10 Hz provides the input)
- Smooth 7-segment second and hundredths display
- Acceptable chrono sweep hand motion

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
- `AviatorChronoScreen` composable — full UI layout
- `formatChrono()` — elapsed time formatting
- `dayAbbrev()` / `monthAbbrev()` — locale-aware date strings

**Key composable structure:**

```
BoxWithConstraints (fillMaxSize)
├── Canvas — PNG background (nativeCanvas.drawBitmap, scaled to fill)
├── Box (fillMaxSize, clickable) — dial tap zone
├── Box (upper LCD position, clickable) — Canvas with 7-seg content
├── Box (lower LCD position, clickable) — Canvas with 7-seg content
└── AviatorDial — analog hands Canvas (top layer, no touch handling)
```

Touch events are consumed by the topmost matching composable. LCD panel boxes appear above the dial tap zone in the composition tree and therefore intercept taps within their bounds before the full-screen dial handler.

### 4.2 `ChronoState.kt`

**Responsibilities:** All application logic and user-facing state.

**Enums:**

| Enum | Values |
|---|---|
| `ChronoPrecision` | `SEC`, `HUNDREDTHS` |
| `LockPreference` | `AUTO`, `OFF` |
| `LcdColor` | `AMBER`, `GREEN`, `RED`, `BLUE`, `YELLOW` |

Each `LcdColor` entry carries its own `onColor` (active segment) and `offColor` (ghost/inactive segment) as Compose `Color` values — see §7 for the full colour table.

**State properties:**

| Property | Type | Default | Visibility |
|---|---|---|---|
| `running` | `Boolean` | `false` | public read, private set |
| `elapsedMs` | `Long` | `0L` | public read, private set |
| `precision` | `ChronoPrecision` | `SEC` | public read, private set |
| `lockPreference` | `LockPreference` | `AUTO` | public read, private set |
| `lcdColor` | `LcdColor` | `AMBER` | public read, private set |

**Private fields:**

| Field | Purpose |
|---|---|
| `startedAtMs` | Timestamp of the most recent start call |
| `lastStopAtMs` | Timestamp of the most recent stop; used for reset detection |

**Computed properties:**

```kotlin
isNavigationActive = lockPreference == AUTO && running && precision == SEC
isChronoActive     = running || elapsedMs > 0L
```

### 4.3 `AviatorDial.kt`

Draws only the analog hands and center hub onto a `Canvas(modifier)`. The PNG background is rendered by `MainActivity` before this composable in the layer stack, so no background drawing happens here.

**Hands drawn (bottom to top):**

| Hand | Length (% radius) | Color | Width |
|---|---|---|---|
| Chrono sweep | 60% | Orange 90% alpha | 2.5 px |
| Second | 82% | Orange | 2 px |
| Minute | 70% | Cream (silver) | 6 px |
| Hour | 48% | Cream (silver) | 9 px |
| Hub circle | r=7 px | Cream | — |
| Hub inner dot | r=2.5 px | `#1A1A1A` | — |

Also defines the shared design token `Color` constants used across files (see §7).

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
         ┌─────────────────────────────┐
         │           RESET             │
         │  running=false, elapsed=0   │◄──── double-tap within 400ms of stop
         └────────────┬────────────────┘      (only if elapsed > 0)
                      │ tap
                      ▼
         ┌─────────────────────────────┐
         │           RUNNING           │
         │  running=true               │
         │  elapsed accumulates live   │
         └────────────┬────────────────┘
                      │ tap
                      ▼
         ┌─────────────────────────────┐
         │           STOPPED           │
         │  running=false              │
         │  elapsed frozen             │
         └─────────────────────────────┘
```

**Reset detection:** `toggleStartStop(nowMs)` is called on every dial tap. When `running == false`, if `nowMs - lastStopAtMs < 400ms AND elapsedMs > 0`, the state resets instead of starting. This makes a second tap within 400 ms of stopping act as a reset.

**Elapsed time accumulation:** `elapsedMs` stores the sum of all completed intervals. The live value is `elapsedMs + (nowMs - startedAtMs)` while running. On stop, `(nowMs - startedAtMs)` is added to `elapsedMs` before clearing `running`.

---

## 6. Display Logic

### 6.1 Upper LCD panel

The upper LCD acts as a **mode indicator**.

| Condition | Label shown | Value shown |
|---|---|---|
| `!isChronoActive` | — | `MON 07 JUL` (date, mixed text + 7-seg) |
| `isChronoActive && precision == SEC` | — | `CHR` (7-seg) |
| `isChronoActive && precision == HUNDREDTHS` | — | `CHR 1-100` (7-seg) |

Date rendering: day-of-week abbreviation (`MON`) and month abbreviation (`JUL`) are drawn as native monospace text via `nativeCanvas.drawText`; the day number (`07`) is drawn as 7-segment digits. All three are vertically aligned by baseline and horizontally centred as a group.

### 6.2 Lower LCD panel

The lower LCD shows the **primary numeric readout** — digits are auto-sized to fill the panel.

| Condition | Content | Format |
|---|---|---|
| `!isChronoActive` | UTC time | `HH:MM:SS` |
| `isChronoActive && precision == SEC` | Chrono elapsed | `HH:MM:SS` |
| `isChronoActive && precision == HUNDREDTHS` | Chrono elapsed | `MM:SS.cc` |

A small context label (`UTC` or `CHR`) is drawn in dim offColor at the bottom-right corner of the panel for readability.

### 6.3 Chrono time formatting (`formatChrono`)

```
SEC mode:        HH:MM:SS   (hours : minutes : seconds)
HUNDREDTHS mode: MM:SS.cc   (minutes : seconds . centiseconds)
```

Maximum displayable time before overflow:
- SEC mode: 99:59:59 (≈100 hours)
- HUNDREDTHS mode: 99:59.99 (≈100 minutes)

### 6.4 Analog hands

Hand angles are calculated from `System.currentTimeMillis()` on every 10 Hz tick:

```
hourAngle   = hour12 × 30° + minute × 0.5°
minuteAngle = minute × 6° + second × 0.1°
secondAngle = second × 6° + millis × 0.006°
chronoAngle = (elapsedMs / 1000 mod 60) × 6°
```

---

## 7. Colour System

### 7.1 Design tokens (defined in `AviatorDial.kt`)

| Token | Hex | Used for |
|---|---|---|
| `DialBackground` | `#0A1433` | Fallback background if PNG fails to load |
| `Cream` | `#D0D8E4` | Hour and minute hands |
| `Orange` | `#FF6B1A` | Second hand, chrono sweep |
| `LcdBackground` | `#050D05` | LCD panel fill |
| `LcdBorder` | `#1E2E1E` | LCD panel border |
| `LcdLabel` | `#607060` | Reserved (currently unused in UI) |

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

| Panel | Top | Bottom | Left | Right | Height frac | Width frac |
|---|---|---|---|---|---|---|
| Upper | 0.1746 | 0.3002 | 0.2088 | 0.7928 | 0.1256 | 0.5840 |
| Lower | 0.6493 | 0.8254 | 0.2088 | 0.7928 | 0.1761 | 0.5840 |

Both panels share the same horizontal bounds (centred on the dial).

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

| Target | Gesture | Action |
|---|---|---|
| Dial (outside LCD panels) | Single tap | `chrono.toggleStartStop(nowMs)` |
| Dial (outside LCD panels) | Double-tap while stopped (< 400 ms) | Reset chrono to zero |
| Upper LCD panel | Single tap | `chrono.togglePrecision()` → SEC ↔ HUNDREDTHS |
| Lower LCD panel | Single tap | `chrono.cycleLcdColor()` → AMBER→GREEN→RED→BLUE→YELLOW→AMBER |

Touch routing: Compose processes events top-down in the composition tree. LCD `Box` composables are placed after (above) the full-screen dial tap zone, so they intercept taps first within their bounds. The dial tap zone catches everything else.

---

## 11. Screen-Awake Lock

The lock engages automatically when all three conditions hold simultaneously:

```
isNavigationActive = (lockPreference == AUTO) && running && (precision == SEC)
```

The rationale: hundredths mode implies a short-duration stopwatch context (not navigation), so the lock is released to save battery. If the user switches to `LockPreference.OFF`, the lock never engages regardless of chrono state.

`onPause()` always clears `FLAG_KEEP_SCREEN_ON` — the lock can never persist after the app is backgrounded.

---

## 12. Asset Pipeline

### 12.1 Dial background

`watch_face_clean_blue.png` (613 × 613 px, RGBA) is stored at:
- Project root — source / reference
- `app/src/main/res/drawable/` — runtime resource

At runtime it is decoded with `BitmapFactory.decodeResource` (held in `remember {}`) and drawn on the main Canvas via `nativeCanvas.drawBitmap(..., RectF(0,0,w,h), null)` to fill the screen exactly.

### 12.2 App icon

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

Icon design: navy dial, silver bezel ring, orange 12 o'clock aviation triangle, cream hour/minute hands at 10:10, orange second hand, silver center hub.

---

## 13. Future Extension Points

| Feature | Notes |
|---|---|
| Additional modes (alarm, timer) | Add a new `WatchMode` enum value; upper LCD already routes on mode |
| Wear OS Complications | Expose chrono running state as a `SHORT_TEXT` complication provider in a separate module |
| Ambient mode | Pass `secondAngleDeg = null` and `chronoAngleDeg = null` to `AviatorDial` to hide thin hands; reduce LCD to static UTC display |
| Persistent chrono | Save `elapsedMs` + `startedAtMs` + `running` to `DataStore` on stop/pause to survive process death |
| Additional LCD colours | Add entries to the `LcdColor` enum; `next()` cycles automatically |
